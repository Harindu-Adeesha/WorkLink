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
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FreelancerDashboardActivity extends AppCompatActivity {

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
        jobAdapter = new JobAdapter(DataManager.getJobs());
        recyclerJobs.setAdapter(jobAdapter);

        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                loadJobs("All");
                return;
            }
            int checkedId = checkedIds.get(0);
            String category = "All";
            if (checkedId == R.id.chip_dev) {
                category = "Software Development";
            } else if (checkedId == R.id.chip_design) {
                category = "UI/UX Design";
            } else if (checkedId == R.id.chip_writing) {
                category = "Content Writing";
            } else if (checkedId == R.id.chip_marketing) {
                category = "Digital Marketing";
            }
            loadJobs(category);
        });

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
        List<DataManager.Job> jobsList = DataManager.getJobsByCategory(category);
        jobAdapter.updateJobs(jobsList);
    }

    // Applications Functions
    private void loadApplications() {
        List<DataManager.Application> appsList = DataManager.getApplications();
        if (appsList.isEmpty()) {
            tvEmptyApplications.setVisibility(View.VISIBLE);
            recyclerApplications.setVisibility(View.GONE);
        } else {
            tvEmptyApplications.setVisibility(View.GONE);
            recyclerApplications.setVisibility(View.VISIBLE);
            if (appAdapter == null) {
                appAdapter = new ApplicationAdapter(appsList);
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

            DataManager.Review newReview = new DataManager.Review(
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

    // JOB ADAPTER CLASS
    private class JobAdapter extends RecyclerView.Adapter<JobAdapter.ViewHolder> {
        private List<DataManager.Job> jobsList;

        public JobAdapter(List<DataManager.Job> jobsList) {
            this.jobsList = jobsList;
        }

        public void updateJobs(List<DataManager.Job> newList) {
            this.jobsList = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_job, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DataManager.Job job = jobsList.get(position);
            holder.tvTitle.setText(job.getTitle());
            holder.tvCompany.setText(job.getCompany());
            holder.tvCategory.setText(job.getCategory());
            holder.tvSalary.setText(job.getSalary());
            holder.tvRating.setText("★ " + job.getEmployerRating());

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(FreelancerDashboardActivity.this, JobDetailsActivity.class);
                intent.putExtra("job_id", job.getId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return jobsList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvCompany, tvCategory, tvSalary, tvRating;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_job_title);
                tvCompany = itemView.findViewById(R.id.tv_job_company);
                tvCategory = itemView.findViewById(R.id.tv_job_category);
                tvSalary = itemView.findViewById(R.id.tv_job_salary);
                tvRating = itemView.findViewById(R.id.tv_job_rating);
            }
        }
    }

    // APPLICATION ADAPTER CLASS
    private class ApplicationAdapter extends RecyclerView.Adapter<ApplicationAdapter.ViewHolder> {
        private List<DataManager.Application> appsList;

        public ApplicationAdapter(List<DataManager.Application> appsList) {
            this.appsList = appsList;
        }

        public void updateApplications(List<DataManager.Application> newList) {
            this.appsList = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_application, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DataManager.Application app = appsList.get(position);
            holder.tvTitle.setText(app.getJob().getTitle());
            holder.tvCompany.setText(app.getJob().getCompany());
            holder.tvResume.setText("Resume: " + app.getResumeFileName());
            holder.tvStatus.setText(app.getStatus());

            // Set color based on status
            if ("Accepted".equalsIgnoreCase(app.getStatus())) {
                holder.tvStatus.setTextColor(0xFF198754); // green
                holder.tvStatus.setBackgroundColor(0xFFD1E7DD);
            } else if ("Rejected".equalsIgnoreCase(app.getStatus())) {
                holder.tvStatus.setTextColor(0xFFDC3545); // red
                holder.tvStatus.setBackgroundColor(0xFFF8D7DA);
            } else {
                holder.tvStatus.setTextColor(0xFF856404); // yellow/orange
                holder.tvStatus.setBackgroundColor(0xFFFFF3CD);
            }

            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(FreelancerDashboardActivity.this)
                        .setTitle("Cancel Application")
                        .setMessage("Are you sure you want to cancel your application for " + app.getJob().getTitle() + "?")
                        .setPositiveButton("Cancel Application", (dialog, which) -> {
                            DataManager.deleteApplication(app.getId());
                            Toast.makeText(FreelancerDashboardActivity.this, "Application cancelled", Toast.LENGTH_SHORT).show();
                            loadApplications();
                        })
                        .setNegativeButton("Keep Application", null)
                        .show();
            });

            holder.btnReview.setOnClickListener(v -> {
                showReviewDialog(app.getJob().getId(), app.getJob().getTitle());
            });
        }

        @Override
        public int getItemCount() {
            return appsList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvCompany, tvResume, tvStatus;
            Button btnReview, btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_app_title);
                tvCompany = itemView.findViewById(R.id.tv_app_company);
                tvResume = itemView.findViewById(R.id.tv_app_resume);
                tvStatus = itemView.findViewById(R.id.tv_app_status);
                btnReview = itemView.findViewById(R.id.btn_app_review);
                btnDelete = itemView.findViewById(R.id.btn_app_delete);
            }
        }
    }
}
