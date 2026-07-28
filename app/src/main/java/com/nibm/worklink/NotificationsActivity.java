package com.nibm.worklink;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private FirebaseFirestore db;
    private String currentUid;
    private String userRole; // "Employer" or "Freelancer"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        userRole = getIntent().getStringExtra("userRole");

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_notifications);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvRole = findViewById(R.id.tv_notifications_role);
        if (tvRole != null && userRole != null && !userRole.isEmpty()) {
            tvRole.setText(userRole);
        }

        // Setup Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.notifications_bottom_navigation);
        if (bottomNav != null) {
            if ("Employer".equalsIgnoreCase(userRole)) {
                bottomNav.setVisibility(View.VISIBLE);
                bottomNav.setSelectedItemId(R.id.nav_employer_notifications);
                bottomNav.setOnItemSelectedListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.nav_employer_notifications) {
                        return true;
                    }
                    EmployerDashboardActivity.pendingTargetTabId = id;
                    finish();
                    return true;
                });
            } else {
                bottomNav.setVisibility(View.GONE);
            }
        }

        recyclerView = findViewById(R.id.recycler_notifications_page);
        tvEmpty      = findViewById(R.id.tv_empty_notifications_page);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadAll();
    }

    private void loadAll() {
        if (currentUid == null) return;

        List<EmployerNotificationAdapter.NotificationItem> items = new ArrayList<>();

        // If userRole not passed, fetch from Firestore Users collection
        if (userRole == null || userRole.trim().isEmpty()) {
            db.collection("Users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        userRole = doc.getString("role");
                        TextView tvRole = findViewById(R.id.tv_notifications_role);
                        if (tvRole != null && userRole != null) {
                            tvRole.setText(userRole);
                        }
                    }
                    fetchNotificationsAndAnnouncements(items);
                })
                .addOnFailureListener(e -> fetchNotificationsAndAnnouncements(items));
        } else {
            fetchNotificationsAndAnnouncements(items);
        }
    }

    private void fetchNotificationsAndAnnouncements(List<EmployerNotificationAdapter.NotificationItem> items) {
        // 1. Fetch personal Notifications
        db.collection("Notifications")
            .whereEqualTo("recipientId", currentUid)
            .get()
            .addOnSuccessListener(notifSnap -> {
                for (DocumentSnapshot doc : notifSnap) {
                    Notification notif = doc.toObject(Notification.class);
                    if (notif != null) {
                        notif.setId(doc.getId());
                        items.add(new EmployerNotificationAdapter.NotificationItem(notif));
                        db.collection("Notifications").document(doc.getId()).update("read", true);
                    }
                }

                // 2. Fetch Announcements relevant to this user's role
                db.collection("Announcements")
                    .get()
                    .addOnSuccessListener(annSnap -> {
                        for (DocumentSnapshot doc : annSnap) {
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

                        // Sort: newer notifications first
                        Collections.sort(items, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                        if (items.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            recyclerView.setAdapter(new EmployerNotificationAdapter(items));
                        }
                    })
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load announcements", Toast.LENGTH_SHORT).show());
            })
            .addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show());
    }

    private boolean isAudienceRelevant(String audience) {
        if (audience == null || audience.trim().isEmpty()) return true;
        String a = audience.trim().toLowerCase();
        
        // Match "All", "All Users", "Everyone", etc.
        if (a.contains("all") || a.contains("everyone") || a.contains("public")) {
            return true;
        }

        if (userRole != null && !userRole.trim().isEmpty()) {
            String roleLower = userRole.trim().toLowerCase();
            return a.contains(roleLower) || roleLower.contains(a);
        }

        return true;
    }
}
