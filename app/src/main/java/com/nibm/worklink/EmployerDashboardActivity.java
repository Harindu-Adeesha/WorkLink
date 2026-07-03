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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployerDashboardActivity extends AppCompatActivity {

    private View layoutJobsTab;
    private View layoutApplicationsTab;
    private View layoutProfileTab;
    private BottomNavigationView bottomNav;

    // My Jobs Views
    private RecyclerView recyclerJobs;
    private EmployerJobAdapter jobAdapter;
    private TextView tvEmptyJobs;

    // Applications Views
    private RecyclerView recyclerApplications;
    private EmployerApplicationAdapter appAdapter;
    private TextView tvEmptyApplications;

    // Profile Views
    private View profileEmptyState;
    private View profileViewState;
    private View profileEditState;

    private TextView tvProfileCompanyName, tvProfileEmail, tvProfileContact, tvProfileRating, tvProfileDesc;
    private EditText etProfileCompanyName, etProfileEmail, etProfileContact, etProfileDesc;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUid;
    private String employerEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employer_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
        currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        // Bind Tabs Layouts
        layoutJobsTab = findViewById(R.id.layout_employer_jobs_tab);
        layoutApplicationsTab = findViewById(R.id.layout_employer_applications_tab);
        layoutProfileTab = findViewById(R.id.layout_employer_profile_tab);

        // Bind Bottom Navigation
        bottomNav = findViewById(R.id.employer_bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_employer_jobs);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_employer_jobs) {
                switchTab(layoutJobsTab);
                loadEmployerJobs();
                return true;
            } else if (id == R.id.nav_employer_applications) {
                switchTab(layoutApplicationsTab);
                loadEmployerApplications();
                return true;
            } else if (id == R.id.nav_employer_profile) {
                switchTab(layoutProfileTab);
                loadEmployerProfileTab();
                return true;
            }
            return false;
        });

        // Initialize My Jobs Tab
        recyclerJobs = findViewById(R.id.recycler_employer_jobs);
        recyclerJobs.setLayoutManager(new LinearLayoutManager(this));
        tvEmptyJobs = findViewById(R.id.tv_empty_employer_jobs);
        jobAdapter = new EmployerJobAdapter(new ArrayList<>());
        recyclerJobs.setAdapter(jobAdapter);

        Button btnPostNewJob = findViewById(R.id.btn_post_new_job);
        btnPostNewJob.setOnClickListener(v -> {
            showPostJobDialog(null);
        });

        // Initialize Applications Tab
        recyclerApplications = findViewById(R.id.recycler_employer_applications);
        recyclerApplications.setLayoutManager(new LinearLayoutManager(this));
        tvEmptyApplications = findViewById(R.id.tv_empty_employer_applications);
        appAdapter = new EmployerApplicationAdapter(new ArrayList<>());
        recyclerApplications.setAdapter(appAdapter);

        // Initialize Profile Tab Views
        profileEmptyState = findViewById(R.id.employer_profile_empty_state);
        profileViewState = findViewById(R.id.employer_profile_view_state);
        profileEditState = findViewById(R.id.employer_profile_edit_state);

        tvProfileCompanyName = findViewById(R.id.tv_employer_profile_name);
        tvProfileEmail = findViewById(R.id.tv_employer_profile_email);
        tvProfileContact = findViewById(R.id.tv_employer_profile_contact);
        tvProfileRating = findViewById(R.id.tv_employer_profile_rating);
        tvProfileDesc = findViewById(R.id.tv_employer_profile_desc);

        etProfileCompanyName = findViewById(R.id.et_employer_profile_name);
        etProfileEmail = findViewById(R.id.et_employer_profile_email);
        etProfileContact = findViewById(R.id.et_employer_profile_contact);
        etProfileDesc = findViewById(R.id.et_employer_profile_desc);

        Button btnCreateProfileEmpty = findViewById(R.id.btn_create_employer_profile_empty);
        Button btnEditProfile = findViewById(R.id.btn_edit_employer_profile);
        Button btnDeleteProfile = findViewById(R.id.btn_delete_employer_profile);
        Button btnSaveProfile = findViewById(R.id.btn_save_employer_profile);
        Button btnCancelProfileEdit = findViewById(R.id.btn_cancel_employer_profile_edit);
        Button btnLogout = findViewById(R.id.btn_employer_logout);
        Button btnReviewApp = findViewById(R.id.btn_review_app);

        btnCreateProfileEmpty.setOnClickListener(v -> enterProfileEditMode(true));
        btnEditProfile.setOnClickListener(v -> enterProfileEditMode(false));
        btnDeleteProfile.setOnClickListener(v -> showDeleteProfileConfirmation());
        btnSaveProfile.setOnClickListener(v -> saveProfileData());
        btnCancelProfileEdit.setOnClickListener(v -> loadEmployerProfileTab());
        btnReviewApp.setOnClickListener(v -> showAppReviewDialog());
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(EmployerDashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Initialize Feed
        loadEmployerJobs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEmployerJobs();
        loadEmployerApplications();
        loadEmployerProfileTab();
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

    // Helper to get active email of employer
    private String getEmployerEmail() {
        return employerEmail != null && !employerEmail.isEmpty() ? employerEmail : "employer@worklink.com";
    }

    // Load Jobs Posted by This Employer
    private void loadEmployerJobs() {
        String email = getEmployerEmail();
        List<DataManager.Job> allJobs = DataManager.getJobs();
        List<DataManager.Job> myJobs = new ArrayList<>();
        for (DataManager.Job job : allJobs) {
            if (job.getEmployerContact().equalsIgnoreCase(email)) {
                myJobs.add(job);
            }
        }

        if (myJobs.isEmpty()) {
            tvEmptyJobs.setVisibility(View.VISIBLE);
            recyclerJobs.setVisibility(View.GONE);
        } else {
            tvEmptyJobs.setVisibility(View.GONE);
            recyclerJobs.setVisibility(View.VISIBLE);
            jobAdapter.updateJobs(myJobs);
        }
    }

    // Load Applications Submitted to This Employer's Jobs
    private void loadEmployerApplications() {
        String email = getEmployerEmail();
        List<DataManager.Application> allApps = DataManager.getApplications();
        List<DataManager.Application> myApps = new ArrayList<>();
        for (DataManager.Application app : allApps) {
            if (app.getJob().getEmployerContact().equalsIgnoreCase(email)) {
                myApps.add(app);
            }
        }

        // --- FALLBACK FOR TESTING ---
        // If there are no applications for the logged-in user, but there are hardcoded apps,
        // show the hardcoded apps so the user can see what the UI looks like.
        if (myApps.isEmpty() && !allApps.isEmpty()) {
            myApps.addAll(allApps);
        }

        if (myApps.isEmpty()) {
            tvEmptyApplications.setVisibility(View.VISIBLE);
            recyclerApplications.setVisibility(View.GONE);
        } else {
            tvEmptyApplications.setVisibility(View.GONE);
            recyclerApplications.setVisibility(View.VISIBLE);
            appAdapter.updateApplications(myApps);
        }
    }

    // Profile Management
    private void loadEmployerProfileTab() {
        profileEditState.setVisibility(View.GONE);
        if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);

        if (currentUid == null) {
            profileEmptyState.setVisibility(View.VISIBLE);
            profileViewState.setVisibility(View.GONE);
            return;
        }

        db.collection("Users").document(currentUid).get()
            .addOnSuccessListener(doc -> {
                String name    = doc.getString("name");
                String email   = doc.getString("email");
                String contact = doc.getString("contact");
                String bio     = doc.getString("bio");
                if (bio == null || bio.isEmpty()) {
                    bio = doc.getString("skills"); // From registration mapping
                }
                
                employerEmail = email; // Cache email for jobs filtering

                boolean hasProfile = name != null && !name.isEmpty();
                profileEmptyState.setVisibility(hasProfile ? View.GONE  : View.VISIBLE);
                profileViewState.setVisibility(hasProfile  ? View.VISIBLE: View.GONE);

                if (hasProfile) {
                    tvProfileCompanyName.setText(name);
                    tvProfileEmail.setText(email != null ? email : "");
                    tvProfileContact.setText("Contact: " + (contact != null ? contact : "Not set"));
                    tvProfileRating.setText("★ 5.0 Rating"); // Hardcoded rating for now
                    tvProfileDesc.setText(bio != null ? bio : "");
                }
                // Refresh feed to use fetched email
                loadEmployerJobs();
                loadEmployerApplications();
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

        TextView formTitle = findViewById(R.id.tv_employer_form_title);
        formTitle.setText(isNew ? "Create Company Profile" : "Update Company Profile");

        if (!isNew && currentUid != null) {
            db.collection("Users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    etProfileCompanyName.setText(doc.getString("name") != null ? doc.getString("name") : "");
                    etProfileEmail.setText(doc.getString("email") != null ? doc.getString("email") : "");
                    etProfileContact.setText(doc.getString("contact") != null ? doc.getString("contact") : "");
                    String bio = doc.getString("bio");
                    if (bio == null || bio.isEmpty()) {
                        bio = doc.getString("skills");
                    }
                    etProfileDesc.setText(bio != null ? bio : "");
                });
        } else {
            etProfileCompanyName.setText("");
            etProfileEmail.setText("");
            etProfileContact.setText("");
            etProfileDesc.setText("");
        }
    }

    private void saveProfileData() {
        String name = etProfileCompanyName.getText().toString().trim();
        String email = etProfileEmail.getText().toString().trim();
        String contact = etProfileContact.getText().toString().trim();
        String desc = etProfileDesc.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || contact.isEmpty()) {
            Toast.makeText(this, "Company Name, Email, and Contact are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUid == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("contact", contact);
        updates.put("bio", desc);

        db.collection("Users").document(currentUid).update(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Company Profile Saved Successfully", Toast.LENGTH_SHORT).show();
                loadEmployerProfileTab();
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
    }

    private void showDeleteProfileConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Profile")
                .setMessage("Are you sure you want to clear your company profile details? All job postings may lose reference to your profile.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    if (currentUid == null) return;
                    Map<String, Object> cleared = new HashMap<>();
                    cleared.put("contact", "");
                    cleared.put("bio", "");
                    db.collection("Users").document(currentUid).update(cleared)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Company Profile Cleared", Toast.LENGTH_SHORT).show();
                            loadEmployerProfileTab();
                        });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Post / Update Job Dialog
    private void showPostJobDialog(DataManager.Job existingJob) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_post_job, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.dialog_job_title);
        EditText etTitle = dialogView.findViewById(R.id.et_dialog_job_title);
        EditText etSalary = dialogView.findViewById(R.id.et_dialog_job_salary);
        EditText etCategory = dialogView.findViewById(R.id.et_dialog_job_category);
        EditText etDeadline = dialogView.findViewById(R.id.et_dialog_job_deadline);
        EditText etDesc = dialogView.findViewById(R.id.et_dialog_job_desc);
        Button btnSave = dialogView.findViewById(R.id.btn_dialog_job_save);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_job_cancel);

        boolean isEdit = existingJob != null;
        tvTitle.setText(isEdit ? "Update Job Details" : "Post New Job");

        if (isEdit) {
            etTitle.setText(existingJob.getTitle());
            etSalary.setText(existingJob.getSalary());
            etCategory.setText(existingJob.getCategory());
            etDeadline.setText(existingJob.getDeadline());
            etDesc.setText(existingJob.getDescription());
        }

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String salary = etSalary.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String deadline = etDeadline.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();

            if (title.isEmpty() || salary.isEmpty() || category.isEmpty() || deadline.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Please fill in all details.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentUid == null) return;
            btnSave.setEnabled(false);
            btnSave.setText("Saving...");

            db.collection("Users").document(currentUid).get().addOnSuccessListener(doc -> {
                String company = doc.getString("name");
                if (company == null || company.isEmpty()) company = "My Company";
                
                String contact = doc.getString("email");
                if (contact == null || contact.isEmpty()) contact = getEmployerEmail();
                
                String compDesc = doc.getString("bio");
                if (compDesc == null || compDesc.isEmpty()) compDesc = doc.getString("skills");
                if (compDesc == null || compDesc.isEmpty()) compDesc = "Expert industry providers.";
                
                float compRating = 5.0f;

                if (isEdit) {
                    DataManager.Job updated = new DataManager.Job(
                            existingJob.getId(), title, company, desc, salary, category, compDesc, compRating, contact, deadline
                    );
                    DataManager.updateJob(updated);
                    Toast.makeText(this, "Job Listing Updated!", Toast.LENGTH_SHORT).show();
                } else {
                    String newId = String.valueOf(DataManager.getJobs().size() + 1);
                    DataManager.Job newJob = new DataManager.Job(
                            newId, title, company, desc, salary, category, compDesc, compRating, contact, deadline
                    );
                    DataManager.addJob(newJob);
                    Toast.makeText(this, "New Job Posted Successfully!", Toast.LENGTH_SHORT).show();
                }

                loadEmployerJobs();
                dialog.dismiss();
            }).addOnFailureListener(e -> {
                btnSave.setEnabled(true);
                btnSave.setText("Save Details");
                Toast.makeText(this, "Failed to get profile details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });

        dialog.show();
    }

    // App Review Dialog
    private void showAppReviewDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_review, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        RatingBar ratingBar = dialogView.findViewById(R.id.rating_bar);
        EditText etReviewText = dialogView.findViewById(R.id.et_review_text);
        Button btnSubmit = dialogView.findViewById(R.id.btn_submit_review);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel_review);

        tvTitle.setText("Review WorkLink App");
        etReviewText.setHint("What do you think about our platform?");
        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String review = etReviewText.getText().toString().trim();
            if (review.isEmpty()) {
                etReviewText.setError("Required");
                return;
            }

            Toast.makeText(this, "App Review Submitted. Rating: " + rating + "★. Thank you!", Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    // Job Status Dialog
    private void showStatusDialog(DataManager.Application app) {
        String[] statuses = {"Pending", "Under Review", "Shortlisted", "Accepted", "Rejected"};
        new AlertDialog.Builder(this)
                .setTitle("Give Status for Job Application")
                .setItems(statuses, (dialog, which) -> {
                    String selectedStatus = statuses[which];
                    app.setStatus(selectedStatus);
                    Toast.makeText(EmployerDashboardActivity.this, "Status Updated to " + selectedStatus, Toast.LENGTH_SHORT).show();
                    loadEmployerApplications();
                })
                .show();
    }

    // JOB ADAPTER FOR EMPLOYER
    private class EmployerJobAdapter extends RecyclerView.Adapter<EmployerJobAdapter.ViewHolder> {
        private List<DataManager.Job> jobsList;

        public EmployerJobAdapter(List<DataManager.Job> jobsList) {
            this.jobsList = jobsList;
        }

        public void updateJobs(List<DataManager.Job> newList) {
            this.jobsList = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employer_job, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DataManager.Job job = jobsList.get(position);
            holder.tvTitle.setText(job.getTitle());
            holder.tvCompany.setText(job.getCompany());
            holder.tvCategory.setText(job.getCategory());
            holder.tvSalary.setText(job.getSalary());
            holder.tvDeadline.setText("Deadline: " + job.getDeadline());
            holder.tvRating.setText("★ " + job.getEmployerRating());

            holder.btnEdit.setOnClickListener(v -> showPostJobDialog(job));
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(EmployerDashboardActivity.this)
                        .setTitle("Delete Job Listing")
                        .setMessage("Are you sure you want to delete the job post for \"" + job.getTitle() + "\"? All related applications will be deleted too.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            DataManager.deleteJob(job.getId());
                            Toast.makeText(EmployerDashboardActivity.this, "Job listing deleted", Toast.LENGTH_SHORT).show();
                            loadEmployerJobs();
                            loadEmployerApplications(); // also updates application list
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return jobsList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvCompany, tvCategory, tvSalary, tvDeadline, tvRating;
            Button btnEdit, btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_employer_job_title);
                tvCompany = itemView.findViewById(R.id.tv_employer_job_company);
                tvCategory = itemView.findViewById(R.id.tv_employer_job_category);
                tvSalary = itemView.findViewById(R.id.tv_employer_job_salary);
                tvDeadline = itemView.findViewById(R.id.tv_employer_job_deadline);
                tvRating = itemView.findViewById(R.id.tv_employer_job_rating);
                btnEdit = itemView.findViewById(R.id.btn_employer_job_edit);
                btnDelete = itemView.findViewById(R.id.btn_employer_job_delete);
            }
        }
    }

    // APPLICATION ADAPTER FOR EMPLOYER
    private class EmployerApplicationAdapter extends RecyclerView.Adapter<EmployerApplicationAdapter.ViewHolder> {
        private List<DataManager.Application> appsList;

        public EmployerApplicationAdapter(List<DataManager.Application> appsList) {
            this.appsList = appsList;
        }

        public void updateApplications(List<DataManager.Application> newList) {
            this.appsList = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employer_application, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DataManager.Application app = appsList.get(position);
            holder.tvTitle.setText(app.getJob().getTitle());
            holder.tvResume.setText("Resume: " + app.getResumeFileName());
            holder.tvCover.setText(app.getCoverLetter());
            holder.tvStatus.setText(app.getStatus());

            // Set dynamic applicant info
            DataManager.Profile fp = DataManager.getProfile();
            if (fp != null) {
                holder.tvApplicant.setText("Applicant: " + fp.getName() + " (" + fp.getEmail() + ")");
            } else {
                holder.tvApplicant.setText("Applicant: Mock Freelancer (freelancer@worklink.com)");
            }

            // Set status tag styling
            if ("Accepted".equalsIgnoreCase(app.getStatus())) {
                holder.tvStatus.setTextColor(0xFF198754);
                holder.tvStatus.setBackgroundColor(0xFFD1E7DD);
            } else if ("Rejected".equalsIgnoreCase(app.getStatus())) {
                holder.tvStatus.setTextColor(0xFFDC3545);
                holder.tvStatus.setBackgroundColor(0xFFF8D7DA);
            } else {
                holder.tvStatus.setTextColor(0xFF856404);
                holder.tvStatus.setBackgroundColor(0xFFFFF3CD);
            }

            holder.btnGiveStatus.setOnClickListener(v -> showStatusDialog(app));
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(EmployerDashboardActivity.this)
                        .setTitle("Dismiss Application")
                        .setMessage("Are you sure you want to dismiss this application from the feed?")
                        .setPositiveButton("Dismiss", (dialog, which) -> {
                            DataManager.deleteApplication(app.getId());
                            Toast.makeText(EmployerDashboardActivity.this, "Application dismissed", Toast.LENGTH_SHORT).show();
                            loadEmployerApplications();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return appsList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvApplicant, tvResume, tvCover, tvStatus;
            Button btnGiveStatus, btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_employer_app_title);
                tvApplicant = itemView.findViewById(R.id.tv_employer_app_applicant);
                tvResume = itemView.findViewById(R.id.tv_employer_app_resume);
                tvCover = itemView.findViewById(R.id.tv_employer_app_cover);
                tvStatus = itemView.findViewById(R.id.tv_employer_app_status);
                btnGiveStatus = itemView.findViewById(R.id.btn_employer_app_give_status);
                btnDelete = itemView.findViewById(R.id.btn_employer_app_delete);
            }
        }
    }
}
