package com.nibm.worklink;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FreelancerDashboardActivity extends AppCompatActivity
        implements ApplicationAdapter.OnApplicationActionListener {

    private View layoutJobsTab;
    private View layoutApplicationsTab;
    private View layoutProfileTab;
    private BottomNavigationView bottomNav;

    // Jobs Feed Views
    private RecyclerView recyclerJobs;
    private JobAdapter jobAdapter;
    private ChipGroup chipGroupCategories;

    // Applications Views
    private RecyclerView recyclerApplications;
    private ApplicationAdapter appAdapter;
    private TextView tvEmptyApplications;

    // Profile Views
    private View profileEmptyState;
    private View profileViewState;
    private View profileEditState;

    private TextView tvProfileName, tvProfileTitle, tvProfileRate, tvProfileBio, tvProfileSkills;
    private EditText etProfileName, etProfileTitle, etProfileRate, etProfileBio, etProfileSkills;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUid;
    private final List<String> availableCategories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_freelancer_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
        currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        // Bind Tabs Layouts
        layoutJobsTab = findViewById(R.id.layout_jobs_tab);
        layoutApplicationsTab = findViewById(R.id.layout_applications_tab);
        layoutProfileTab = findViewById(R.id.layout_profile_tab);

        // Bind Bottom Navigation
        bottomNav = findViewById(R.id.freelancer_bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_job_feed);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_job_feed) {
                switchTab(layoutJobsTab);
                loadJobs("All");
                return true;
            } else if (id == R.id.nav_my_applications) {
                switchTab(layoutApplicationsTab);
                loadApplications();
                return true;
            } else if (id == R.id.nav_profile) {
                switchTab(layoutProfileTab);
                loadProfileTab();
                return true;
            }
            return false;
        });

        // Initialize Job Feed Tab
        chipGroupCategories = findViewById(R.id.chip_group_categories);
        recyclerJobs = findViewById(R.id.recycler_jobs);
        recyclerJobs.setLayoutManager(new LinearLayoutManager(this));
        jobAdapter = new JobAdapter(new ArrayList<>());
        recyclerJobs.setAdapter(jobAdapter);

        setupCategoryChips();

        // Initialize Applications Tab
        recyclerApplications = findViewById(R.id.recycler_applications);
        recyclerApplications.setLayoutManager(new LinearLayoutManager(this));
        tvEmptyApplications = findViewById(R.id.tv_empty_applications);

        // Initialize Profile Tab Views
        profileEmptyState = findViewById(R.id.profile_empty_state);
        profileViewState = findViewById(R.id.profile_view_state);
        profileEditState = findViewById(R.id.profile_edit_state);

        tvProfileName = findViewById(R.id.tv_profile_name);
        tvProfileTitle = findViewById(R.id.tv_profile_title);
        tvProfileRate = findViewById(R.id.tv_profile_rate);
        tvProfileBio = findViewById(R.id.tv_profile_bio);
        tvProfileSkills = findViewById(R.id.tv_profile_skills);

        etProfileName = findViewById(R.id.et_profile_name);
        etProfileTitle = findViewById(R.id.et_profile_title);
        etProfileRate = findViewById(R.id.et_profile_rate);
        etProfileBio = findViewById(R.id.et_profile_bio);
        etProfileSkills = findViewById(R.id.et_profile_skills);

        // Pre-fetch categories from Firestore backend
        fetchCategoriesFromBackend();
        setupSkillsFieldForFreelancer();

        Button btnCreateProfileEmpty = findViewById(R.id.btn_create_profile_empty);
        Button btnEditProfile = findViewById(R.id.btn_edit_profile);
        Button btnDeleteProfile = findViewById(R.id.btn_delete_profile);
        Button btnSaveProfile = findViewById(R.id.btn_save_profile);
        Button btnCancelProfileEdit = findViewById(R.id.btn_cancel_profile_edit);
        Button btnLogout = findViewById(R.id.btn_freelancer_logout);

        btnCreateProfileEmpty.setOnClickListener(v -> enterProfileEditMode(true));
        btnEditProfile.setOnClickListener(v -> enterProfileEditMode(false));
        btnDeleteProfile.setOnClickListener(v -> showDeleteProfileConfirmation());
        btnSaveProfile.setOnClickListener(v -> saveProfileData());
        btnCancelProfileEdit.setOnClickListener(v -> loadProfileTab());
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(FreelancerDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Initialize feed
        loadJobs("All");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh tabs in case they were returned to after applying to a job
        loadJobs("All");
        loadApplications();
        loadProfileTab();
    }

    private void switchTab(View activeTab) {
        layoutJobsTab.setVisibility(View.GONE);
        layoutApplicationsTab.setVisibility(View.GONE);
        layoutProfileTab.setVisibility(View.GONE);
        activeTab.setVisibility(View.VISIBLE);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
        }
    }

    // Job Feed Functions
    private void loadJobs(String category) {
        db.collection("Jobs").get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Job> fetchedJobs = new ArrayList<>();
            if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    Job job = doc.toObject(Job.class);
                    if (job != null) {
                        String id = doc.getId();
                        Job fullJob = new Job(
                                id,
                                job.getTitle(),
                                job.getCompany(),
                                job.getDescription(),
                                job.getSalary(),
                                job.getCategory(),
                                job.getEmployerDescription(),
                                job.getEmployerRating(),
                                job.getEmployerContact(),
                                job.getDeadline(),
                                job.getStatus(),
                                job.isVerified()
                        );
                        fetchedJobs.add(fullJob);
                    }
                }
            }

            if (!fetchedJobs.isEmpty()) {
                DataManager.setJobs(fetchedJobs);
            } else {
                fetchedJobs = DataManager.getJobs();
            }

            List<Job> filteredJobs = new ArrayList<>();
            for (Job job : fetchedJobs) {
                if (category == null || category.equalsIgnoreCase("All")) {
                    filteredJobs.add(job);
                } else if (job.getCategory() != null && job.getCategory().equalsIgnoreCase(category.trim())) {
                    filteredJobs.add(job);
                }
            }

            if (jobAdapter == null) {
                jobAdapter = new JobAdapter(filteredJobs);
                recyclerJobs.setAdapter(jobAdapter);
            } else {
                jobAdapter.updateJobs(filteredJobs);
            }
        }).addOnFailureListener(e -> {
            List<Job> jobsList = DataManager.getJobsByCategory(category);
            if (jobAdapter == null) {
                jobAdapter = new JobAdapter(jobsList);
                recyclerJobs.setAdapter(jobAdapter);
            } else {
                jobAdapter.updateJobs(jobsList);
            }
        });
    }

    // Applications Functions
    private void loadApplications() {
        List<Application> appsList = DataManager.getApplications();
        if (appsList.isEmpty()) {
            tvEmptyApplications.setVisibility(View.VISIBLE);
            recyclerApplications.setVisibility(View.GONE);
        } else {
            tvEmptyApplications.setVisibility(View.GONE);
            recyclerApplications.setVisibility(View.VISIBLE);
            if (appAdapter == null) {
                appAdapter = new ApplicationAdapter(appsList, this);
                recyclerApplications.setAdapter(appAdapter);
            } else {
                appAdapter.updateApplications(appsList);
            }
        }
    }

    // Profile Functions
    private void loadProfileTab() {
        profileEditState.setVisibility(View.GONE);
        if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);

        if (currentUid == null) {
            profileEmptyState.setVisibility(View.VISIBLE);
            profileViewState.setVisibility(View.GONE);
            return;
        }

        db.collection("Users").document(currentUid).get()
            .addOnSuccessListener(doc -> {
                String name   = doc.getString("name");
                String skills = doc.getString("skills");
                String title  = doc.getString("title");
                String rate   = doc.getString("hourlyRate");
                String bio    = doc.getString("bio");

                boolean hasProfile = name != null && !name.isEmpty();
                profileEmptyState.setVisibility(hasProfile ? View.GONE  : View.VISIBLE);
                profileViewState.setVisibility(hasProfile  ? View.VISIBLE: View.GONE);

                if (hasProfile) {
                    tvProfileName.setText(name);
                    tvProfileTitle.setText(title  != null ? title  : "");
                    tvProfileRate.setText(rate    != null ? rate   : "");
                    tvProfileBio.setText(bio      != null ? bio    : "");
                    tvProfileSkills.setText(skills != null ? skills : "");
                }
            })
            .addOnFailureListener(e -> {
                profileEmptyState.setVisibility(View.VISIBLE);
                profileViewState.setVisibility(View.GONE);
            });
    }

    private void setupSkillsFieldForFreelancer() {
        if (etProfileSkills != null) {
            etProfileSkills.setFocusable(false);
            etProfileSkills.setClickable(true);
            etProfileSkills.setOnClickListener(v -> showCategorySelectionDialog());
        }
    }

    private void fetchCategoriesFromBackend() {
        db.collection("Categories").get().addOnSuccessListener(snapshots -> {
            availableCategories.clear();
            if (snapshots != null) {
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    String name = doc.getString("name");
                    if (name != null && !name.trim().isEmpty()) {
                        availableCategories.add(name);
                    }
                }
            }
            if (availableCategories.isEmpty()) {
                availableCategories.add("Software Development");
                availableCategories.add("UI/UX Design");
                availableCategories.add("Content Writing");
                availableCategories.add("Digital Marketing");
            }
            setupCategoryChips();
        });
    }

    private void setupCategoryChips() {
        if (chipGroupCategories == null) return;
        chipGroupCategories.removeAllViews();

        Chip chipAll = new Chip(this);
        chipAll.setId(View.generateViewId());
        chipAll.setText("All");
        chipAll.setCheckable(true);
        chipAll.setClickable(true);
        chipAll.setFocusable(true);
        chipAll.setChecked(true);
        chipGroupCategories.addView(chipAll);

        for (String cat : availableCategories) {
            if ("All".equalsIgnoreCase(cat)) continue;
            Chip chip = new Chip(this);
            chip.setId(View.generateViewId());
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setFocusable(true);
            chipGroupCategories.addView(chip);
        }

        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                loadJobs("All");
                return;
            }
            int checkedId = checkedIds.get(0);
            Chip selectedChip = group.findViewById(checkedId);
            if (selectedChip != null) {
                loadJobs(selectedChip.getText().toString());
            } else {
                loadJobs("All");
            }
        });
    }

    private void showCategorySelectionDialog() {
        if (availableCategories.isEmpty()) {
            fetchCategoriesFromBackend();
        }
        String[] items = availableCategories.toArray(new String[0]);
        boolean[] checkedItems = new boolean[items.length];

        String currentText = etProfileSkills.getText().toString();
        if (!currentText.isEmpty()) {
            String[] selected = currentText.split(",\\s*");
            for (int i = 0; i < items.length; i++) {
                for (String s : selected) {
                    if (items[i].equalsIgnoreCase(s.trim())) {
                        checkedItems[i] = true;
                        break;
                    }
                }
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Categories / Area of Expertise")
                .setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton("Select", (dialog, which) -> {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < items.length; i++) {
                        if (checkedItems[i]) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(items[i]);
                        }
                    }
                    etProfileSkills.setText(sb.toString());
                    etProfileSkills.setError(null);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void enterProfileEditMode(boolean isNew) {
        profileEmptyState.setVisibility(View.GONE);
        profileViewState.setVisibility(View.GONE);
        profileEditState.setVisibility(View.VISIBLE);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);

        TextView formTitle = findViewById(R.id.tv_form_title);
        formTitle.setText(isNew ? "Create Freelancer Profile" : "Update Freelancer Profile");

        if (!isNew && currentUid != null) {
            db.collection("Users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    etProfileName.setText(doc.getString("name")   != null ? doc.getString("name")      : "");
                    etProfileTitle.setText(doc.getString("title")  != null ? doc.getString("title")     : "");
                    etProfileRate.setText(doc.getString("hourlyRate") != null ? doc.getString("hourlyRate") : "$");
                    etProfileBio.setText(doc.getString("bio")     != null ? doc.getString("bio")      : "");
                    etProfileSkills.setText(doc.getString("skills")!= null ? doc.getString("skills")   : "");
                });
        } else {
            etProfileName.setText("");
            etProfileTitle.setText("");
            etProfileRate.setText("$");
            etProfileBio.setText("");
            etProfileSkills.setText("");
        }
    }

    private void saveProfileData() {
        String name   = etProfileName.getText().toString().trim();
        String title  = etProfileTitle.getText().toString().trim();
        String rate   = etProfileRate.getText().toString().trim();
        String bio    = etProfileBio.getText().toString().trim();
        String skills = etProfileSkills.getText().toString().trim();

        if (name.isEmpty() || title.isEmpty() || rate.isEmpty()) {
            Toast.makeText(this, "Name, Title, and Hourly Rate are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUid == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("title", title);
        updates.put("hourlyRate", rate);
        updates.put("bio", bio);
        updates.put("skills", skills);

        db.collection("Users").document(currentUid).update(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Profile Saved Successfully", Toast.LENGTH_SHORT).show();
                loadProfileTab();
            })
            .addOnFailureListener(e ->
                Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }

    private void showDeleteProfileConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your freelancer profile details?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (currentUid == null) return;
                    Map<String, Object> cleared = new HashMap<>();
                    cleared.put("title", "");
                    cleared.put("hourlyRate", "");
                    cleared.put("bio", "");
                    cleared.put("skills", "");
                    db.collection("Users").document(currentUid).update(cleared)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Profile Cleared", Toast.LENGTH_SHORT).show();
                            loadProfileTab();
                        });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Custom review dialog
    private void showReviewDialog(String jobId, String jobTitle) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_review, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        RatingBar ratingBar = dialogView.findViewById(R.id.rating_bar);
        EditText etReviewText = dialogView.findViewById(R.id.et_review_text);
        Button btnSubmit = dialogView.findViewById(R.id.btn_submit_review);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_review);

        tvTitle.setText("Review for " + jobTitle);
        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String review = etReviewText.getText().toString().trim();
            if (review.isEmpty()) {
                etReviewText.setError("Required");
                return;
            }

            Review newReview = new Review(
                    String.valueOf(DataManager.getReviews().size() + 1),
                    jobId,
                    rating,
                    review
            );
            DataManager.addReview(newReview);
            Toast.makeText(this, "Review Submitted. Thank you!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    // --- ApplicationAdapter.OnApplicationActionListener callbacks ---

    @Override
    public void onDeleteApplication(String appId) {
        DataManager.deleteApplication(appId);
        loadApplications();
    }

    @Override
    public void onReviewApplication(String jobId, String jobTitle) {
        showReviewDialog(jobId, jobTitle);
    }
}

