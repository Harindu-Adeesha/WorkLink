package com.nibm.worklink;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class RecruiterDashboardActivity extends AppCompatActivity {

    private View layoutDashboardTab;
    private View layoutJobsTab;
    private View layoutUsersTab;
    private View layoutReviewsTab;
    private View layoutProfileTab;
    private BottomNavigationView bottomNav;

    // Adapters
    private RecruiterJobAdapter jobAdapter;
    private RecruiterUserAdapter userAdapter;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recruiter_dashboard);

        db = FirebaseFirestore.getInstance();

        // Bind Tab Layouts
        layoutDashboardTab = findViewById(R.id.layout_recruiter_dashboard_tab);
        layoutJobsTab      = findViewById(R.id.layout_recruiter_jobs_tab);
        layoutUsersTab     = findViewById(R.id.layout_recruiter_users_tab);
        layoutReviewsTab   = findViewById(R.id.layout_recruiter_reviews_tab);
        layoutProfileTab   = findViewById(R.id.layout_recruiter_profile_tab);

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

        btnVerifyJobs.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_recruiter_jobs));
        btnVerifyUsers.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_recruiter_users));
        btnViewReviews.setOnClickListener(v -> bottomNav.setSelectedItemId(R.id.nav_recruiter_reviews));

        // Logout button
        Button btnLogout = findViewById(R.id.btn_recruiter_logout);
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
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

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNav != null && bottomNav.getSelectedItemId() == R.id.nav_recruiter_jobs) {
            loadJobs();
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
        List<RecruiterUserAdapter.UserModel> users = new ArrayList<>();
        users.add(new RecruiterUserAdapter.UserModel("Alice Smith", "Employer Profile"));
        users.add(new RecruiterUserAdapter.UserModel("Bob Johnson", "Freelancer Profile"));
        users.add(new RecruiterUserAdapter.UserModel("Tech Innovators Inc", "Employer Profile"));
        users.add(new RecruiterUserAdapter.UserModel("Sarah Connor", "Freelancer Profile"));
        users.add(new RecruiterUserAdapter.UserModel("David Park", "Employer Profile"));

        RecyclerView recyclerUsers = findViewById(R.id.recycler_recruiter_users);
        userAdapter = new RecruiterUserAdapter(users);
        recyclerUsers.setAdapter(userAdapter);
    }

    private void loadReviews() {
        List<Review> reviews = DataManager.getReviews();
        RecyclerView recyclerReviews = findViewById(R.id.recycler_recruiter_reviews);
        recyclerReviews.setLayoutManager(new LinearLayoutManager(this));
        RecruiterReviewAdapter reviewAdapter = new RecruiterReviewAdapter(new ArrayList<>(reviews));
        recyclerReviews.setAdapter(reviewAdapter);
    }
}
