package com.nibm.worklink;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecruiterDashboardActivity extends AppCompatActivity {

    private View layoutDashboardTab;
    private View layoutJobsTab;
    private View layoutUsersTab;
    private View layoutReviewsTab;
    private View layoutProfileTab;
    private BottomNavigationView bottomNav;

    // Adapters & User Data
    private RecruiterJobAdapter jobAdapter;
    private RecruiterUserAdapter userAdapter;
    private List<RecruiterUserAdapter.UserModel> allVerifyUsers = new ArrayList<>();
    private TextInputEditText etSearchVerifyUsers;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUid;

    // Profile Views
    private MaterialCardView cardProfileView;
    private MaterialCardView cardProfileEdit;
    private TextView tvProfileName, tvProfileRole, tvProfileEmail, tvProfileBio, tvProfileSkills;
    private TextInputEditText etProfileName, etProfileBio, etProfileSkills;
    private Button btnEditProfile, btnSaveProfile, btnCancelProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recruiter_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        currentUid = currentUser != null ? currentUser.getUid() : null;

        // Bind Tab Layouts
        layoutDashboardTab = findViewById(R.id.layout_recruiter_dashboard_tab);
        layoutJobsTab      = findViewById(R.id.layout_recruiter_jobs_tab);
        layoutUsersTab     = findViewById(R.id.layout_recruiter_users_tab);
        layoutReviewsTab   = findViewById(R.id.layout_recruiter_reviews_tab);
        layoutProfileTab   = findViewById(R.id.layout_recruiter_profile_tab);

        // Bind Profile Views
        cardProfileView = findViewById(R.id.card_recruiter_profile_view);
        cardProfileEdit = findViewById(R.id.card_recruiter_profile_edit);

        tvProfileName   = findViewById(R.id.tv_profile_name);
        tvProfileRole   = findViewById(R.id.tv_profile_role);
        tvProfileEmail  = findViewById(R.id.tv_profile_email);
        tvProfileBio    = findViewById(R.id.tv_profile_bio);
        tvProfileSkills = findViewById(R.id.tv_profile_skills);

        etProfileName   = findViewById(R.id.et_recruiter_profile_name);
        etProfileBio    = findViewById(R.id.et_recruiter_profile_bio);
        etProfileSkills = findViewById(R.id.et_recruiter_profile_skills);

        btnEditProfile   = findViewById(R.id.btn_edit_recruiter_profile);
        btnSaveProfile   = findViewById(R.id.btn_save_recruiter_profile);
        btnCancelProfile = findViewById(R.id.btn_cancel_recruiter_profile);

        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> enterProfileEditMode());
        }
        if (btnCancelProfile != null) {
            btnCancelProfile.setOnClickListener(v -> exitProfileEditMode());
        }
        if (btnSaveProfile != null) {
            btnSaveProfile.setOnClickListener(v -> saveProfileData());
        }

        // Search Bar for Verify Users
        etSearchVerifyUsers = findViewById(R.id.et_search_verify_users);
        if (etSearchVerifyUsers != null) {
            etSearchVerifyUsers.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterVerifyUsers(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Bind Bottom Navigation
        bottomNav = findViewById(R.id.recruiter_bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_recruiter_dashboard);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_recruiter_dashboard) {
                switchTab(layoutDashboardTab);
                return true;
            } else if (id == R.id.nav_recruiter_jobs) {
                switchTab(layoutJobsTab);
                loadJobs();
                return true;
            } else if (id == R.id.nav_recruiter_users) {
                switchTab(layoutUsersTab);
                loadUsers();
                return true;
            } else if (id == R.id.nav_recruiter_reviews) {
                switchTab(layoutReviewsTab);
                loadReviews();
                return true;
            } else if (id == R.id.nav_recruiter_profile) {
                switchTab(layoutProfileTab);
                loadProfileTab();
                return true;
            }
            return false;
        });

        // Initialize RecyclerViews
        RecyclerView recyclerJobs = findViewById(R.id.recycler_recruiter_jobs);
        recyclerJobs.setLayoutManager(new LinearLayoutManager(this));
        jobAdapter = new RecruiterJobAdapter(new ArrayList<>());
        recyclerJobs.setAdapter(jobAdapter);

        RecyclerView recyclerUsers = findViewById(R.id.recycler_recruiter_users);
        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new RecruiterUserAdapter(new ArrayList<>());
        recyclerUsers.setAdapter(userAdapter);

        // Dashboard shortcut buttons
        Button btnVerifyJobs = findViewById(R.id.btn_verify_jobs);
        Button btnVerifyUsers = findViewById(R.id.btn_verify_users);
        Button btnViewReviews = findViewById(R.id.btn_view_reviews);

        if (btnVerifyJobs != null) btnVerifyJobs.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_recruiter_jobs));
        if (btnVerifyUsers != null) btnVerifyUsers.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_recruiter_users));
        if (btnViewReviews != null) btnViewReviews.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_recruiter_reviews));

        // Logout button
        Button btnLogout = findViewById(R.id.btn_recruiter_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        mAuth.signOut();
                        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null) {
            int selectedId = bottomNav.getSelectedItemId();
            if (selectedId == R.id.nav_recruiter_jobs) {
                loadJobs();
            } else if (selectedId == R.id.nav_recruiter_users) {
                loadUsers();
            } else if (selectedId == R.id.nav_recruiter_profile) {
                loadProfileTab();
            }
        }
    }

    private void switchTab(View activeTab) {
        layoutDashboardTab.setVisibility(View.GONE);
        layoutJobsTab.setVisibility(View.GONE);
        layoutUsersTab.setVisibility(View.GONE);
        layoutReviewsTab.setVisibility(View.GONE);
        layoutProfileTab.setVisibility(View.GONE);

        activeTab.setVisibility(View.VISIBLE);
    }

    private void loadProfileTab() {
        if (currentUid == null && mAuth.getCurrentUser() != null) {
            currentUid = mAuth.getCurrentUser().getUid();
        }

        if (currentUid != null) {
            db.collection("Users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name  = doc.getString("name");
                        String email = doc.getString("email");
                        String role  = doc.getString("role");
                        String bio   = doc.getString("bio");
                        String skills = doc.getString("skills");

                        if (tvProfileName != null) tvProfileName.setText(name != null ? name : "Recruiter Name");
                        if (tvProfileEmail != null) tvProfileEmail.setText(email != null ? email : (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : ""));
                        if (tvProfileRole != null) tvProfileRole.setText(role != null ? role : "Recruiter");
                        if (tvProfileBio != null) tvProfileBio.setText(bio != null && !bio.trim().isEmpty() ? bio : "No bio provided yet.");
                        if (tvProfileSkills != null) tvProfileSkills.setText(skills != null && !skills.trim().isEmpty() ? skills : "No skills listed yet.");
                    } else {
                        if (tvProfileName != null) tvProfileName.setText("Recruiter");
                        if (tvProfileEmail != null && mAuth.getCurrentUser() != null) tvProfileEmail.setText(mAuth.getCurrentUser().getEmail());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile details", Toast.LENGTH_SHORT).show());
        }
    }

    private void enterProfileEditMode() {
        if (cardProfileView != null) cardProfileView.setVisibility(View.GONE);
        if (cardProfileEdit != null) cardProfileEdit.setVisibility(View.VISIBLE);

        if (currentUid != null) {
            db.collection("Users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name   = doc.getString("name");
                        String bio    = doc.getString("bio");
                        String skills = doc.getString("skills");

                        if (etProfileName != null) etProfileName.setText(name != null ? name : "");
                        if (etProfileBio != null) etProfileBio.setText(bio != null ? bio : "");
                        if (etProfileSkills != null) etProfileSkills.setText(skills != null ? skills : "");
                    }
                });
        }
    }

    private void exitProfileEditMode() {
        if (cardProfileEdit != null) cardProfileEdit.setVisibility(View.GONE);
        if (cardProfileView != null) cardProfileView.setVisibility(View.VISIBLE);
    }

    private void saveProfileData() {
        String name   = etProfileName != null ? etProfileName.getText().toString().trim() : "";
        String bio    = etProfileBio != null ? etProfileBio.getText().toString().trim() : "";
        String skills = etProfileSkills != null ? etProfileSkills.getText().toString().trim() : "";

        if (name.isEmpty()) {
            if (etProfileName != null) etProfileName.setError("Name is required");
            return;
        }

        if (currentUid == null && mAuth.getCurrentUser() != null) {
            currentUid = mAuth.getCurrentUser().getUid();
        }

        if (currentUid == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("bio", bio);
        updates.put("skills", skills);

        db.collection("Users").document(currentUid).update(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Profile Saved Successfully!", Toast.LENGTH_SHORT).show();
                exitProfileEditMode();
                loadProfileTab();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadJobs() {
        RecyclerView recyclerJobs = findViewById(R.id.recycler_recruiter_jobs);
        db.collection("Jobs").get().addOnCompleteListener(task -> {
            List<Job> jobList = new ArrayList<>();
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult()) {
                    Job job = doc.toObject(Job.class);
                    if (job != null && !job.isVerified() && !"Verified".equalsIgnoreCase(job.getStatus())) {
                        jobList.add(job);
                    }
                }
            }
            jobAdapter = new RecruiterJobAdapter(jobList);
            recyclerJobs.setAdapter(jobAdapter);
        });
    }

    private void loadUsers() {
        RecyclerView recyclerUsers = findViewById(R.id.recycler_recruiter_users);
        db.collection("Users").get().addOnCompleteListener(task -> {
            allVerifyUsers.clear();
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult()) {
                    String uid = doc.getId();
                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    String role = doc.getString("role");
                    String skills = doc.getString("skills");
                    String bio = doc.getString("bio");
                    Boolean isVerified = doc.getBoolean("isVerified");
                    boolean verified = isVerified != null && isVerified;

                    if (name == null) name = "User";
                    if (role == null) role = "Member";

                    // Only show unverified non-admin and non-recruiter users in verify users tab
                    if (!verified && !"Admin".equalsIgnoreCase(role) && !"Recruiter".equalsIgnoreCase(role)) {
                        allVerifyUsers.add(new RecruiterUserAdapter.UserModel(uid, name, email, role, skills, bio, false));
                    }
                }
            }
            userAdapter = new RecruiterUserAdapter(new ArrayList<>(allVerifyUsers));
            recyclerUsers.setAdapter(userAdapter);

            String query = etSearchVerifyUsers != null && etSearchVerifyUsers.getText() != null ? etSearchVerifyUsers.getText().toString() : "";
            if (!query.trim().isEmpty()) {
                filterVerifyUsers(query);
            }
        });
    }

    private void filterVerifyUsers(String query) {
        if (allVerifyUsers == null) return;
        List<RecruiterUserAdapter.UserModel> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();

        for (RecruiterUserAdapter.UserModel user : allVerifyUsers) {
            boolean match = lowerQuery.isEmpty()
                    || (user.name != null && user.name.toLowerCase().contains(lowerQuery))
                    || (user.email != null && user.email.toLowerCase().contains(lowerQuery))
                    || (user.role != null && user.role.toLowerCase().contains(lowerQuery))
                    || (user.skills != null && user.skills.toLowerCase().contains(lowerQuery))
                    || (user.bio != null && user.bio.toLowerCase().contains(lowerQuery));

            if (match) {
                filtered.add(user);
            }
        }

        if (userAdapter != null) {
            userAdapter.updateList(filtered);
        }
    }

    private void loadReviews() {
        RecyclerView recyclerReviews = findViewById(R.id.recycler_recruiter_reviews);
        recyclerReviews.setLayoutManager(new LinearLayoutManager(this));

        FirebaseFirestore.getInstance().collection("Reviews")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Review> reviews = new ArrayList<>();
                if (queryDocumentSnapshots != null) {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Review r = doc.toObject(Review.class);
                        if (r != null) {
                            if (r.getId() == null) r.setId(doc.getId());
                            reviews.add(r);
                        }
                    }
                }
                RecruiterReviewAdapter reviewAdapter = new RecruiterReviewAdapter(reviews);
                recyclerReviews.setAdapter(reviewAdapter);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
            });
    }
}
