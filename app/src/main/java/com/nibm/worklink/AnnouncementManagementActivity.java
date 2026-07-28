package com.nibm.worklink;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnnouncementManagementActivity extends AppCompatActivity
        implements AnnouncementAdapter.OnAnnouncementActionListener {

    private RecyclerView recyclerView;
    private AnnouncementAdapter adapter;
    private List<Announcement> allAnnouncements = new ArrayList<>();
    private List<Announcement> displayedAnnouncements = new ArrayList<>();

    private FirebaseFirestore db;
    private ListenerRegistration announcementListener;

    private String currentPriorityFilter = "All Priorities";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcement_management);

        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recycler_announcements);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AnnouncementAdapter(displayedAnnouncements, this);
        recyclerView.setAdapter(adapter);

        // Search bar
        EditText etSearch = findViewById(R.id.et_search_announcements);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Priority filter chips
        ChipGroup chipGroup = findViewById(R.id.chip_group_priority);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int chipId = checkedIds.get(0);
            if (chipId == R.id.chip_prio_all) currentPriorityFilter = "All Priorities";
            else if (chipId == R.id.chip_prio_urgent) currentPriorityFilter = "Urgent";
            else if (chipId == R.id.chip_prio_important) currentPriorityFilter = "Important";
            else if (chipId == R.id.chip_prio_info) currentPriorityFilter = "Info";
            else if (chipId == R.id.chip_prio_maintenance) currentPriorityFilter = "Maintenance";
            applyFilters();
        });

        // FAB - add announcement
        FloatingActionButton fab = findViewById(R.id.fab_add_announcement);
        fab.setOnClickListener(v -> showAnnouncementDialog(null, -1));

        // Bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_announcements);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_announcements) return true;
            Intent intent = null;
            if (id == R.id.nav_dashboard) intent = new Intent(this, AdminDashboardActivity.class);
            else if (id == R.id.nav_categories) intent = new Intent(this, CategoryManagementActivity.class);
            else if (id == R.id.nav_users) intent = new Intent(this, UserManagementActivity.class);
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

        loadAnnouncements();
    }

    private void loadAnnouncements() {
        announcementListener = db.collection("Announcements")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    allAnnouncements.clear();
                    for (var doc : snapshots.getDocuments()) {
                        Announcement ann = new Announcement();
                        ann.setId(doc.getId());
                        ann.setTitle(doc.getString("title"));
                        ann.setMessage(doc.getString("message"));
                        ann.setTargetAudience(doc.getString("targetAudience"));
                        ann.setPriority(doc.getString("priority"));

                        // Format timestamp to readable date string
                        Timestamp ts = doc.getTimestamp("createdAt");
                        if (ts != null) {
                            String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    .format(new Date(ts.toDate().getTime()));
                            ann.setDate(formatted);
                        } else {
                            ann.setDate(doc.getString("date"));
                        }
                        allAnnouncements.add(ann);
                    }
                    applyFilters();
                });
    }

    private void applyFilters() {
        displayedAnnouncements.clear();
        for (Announcement ann : allAnnouncements) {
            boolean matchesPriority = "All Priorities".equals(currentPriorityFilter)
                    || currentPriorityFilter.equalsIgnoreCase(ann.getPriority());

            String title = ann.getTitle() != null ? ann.getTitle().toLowerCase() : "";
            String message = ann.getMessage() != null ? ann.getMessage().toLowerCase() : "";
            String audience = ann.getTargetAudience() != null ? ann.getTargetAudience().toLowerCase() : "";
            boolean matchesSearch = currentSearchQuery.isEmpty()
                    || title.contains(currentSearchQuery)
                    || message.contains(currentSearchQuery)
                    || audience.contains(currentSearchQuery);

            if (matchesPriority && matchesSearch) {
                displayedAnnouncements.add(ann);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showAnnouncementDialog(Announcement existing, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_announcement, null);
        builder.setView(view);

        TextView dialogTitle    = view.findViewById(R.id.dialog_title);
        EditText etTitle        = view.findViewById(R.id.et_announcement_title);
        EditText etMessage      = view.findViewById(R.id.et_announcement_message);
        Spinner spinnerPriority = view.findViewById(R.id.spinner_announcement_priority);
        Spinner spinnerAudience = view.findViewById(R.id.spinner_announcement_audience);
        Button btnCancel        = view.findViewById(R.id.btn_cancel);
        Button btnSave          = view.findViewById(R.id.btn_save);

        // Priority spinner
        String[] priorities = {"Info", "Important", "Urgent", "Maintenance"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_role, priorities);
        priorityAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_role);
        spinnerPriority.setAdapter(priorityAdapter);

        // Audience spinner
        String[] audiences = {"All Users", "Freelancers", "Employers", "Recruiters", "Admins"};
        ArrayAdapter<String> audienceAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_role, audiences);
        audienceAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_role);
        spinnerAudience.setAdapter(audienceAdapter);

        if (existing != null) {
            dialogTitle.setText("Edit Announcement");
            etTitle.setText(existing.getTitle());
            etMessage.setText(existing.getMessage());

            // Pre-select priority
            for (int i = 0; i < priorities.length; i++) {
                if (priorities[i].equalsIgnoreCase(existing.getPriority())) {
                    spinnerPriority.setSelection(i);
                    break;
                }
            }
            // Pre-select audience
            for (int i = 0; i < audiences.length; i++) {
                if (audiences[i].equalsIgnoreCase(existing.getTargetAudience())) {
                    spinnerAudience.setSelection(i);
                    break;
                }
            }
        } else {
            dialogTitle.setText("New Announcement");
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String title   = etTitle.getText().toString().trim();
            String message = etMessage.getText().toString().trim();
            String priority  = spinnerPriority.getSelectedItem().toString();
            String audience  = spinnerAudience.getSelectedItem().toString();

            if (title.isEmpty()) {
                etTitle.setError("Title is required");
                return;
            }
            if (message.isEmpty()) {
                etMessage.setError("Message is required");
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("message", message);
            data.put("priority", priority);
            data.put("targetAudience", audience);
            data.put("createdAt", Timestamp.now());

            if (existing == null) {
                // Create new
                db.collection("Announcements").add(data)
                        .addOnSuccessListener(ref -> Toast.makeText(this, "Announcement posted", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                // Update existing
                db.collection("Announcements").document(existing.getId()).update(data)
                        .addOnSuccessListener(unused -> Toast.makeText(this, "Announcement updated", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onEditAnnouncement(Announcement announcement, int position) {
        showAnnouncementDialog(announcement, position);
    }

    @Override
    public void onDeleteAnnouncement(Announcement announcement, int position) {
        db.collection("Announcements").document(announcement.getId()).delete()
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Announcement deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (announcementListener != null) announcementListener.remove();
    }
}
