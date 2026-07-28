package com.nibm.worklink;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AdapterView;
import java.util.Collections;
import java.util.Comparator;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EmployerDashboardActivity extends AppCompatActivity
        implements EmployerJobAdapter.OnJobActionListener,
                   EmployerApplicationAdapter.OnApplicationStatusListener {

    private View layoutJobsTab;
    private View layoutApplicationsTab;
    private View layoutProfileTab;
    private BottomNavigationView bottomNav;

    // Navigation History Stack
    private final java.util.Stack<Integer> tabHistory = new java.util.Stack<>();
    private boolean isNavigatingBack = false;

    // My Jobs Views
    private RecyclerView recyclerJobs;
    private EmployerJobAdapter jobAdapter;
    private TextView tvEmptyJobs;

    // Applications Views
    private RecyclerView recyclerApplications;
    private EmployerApplicationAdapter appAdapter;
    private TextView tvEmptyApplications;
    private EditText etSearchApplications;
    private Spinner spinnerSortApplications;
    private List<Application> allEmployerApplications = new ArrayList<>();

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
    private int lastSelectedNavId = R.id.nav_employer_jobs; // track last real tab

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
            if (id != R.id.nav_employer_notifications) {
                int currentSelectedId = bottomNav.getSelectedItemId();
                if (!isNavigatingBack && currentSelectedId != id) {
                    tabHistory.push(currentSelectedId);
                }
                isNavigatingBack = false;
            }

            if (id == R.id.nav_employer_jobs) {
                lastSelectedNavId = id;
                switchTab(layoutJobsTab);
                loadEmployerJobs();
                return true;
            } else if (id == R.id.nav_employer_applications) {
                lastSelectedNavId = id;
                switchTab(layoutApplicationsTab);
                loadEmployerApplications();
                return true;
            } else if (id == R.id.nav_employer_notifications) {
                // Launch alerts screen; keep nav on the previous real tab
                Intent intent = new Intent(EmployerDashboardActivity.this, NotificationsActivity.class);
                intent.putExtra("userRole", "Employer");
                startActivity(intent);
                // Immediately revert nav highlight back to last real tab so it's correct on return
                bottomNav.post(() -> bottomNav.setSelectedItemId(lastSelectedNavId));
                return false; // returning false prevents the item being highlighted
            } else if (id == R.id.nav_employer_profile) {
                lastSelectedNavId = id;
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
        jobAdapter = new EmployerJobAdapter(new ArrayList<>(), this);
        recyclerJobs.setAdapter(jobAdapter);

        Button btnPostNewJob = findViewById(R.id.btn_post_new_job);
        btnPostNewJob.setOnClickListener(v -> {
            showPostJobDialog(null);
        });

        // Initialize Applications Tab
        recyclerApplications = findViewById(R.id.recycler_employer_applications);
        recyclerApplications.setLayoutManager(new LinearLayoutManager(this));
        tvEmptyApplications = findViewById(R.id.tv_empty_employer_applications);
        etSearchApplications = findViewById(R.id.et_search_applications);
        spinnerSortApplications = findViewById(R.id.spinner_sort_applications);
        
        appAdapter = new EmployerApplicationAdapter(new ArrayList<>(), this);
        recyclerApplications.setAdapter(appAdapter);

        // Setup search and sort (Filter by Status)
        String[] sortOptions = {"All Statuses", "Pending", "Under Review", "Shortlisted", "Accepted", "Rejected"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, sortOptions);
        sortAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerSortApplications.setAdapter(sortAdapter);

        etSearchApplications.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterAndSortApplications(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        spinnerSortApplications.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { filterAndSortApplications(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

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

        // Handle system back button tab navigation
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!tabHistory.isEmpty()) {
                    int previousTabId = tabHistory.pop();
                    isNavigatingBack = true;
                    if (bottomNav != null) bottomNav.setSelectedItemId(previousTabId);
                } else {
                    finish();
                }
            }
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
        db.collection("Jobs").whereEqualTo("employerContact", email).get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Job> myJobs = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    Job job = doc.toObject(Job.class);
                    if (job != null) {
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

                // Fetch real review-based ratings and push to adapter
                RatingRepository.fetchJobRatings(ratingsMap -> {
                    if (jobAdapter != null) jobAdapter.updateRatings(ratingsMap);
                });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load jobs", Toast.LENGTH_SHORT).show();
            });
    }

    // Load Applications Submitted to This Employer's Jobs
    private void loadEmployerApplications() {
        String email = getEmployerEmail();
        db.collection("Applications").whereEqualTo("employerContact", email).get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Application> myApps = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    String appId = doc.getId();
                    String jobId = doc.getString("jobId");
                    String jobTitle = doc.getString("jobTitle");
                    String company = doc.getString("company");
                    String employerContact = doc.getString("employerContact");
                    String coverLetter = doc.getString("coverLetter");
                    String resumeFileName = doc.getString("resumeFileName");
                    String status = doc.getString("status");
                    String applicantName = doc.getString("applicantName");
                    String applicantEmail = doc.getString("applicantEmail");
                    String freelancerUid = doc.getString("freelancerUid");

                    Job dummyJob = new Job(jobId, jobTitle, company, "", "", "", "", 0f, employerContact, "");
                    Application app = new Application(appId, dummyJob, coverLetter, resumeFileName, status, applicantName, applicantEmail);
                    app.setFreelancerUid(freelancerUid);
                    myApps.add(app);

                    // Backward compatibility: fetch missing name/email
                    if (applicantName == null && freelancerUid != null && !freelancerUid.isEmpty()) {
                        db.collection("Users").document(freelancerUid).get().addOnSuccessListener(userDoc -> {
                            app.setApplicantName(userDoc.getString("name"));
                            app.setApplicantEmail(userDoc.getString("email"));
                            if (appAdapter != null) appAdapter.notifyDataSetChanged();
                        });
                    }
                }

                allEmployerApplications = myApps;
                filterAndSortApplications();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load applications", Toast.LENGTH_SHORT).show();
            });
    }

    private void filterAndSortApplications() {
        if (allEmployerApplications == null) return;
        
        String query = etSearchApplications.getText().toString().trim().toLowerCase(Locale.getDefault());
        String sortOption = spinnerSortApplications.getSelectedItem() != null ? spinnerSortApplications.getSelectedItem().toString() : "All Statuses";
        
        List<Application> filtered = new ArrayList<>();
        for (Application app : allEmployerApplications) {
            String appName = app.getApplicantName() != null ? app.getApplicantName().toLowerCase(Locale.getDefault()) : "";
            String jobTitle = app.getJob() != null && app.getJob().getTitle() != null ? app.getJob().getTitle().toLowerCase(Locale.getDefault()) : "";
            String appStatus = app.getStatus() != null ? app.getStatus() : "";
            
            boolean matchesSearch = appName.contains(query) || jobTitle.contains(query);
            boolean matchesStatus = "All Statuses".equals(sortOption) || sortOption.equalsIgnoreCase(appStatus);
            
            if (matchesSearch && matchesStatus) {
                filtered.add(app);
            }
        }
        
        // Optional: still sort them nicely by Name when showing
        Collections.sort(filtered, (a1, a2) -> {
            String n1 = a1.getApplicantName() != null ? a1.getApplicantName() : "";
            String n2 = a2.getApplicantName() != null ? a2.getApplicantName() : "";
            return n1.compareToIgnoreCase(n2);
        });
        
        if (filtered.isEmpty()) {
            tvEmptyApplications.setVisibility(View.VISIBLE);
            recyclerApplications.setVisibility(View.GONE);
        } else {
            tvEmptyApplications.setVisibility(View.GONE);
            recyclerApplications.setVisibility(View.VISIBLE);
            if (appAdapter != null) appAdapter.updateApplications(filtered);
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
    private void showPostJobDialog(Job existingJob) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_post_job, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.dialog_job_title);
        EditText etTitle = dialogView.findViewById(R.id.et_dialog_job_title);
        EditText etSalary = dialogView.findViewById(R.id.et_dialog_job_salary);
        AutoCompleteTextView etCategory = dialogView.findViewById(R.id.et_dialog_job_category);
        EditText etDeadline = dialogView.findViewById(R.id.et_dialog_job_deadline);
        EditText etDesc = dialogView.findViewById(R.id.et_dialog_job_desc);
        Button btnSave = dialogView.findViewById(R.id.btn_dialog_job_save);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_job_cancel);

        // Fetch categories from DB
        db.collection("Categories").get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<String> categories = new ArrayList<>();
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String name = doc.getString("name");
                if (name != null) categories.add(name);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
            etCategory.setAdapter(adapter);
        });

        // Date Picker for Deadline
        etDeadline.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                        etDeadline.setText(formattedDate);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

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
                    Job updated = new Job(
                            existingJob.getId(), title, company, desc, salary, category, compDesc, compRating, contact, deadline
                    );
                    db.collection("Jobs").document(existingJob.getId()).set(updated)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Job Listing Updated!", Toast.LENGTH_SHORT).show();
                            loadEmployerJobs();
                        });
                    DataManager.updateJob(updated);
                } else {
                    DocumentReference ref = db.collection("Jobs").document();
                    String newId = ref.getId();
                    Job newJob = new Job(
                            newId, title, company, desc, salary, category, compDesc, compRating, contact, deadline
                    );
                    ref.set(newJob).addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "New Job Posted Successfully!", Toast.LENGTH_SHORT).show();
                        loadEmployerJobs();
                    });
                    DataManager.addJob(newJob);
                }

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
            String reviewText = etReviewText.getText().toString().trim();
            if (reviewText.isEmpty()) {
                etReviewText.setError("Required");
                return;
            }

            btnSubmit.setEnabled(false);
            btnSubmit.setText("Submitting…");

            String reviewerUid = currentUid != null ? currentUid : "";
            String reviewId = String.valueOf(System.currentTimeMillis());

            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", reviewId);
            map.put("reviewerUid", reviewerUid);
            map.put("rating", rating);
            map.put("reviewText", reviewText);
            map.put("createdAt", System.currentTimeMillis());

            db.collection("AppReviews").document(reviewId).set(map)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "App Review Submitted. Rating: " + rating + "★. Thank you!", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit");
                    Toast.makeText(this, "Failed to submit review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        });

        dialog.show();
    }

    // Job Status Dialog
    private void showStatusDialog(Application app) {
        String[] statuses = {"Pending", "Under Review", "Shortlisted", "Accepted", "Rejected"};
        new AlertDialog.Builder(this)
                .setTitle("Give Status for Job Application")
                .setItems(statuses, (dialog, which) -> {
                    String selectedStatus = statuses[which];
                    app.setStatus(selectedStatus);
                    
                    db.collection("Applications").document(app.getId())
                        .update("status", selectedStatus)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(EmployerDashboardActivity.this, "Status Updated to " + selectedStatus, Toast.LENGTH_SHORT).show();
                            
                            // Send notification to freelancer
                            String recipientUid = app.getFreelancerUid();
                            if (recipientUid != null && !recipientUid.isEmpty()) {
                                String notifId = db.collection("Notifications").document().getId();
                                String title = "Application Status Update";
                                String message = "Your application for " + app.getJob().getTitle() + " has been marked as: " + selectedStatus;
                                Notification notification = new Notification(notifId, recipientUid, title, message, "ApplicationStatus", app.getJob().getId(), app.getJob().getTitle(), System.currentTimeMillis());
                                db.collection("Notifications").document(notifId).set(notification);
                                DataManager.addNotification(notification);
                            }

                            loadEmployerApplications();
                        });
                })
                .show();
    }

    // --- EmployerJobAdapter.OnJobActionListener callbacks ---

    @Override
    public void onEditJob(Job job) {
        showPostJobDialog(job);
    }

    @Override
    public void onDeleteJob(String jobId) {
        db.collection("Jobs").document(jobId).delete()
            .addOnSuccessListener(aVoid -> {
                loadEmployerJobs();
            });
        DataManager.deleteJob(jobId);
        loadEmployerApplications();
    }

    // --- EmployerApplicationAdapter.OnApplicationStatusListener callbacks ---

    @Override
    public void onGiveStatus(Application app) {
        showStatusDialog(app);
    }

    @Override
    public void onDismissApplication(String appId) {
        db.collection("Applications").document(appId).delete()
            .addOnSuccessListener(aVoid -> {
                loadEmployerApplications();
            });
        DataManager.deleteApplication(appId);
    }
}

