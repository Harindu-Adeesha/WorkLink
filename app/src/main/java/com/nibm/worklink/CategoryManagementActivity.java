package com.nibm.worklink;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class CategoryManagementActivity extends AppCompatActivity
        implements CategoryAdapter.OnCategoryActionListener {

    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private List<Category> categoryList;
    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private ListenerRegistration categoryListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_management);

        db = FirebaseFirestore.getInstance();
        categoryList = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_categories);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryAdapter(categoryList, this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add_category);
        fab.setOnClickListener(v -> showCategoryDialog(null, -1));

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_categories);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_categories) return true;

            Intent intent = null;
            if (id == R.id.nav_dashboard) intent = new Intent(this, AdminDashboardActivity.class);
            else if (id == R.id.nav_users) intent = new Intent(this, UserManagementActivity.class);
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

        listenToCategoryUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_categories);
        }
    }

    private void listenToCategoryUpdates() {
        categoryListener = db.collection("Categories")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading categories: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (snapshots != null) {
                        categoryList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String docId = doc.getId();
                            String name = doc.getString("name");
                            if (name == null || name.trim().isEmpty()) {
                                Category cat = doc.toObject(Category.class);
                                if (cat != null && cat.getName() != null) {
                                    name = cat.getName();
                                }
                            }
                            if (name != null && !name.trim().isEmpty()) {
                                categoryList.add(new Category(docId, name));
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void showCategoryDialog(Category existingCategory, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_category, null);
        builder.setView(view);

        TextView dialogTitle = view.findViewById(R.id.dialog_title);
        EditText etCategoryName = view.findViewById(R.id.et_category_name);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnSave = view.findViewById(R.id.btn_save);

        if (existingCategory != null) {
            dialogTitle.setText("Edit Category");
            etCategoryName.setText(existingCategory.getName());
        } else {
            dialogTitle.setText("Add Category");
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String name = etCategoryName.getText().toString().trim();
            if (name.isEmpty()) {
                etCategoryName.setError("Required");
                return;
            }
            if (existingCategory == null) {
                // CREATE operation in backend
                DocumentReference docRef = db.collection("Categories").document();
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("id", docRef.getId());
                data.put("name", name);
                docRef.set(data)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } else {
                // UPDATE operation in backend
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("name", name);
                db.collection("Categories").document(existingCategory.getId())
                        .update(data)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Error updating: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
            dialog.dismiss();
        });

        dialog.show();
    }


    // --- CategoryAdapter.OnCategoryActionListener callbacks ---

    @Override
    public void onEditCategory(Category category, int position) {
        showCategoryDialog(category, position);
    }

    @Override
    public void onDeleteCategory(Category category, int position) {
        // DELETE operation in backend
        if (category != null && category.getId() != null) {
            db.collection("Categories").document(category.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error deleting category: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (categoryListener != null) {
            categoryListener.remove();
        }
    }
}


