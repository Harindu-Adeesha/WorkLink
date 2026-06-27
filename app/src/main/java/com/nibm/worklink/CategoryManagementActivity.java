package com.nibm.worklink;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class CategoryManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private List<String> categoryList;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_management);

        categoryList = new ArrayList<>();
        categoryList.add("Electronics");
        categoryList.add("Home Decor");
        categoryList.add("Apparel");
        categoryList.add("Sports");
        categoryList.add("Toys");

        recyclerView = findViewById(R.id.recycler_categories);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryAdapter(categoryList);
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
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });

        android.widget.ImageView ivProfile = findViewById(R.id.iv_profile);
        if (ivProfile != null) {
            ivProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        }
    }

    private void showCategoryDialog(String existingName, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_category, null);
        builder.setView(view);

        TextView dialogTitle = view.findViewById(R.id.dialog_title);
        EditText etCategoryName = view.findViewById(R.id.et_category_name);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnSave = view.findViewById(R.id.btn_save);

        if (existingName != null) {
            dialogTitle.setText("Edit Category");
            etCategoryName.setText(existingName);
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
            if (position == -1) {
                categoryList.add(name);
                adapter.notifyItemInserted(categoryList.size() - 1);
                Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show();
            } else {
                categoryList.set(position, name);
                adapter.notifyItemChanged(position);
                Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

        private List<String> categories;

        public CategoryAdapter(List<String> categories) {
            this.categories = categories;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String category = categories.get(position);
            holder.tvName.setText(category);
            
            holder.btnEdit.setOnClickListener(v -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    showCategoryDialog(categories.get(currentPos), currentPos);
                }
            });
            
            holder.btnDelete.setOnClickListener(v -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    new AlertDialog.Builder(CategoryManagementActivity.this)
                            .setTitle("Delete Category")
                            .setMessage("Are you sure you want to delete '" + categories.get(currentPos) + "'?")
                            .setPositiveButton("Delete", (d, w) -> {
                                categories.remove(currentPos);
                                notifyItemRemoved(currentPos);
                                notifyItemRangeChanged(currentPos, categories.size());
                                Toast.makeText(CategoryManagementActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            ImageButton btnEdit;
            ImageButton btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_category_name);
                btnEdit = itemView.findViewById(R.id.btn_edit);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }
}
