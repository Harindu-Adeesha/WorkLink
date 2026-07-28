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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserManagementActivity extends AppCompatActivity
        implements UserAdapter.OnUserActionListener {

    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private final List<User> allUserList = new ArrayList<>();
    private final List<User> displayedUserList = new ArrayList<>();
    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;

    private EditText etSearch;
    private ChipGroup chipGroupRoles;
    private String selectedRoleFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recycler_users);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(displayedUserList, this);
        recyclerView.setAdapter(adapter);

        etSearch = findViewById(R.id.et_search_users);
        chipGroupRoles = findViewById(R.id.chip_group_roles);

        setupSearchAndFilter();

        FloatingActionButton fab = findViewById(R.id.fab_add_user);
        fab.setOnClickListener(v -> showUserDialog(null, -1));

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_users);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_users) return true;
            
            Intent intent = null;
            if (id == R.id.nav_dashboard) intent = new Intent(this, AdminDashboardActivity.class);
            else if (id == R.id.nav_categories) intent = new Intent(this, CategoryManagementActivity.class);
            else if (id == R.id.nav_announcements) intent = new Intent(this, AnnouncementManagementActivity.class);
            
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        ImageView ivProfile = findViewById(R.id.iv_profile);
        if (ivProfile != null) {
            ivProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }

        listenToUserUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_users);
        }
    }

    private void setupSearchAndFilter() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAndSortUsers();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipGroupRoles.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedRoleFilter = "All";
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chip_role_freelancer) selectedRoleFilter = "Freelancer";
                else if (id == R.id.chip_role_employer) selectedRoleFilter = "Employer";
                else if (id == R.id.chip_role_recruiter) selectedRoleFilter = "Recruiter";
                else if (id == R.id.chip_role_admin) selectedRoleFilter = "Admin";
                else selectedRoleFilter = "All";
            }
            filterAndSortUsers();
        });
    }

    private void listenToUserUpdates() {
        userListener = db.collection("Users")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading users: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (snapshots != null) {
                        allUserList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String uid = doc.getId();
                            String name = doc.getString("name");
                            String email = doc.getString("email");
                            String role = doc.getString("role");
                            String skills = doc.getString("skills");
                            String bio = doc.getString("bio");

                            if (role == null || role.isEmpty()) role = "Freelancer";
                            if (name == null || name.isEmpty()) name = "User " + uid.substring(0, Math.min(5, uid.length()));
                            if (email == null) email = "";

                            User user = new User(uid, name, email, role, skills, bio);
                            allUserList.add(user);
                        }
                        filterAndSortUsers();
                    }
                });
    }

    private void filterAndSortUsers() {
        displayedUserList.clear();
        String query = etSearch.getText().toString().trim().toLowerCase();

        for (User user : allUserList) {
            // Role Filter
            boolean matchesRole = "All".equalsIgnoreCase(selectedRoleFilter) ||
                    (user.getRole() != null && user.getRole().equalsIgnoreCase(selectedRoleFilter));

            // Search Query Filter
            boolean matchesSearch = query.isEmpty() ||
                    (user.getName() != null && user.getName().toLowerCase().contains(query)) ||
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(query)) ||
                    (user.getRole() != null && user.getRole().toLowerCase().contains(query)) ||
                    (user.getDetails() != null && user.getDetails().toLowerCase().contains(query));

            if (matchesRole && matchesSearch) {
                displayedUserList.add(user);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showUserDialog(User existingUser, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_user, null);
        builder.setView(view);

        TextView dialogTitle = view.findViewById(R.id.dialog_title);
        Spinner spinnerRole = view.findViewById(R.id.spinner_user_role);
        EditText etName = view.findViewById(R.id.et_user_name);
        EditText etEmail = view.findViewById(R.id.et_user_email);
        EditText etSkills = view.findViewById(R.id.et_user_skills);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnSave = view.findViewById(R.id.btn_save);

        String[] roles = {"Freelancer", "Employer", "Recruiter", "Admin"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_role, roles);
        roleAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_role);
        spinnerRole.setAdapter(roleAdapter);

        if (existingUser != null) {
            dialogTitle.setText("Edit User Profile");
            etName.setText(existingUser.getName());
            etEmail.setText(existingUser.getEmail());
            etSkills.setText(existingUser.getDetails());

            for (int i = 0; i < roles.length; i++) {
                if (roles[i].equalsIgnoreCase(existingUser.getRole())) {
                    spinnerRole.setSelection(i);
                    break;
                }
            }
            // Disallow changing role of existing users
            spinnerRole.setEnabled(false);
            spinnerRole.setAlpha(0.6f);
        } else {
            dialogTitle.setText("Add New User");
            spinnerRole.setEnabled(true);
            spinnerRole.setAlpha(1.0f);
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String skills = etSkills.getText().toString().trim();
            String role = spinnerRole.getSelectedItem().toString();

            if (name.isEmpty()) {
                etName.setError("Name is required");
                return;
            }
            if (email.isEmpty()) {
                etEmail.setError("Email is required");
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("email", email);
            data.put("role", role);
            if ("Employer".equals(role) || "Recruiter".equals(role)) {
                data.put("bio", skills);
                data.put("skills", "");
            } else {
                data.put("skills", skills);
                data.put("bio", "");
            }

            if (existingUser == null) {
                // CREATE operation in Firestore
                DocumentReference docRef = db.collection("Users").document();
                docRef.set(data)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "User created", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Error creating user: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } else {
                // UPDATE operation in Firestore
                db.collection("Users").document(existingUser.getUid())
                        .update(data)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "User profile updated", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Error updating user: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
            dialog.dismiss();
        });

        dialog.show();
    }


    // --- UserAdapter.OnUserActionListener callbacks ---

    @Override
    public void onEditUser(User user, int position) {
        showUserDialog(user, position);
    }

    @Override
    public void onDeleteUser(User user, int position) {
        // DELETE operation in backend
        if (user != null && user.getUid() != null) {
            db.collection("Users").document(user.getUid())
                    .delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error deleting user: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) {
            userListener.remove();
        }
    }
}


