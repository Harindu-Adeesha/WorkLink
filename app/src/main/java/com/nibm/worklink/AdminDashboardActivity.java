package com.nibm.worklink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class AdminDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ListenerRegistration categoryListener;
    private ListenerRegistration userListener;
    private TextView tvCategoryCount;
    private TextView tvUserCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        tvCategoryCount = findViewById(R.id.tv_category_count);
        tvUserCount = findViewById(R.id.tv_user_count);

        MaterialCardView cardCategories = findViewById(R.id.card_categories);
        if (cardCategories != null) {
            cardCategories.setOnClickListener(v -> {
                Intent intent = new Intent(AdminDashboardActivity.this, CategoryManagementActivity.class);
                startActivity(intent);
            });
        }

        MaterialCardView cardUsers = findViewById(R.id.card_users);
        if (cardUsers != null) {
            cardUsers.setOnClickListener(v -> {
                Intent intent = new Intent(AdminDashboardActivity.this, UserManagementActivity.class);
                startActivity(intent);
            });
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) return true;
            
            Intent intent = null;
            if (id == R.id.nav_categories) intent = new Intent(this, CategoryManagementActivity.class);
            else if (id == R.id.nav_users) intent = new Intent(this, UserManagementActivity.class);
            else if (id == R.id.nav_announcements) intent = new Intent(this, AnnouncementManagementActivity.class);
            
            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
        
        ImageView ivProfile = findViewById(R.id.iv_profile);
        if (ivProfile != null) {
            ivProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }

        loadCounts();
    }

    private void loadCounts() {
        categoryListener = db.collection("Categories")
                .addSnapshotListener((snapshots, error) -> {
                    if (error == null && snapshots != null && tvCategoryCount != null) {
                        tvCategoryCount.setText(String.valueOf(snapshots.size()));
                    }
                });

        userListener = db.collection("Users")
                .addSnapshotListener((snapshots, error) -> {
                    if (error == null && snapshots != null && tvUserCount != null) {
                        tvUserCount.setText(String.valueOf(snapshots.size()));
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (categoryListener != null) {
            categoryListener.remove();
        }
        if (userListener != null) {
            userListener.remove();
        }
    }
}


