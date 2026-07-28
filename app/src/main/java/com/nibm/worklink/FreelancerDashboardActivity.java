package com.nibm.worklink;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
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
    private View layoutNotificationsTab;
    private View layoutProfileTab;
    private BottomNavigationView bottomNav;

    // Navigation History Stack
    private final java.util.Stack<Integer> tabHistory = new java.util.Stack<>();
    private boolean isNavigatingBack = false;

    // Jobs Feed Views
    private RecyclerView recyclerJobs;
    private JobAdapter jobAdapter;
    private ChipGroup chipGroupCategories;

    // Applications Views
    private RecyclerView recyclerApplications;
    private ApplicationAdapter appAdapter;
    private TextView tvEmptyApplications;

    // Notifications Views
    private RecyclerView recyclerNotifications;
    private TextView tvEmptyNotifications;
    private EmployerNotificationAdapter notificationAdapter;

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
    private final androidx.activity.result.ActivityResultLauncher<Intent> notificationsLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    int targetNavId = result.getData().getIntExtra("selected_nav_id", -1);
                    if (targetNavId != -1) {
                        navigateToTab(targetNavId);
                    }
                }
            });

    private void navigateToTab(int navId) {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(navId);
        }
        if (navId == R.id.nav_job_feed) {
            switchTab(layoutJobsTab);
            loadJobs("All");
        } else if (navId == R.id.nav_my_applications) {
            switchTab(layoutApplicationsTab);
            loadApplications();
        } else if (navId == R.id.nav_profile) {
            switchTab(layoutProfileTab);
            loadProfileTab();
        }
    }

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
        layoutNotificationsTab = findViewById(R.id.layout_notifications_tab);
        layoutProfileTab = findViewById(R.id.layout_profile_tab);

        // Bind Toolbar Notifications Bell Icon
        ImageView ivNotifications = findViewById(R.id.iv_freelancer_notifications);
        if (ivNotifications != null) {
            ivNotifications.setOnClickListener(v -> {
                if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_freelancer_notifications);
            });
        }

        // Bind Bottom Navigation
        bottomNav = findViewById(R.id.freelancer_bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_job_feed);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            int currentSelectedId = bottomNav.getSelectedItemId();
            if (!isNavigatingBack && currentSelectedId != id) {
                tabHistory.push(currentSelectedId);
            }
            isNavigatingBack = false;

            if (id == R.id.nav_job_feed) {
                switchTab(layoutJobsTab);
                loadJobs("All");
                return true;
            } else if (id == R.id.nav_my_applications) {
                switchTab(layoutApplicationsTab);
                loadApplications();
                return true;
            } else if (id == R.id.nav_freelancer_notifications) {
                Intent intent = new Intent(FreelancerDashboardActivity.this, NotificationsActivity.class);
                intent.putExtra("userRole", "Freelancer");
                notificationsLauncher.launch(intent);
                return false;
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

        // Initialize Notifications Tab
        recyclerNotifications = findViewById(R.id.recycler_freelancer_notifications);
        if (recyclerNotifications != null) {
            recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        }
        tvEmptyNotifications = findViewById(R.id.tv_empty_freelancer_notifications);

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

        // Handle system back button tab navigation
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (profileEditState != null && profileEditState.getVisibility() == View.VISIBLE) {
                    loadProfileTab();
                    return;
                }
                if (!tabHistory.isEmpty()) {
                    int previousTabId = tabHistory.pop();
                    isNavigatingBack = true;
                    if (bottomNav != null) bottomNav.setSelectedItemId(previousTabId);
                } else {
                    finish();
                }
            }
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
        loadNotifications();
        loadProfileTab();
    }

    private void switchTab(View activeTab) {
        layoutJobsTab.setVisibility(View.GONE);
        layoutApplicationsTab.setVisibility(View.GONE);
        layoutNotificationsTab.setVisibility(View.GONE);
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
                boolean isApproved = job.isVerified() || "Verified".equalsIgnoreCase(job.getStatus()) || "Approved".equalsIgnoreCase(job.getStatus());
                if (!isApproved) {
                    continue;
                }
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

            // Fetch real ratings from Reviews collection and push to adapter
            RatingRepository.fetchJobRatings(ratingsMap -> {
                if (jobAdapter != null) {
                    jobAdapter.updateRatings(ratingsMap);
                }
            });
        }).addOnFailureListener(e -> {
            List<Job> jobsList = DataManager.getJobsByCategory(category);
            List<Job> approvedJobsList = new ArrayList<>();
            for (Job job : jobsList) {
                boolean isApproved = job.isVerified() || "Verified".equalsIgnoreCase(job.getStatus()) || "Approved".equalsIgnoreCase(job.getStatus());
                if (isApproved) {
                    approvedJobsList.add(job);
                }
            }
            if (jobAdapter == null) {
                jobAdapter = new JobAdapter(approvedJobsList);
                recyclerJobs.setAdapter(jobAdapter);
            } else {
                jobAdapter.updateJobs(approvedJobsList);
            }
            RatingRepository.fetchJobRatings(ratingsMap -> {
                if (jobAdapter != null) jobAdapter.updateRatings(ratingsMap);
            });
        });
    }

    // Applications Functions
    private void loadApplications() {
        if (currentUid == null) {
            tvEmptyApplications.setVisibility(View.VISIBLE);
            recyclerApplications.setVisibility(View.GONE);
            return;
        }

        db.collection("Applications")
            .whereEqualTo("freelancerUid", currentUid)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Application> appsList = new ArrayList<>();

                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String id            = doc.getString("id");
                        String jobId         = doc.getString("jobId");
                        String jobTitle      = doc.getString("jobTitle");
                        String company       = doc.getString("company");
                        String empContact    = doc.getString("employerContact");
                        String coverLetter   = doc.getString("coverLetter");
                        String resumeUrl     = doc.getString("resumeUrl");
                        if (resumeUrl == null) resumeUrl = doc.getString("resumeFileName");
                        String status        = doc.getString("status");
                        if (status == null) status = "Pending";

                        // Build a minimal Job for display
                        Job job = DataManager.getJobById(jobId);
                        if (job == null) {
                            // Create a lightweight placeholder if job is not in cache
                            job = new Job(
                                jobId != null ? jobId : "",
                                jobTitle != null ? jobTitle : "Job",
                                company != null ? company : "",
                                "", "", "",
                                "", 0f,
                                empContact != null ? empContact : "",
                                "", status, false
                            );
                        }

                        String applicantName = doc.getString("applicantName");
                        String applicantEmail = doc.getString("applicantEmail");
                        
                        Application app = new Application(
                            id != null ? id : doc.getId(),
                            job, coverLetter != null ? coverLetter : "",
                            resumeUrl != null ? resumeUrl : "",
                            status,
                            applicantName,
                            applicantEmail
                        );
                        appsList.add(app);
                    }
                }

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

                    // Fetch user's existing reviews to disable review button for already reviewed jobs
                    db.collection("Reviews").whereEqualTo("reviewerUid", currentUid).get()
                        .addOnSuccessListener(reviewSnaps -> {
                            java.util.Set<String> reviewedJobIds = new java.util.HashSet<>();
                            if (reviewSnaps != null) {
                                for (DocumentSnapshot rDoc : reviewSnaps.getDocuments()) {
                                    String rJobId = rDoc.getString("jobId");
                                    if (rJobId != null) reviewedJobIds.add(rJobId);
                                }
                            }
                            if (appAdapter != null) {
                                appAdapter.updateReviewedJobIds(reviewedJobIds);
                            }
                        });
                }
            })
            .addOnFailureListener(e -> {
                tvEmptyApplications.setVisibility(View.VISIBLE);
                recyclerApplications.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to load applications", Toast.LENGTH_SHORT).show();
            });
    }

    // Notifications & Announcements Functions
    private void loadNotifications() {
        if (currentUid == null || recyclerNotifications == null) return;

        List<EmployerNotificationAdapter.NotificationItem> items = new ArrayList<>();

        // 1. Fetch personal notifications where recipientId == currentUid
        db.collection("Notifications")
            .whereEqualTo("recipientId", currentUid)
            .get()
            .addOnSuccessListener(notifSnap -> {
                if (notifSnap != null) {
                    for (DocumentSnapshot doc : notifSnap.getDocuments()) {
                        Notification notif = doc.toObject(Notification.class);
                        if (notif != null) {
                            notif.setId(doc.getId());
                            items.add(new EmployerNotificationAdapter.NotificationItem(notif));
                            db.collection("Notifications").document(doc.getId()).update("read", true);
                        }
                    }
                }

                // 2. Fetch announcements relevant to Freelancer role
                db.collection("Announcements").get()
                    .addOnSuccessListener(annSnap -> {
                        if (annSnap != null) {
                            for (DocumentSnapshot doc : annSnap.getDocuments()) {
                                String audience = doc.getString("targetAudience");
                                if (isAudienceRelevant(audience)) {
                                    long createdAtMs = 0;
                                    com.google.firebase.Timestamp ts = doc.getTimestamp("createdAt");
                                    if (ts != null) {
                                        createdAtMs = ts.toDate().getTime();
                                    } else {
                                        Long l = doc.getLong("createdAt");
                                        if (l != null) createdAtMs = l;
                                    }

                                    String date = doc.getString("date");
                                    if (date == null || date.trim().isEmpty()) {
                                        if (createdAtMs > 0) {
                                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                                            date = sdf.format(new java.util.Date(createdAtMs));
                                        } else {
                                            date = "General";
                                        }
                                    }

                                    String priority = doc.getString("priority");
                                    if (priority == null) priority = "Info";

                                    Announcement ann = new Announcement(
                                        doc.getId(),
                                        doc.getString("title"),
                                        doc.getString("message"),
                                        audience != null ? audience : "All Users",
                                        priority,
                                        date
                                    );
                                    items.add(new EmployerNotificationAdapter.NotificationItem(ann, createdAtMs));
                                }
                            }
                        }

                        // Sort: newest first
                        java.util.Collections.sort(items, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                        if (items.isEmpty()) {
                            if (tvEmptyNotifications != null) tvEmptyNotifications.setVisibility(View.VISIBLE);
                            recyclerNotifications.setVisibility(View.GONE);
                        } else {
                            if (tvEmptyNotifications != null) tvEmptyNotifications.setVisibility(View.GONE);
                            recyclerNotifications.setVisibility(View.VISIBLE);
                            notificationAdapter = new EmployerNotificationAdapter(items);
                            recyclerNotifications.setAdapter(notificationAdapter);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to load announcements", Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
            });
    }

    private boolean isAudienceRelevant(String audience) {
        if (audience == null || audience.trim().isEmpty()) return true;
        String a = audience.trim().toLowerCase();
        return a.contains("all") || a.contains("everyone") || a.contains("public") || a.contains("freelancer");
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
            String reviewText = etReviewText.getText().toString().trim();
            if (reviewText.isEmpty()) {
                etReviewText.setError("Required");
                return;
            }

            btnSubmit.setEnabled(false);
            btnSubmit.setText("Submitting…");

            // Resolve company from cached job
            Job job = DataManager.getJobById(jobId);
            String company = job != null ? job.getCompany() : "";

            String reviewerUid = currentUid != null ? currentUid : "";
            // Use deterministic document ID (reviewerUid_jobId) to prevent duplicate reviews per user per job
            String reviewId = reviewerUid.isEmpty() ? String.valueOf(System.currentTimeMillis()) : reviewerUid + "_" + jobId;

            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", reviewId);
            map.put("jobId", jobId);
            map.put("jobTitle", jobTitle);
            map.put("company", company);
            map.put("reviewerUid", reviewerUid);
            map.put("rating", rating);
            map.put("reviewText", reviewText);
            map.put("createdAt", System.currentTimeMillis());

            db.collection("Reviews").document(reviewId).set(map)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Review submitted! Thank you.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadApplications();
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit");
                    Toast.makeText(this, "Failed to submit review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        });

        dialog.show();
    }

    // --- ApplicationAdapter.OnApplicationActionListener callbacks ---

    @Override
    public void onDeleteApplication(String appId) {
        db.collection("Applications").document(appId).delete().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Application removed", Toast.LENGTH_SHORT).show();
            loadApplications();
        });
        DataManager.deleteApplication(appId);
    }

    @Override
    public void onReviewApplication(String jobId, String jobTitle) {
        if (currentUid == null || currentUid.isEmpty()) {
            showReviewDialog(jobId, jobTitle);
            return;
        }

        String reviewDocId = currentUid + "_" + jobId;
        db.collection("Reviews").document(reviewDocId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    new AlertDialog.Builder(this)
                        .setTitle("Already Reviewed")
                        .setMessage("You have already submitted a review for \"" + jobTitle + "\".")
                        .setPositiveButton("OK", null)
                        .show();
                    loadApplications();
                } else {
                    showReviewDialog(jobId, jobTitle);
                }
            })
            .addOnFailureListener(e -> showReviewDialog(jobId, jobTitle));
    }
}

