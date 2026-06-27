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
import android.widget.ImageView;
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

public class UserManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private List<String> userList;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        userList = new ArrayList<>();
        userList.add("Alice Johnson");
        userList.add("Bob Smith");
        userList.add("Charlie Brown");

        recyclerView = findViewById(R.id.recycler_users);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(userList);
        recyclerView.setAdapter(adapter);

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
    }

    private void showUserDialog(String existingName, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_category, null);
        builder.setView(view);

        TextView dialogTitle = view.findViewById(R.id.dialog_title);
        EditText etUserName = view.findViewById(R.id.et_category_name);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnSave = view.findViewById(R.id.btn_save);

        dialogTitle.setText(existingName != null ? "Edit User" : "Add User");
        etUserName.setHint("User Name");
        if (existingName != null) {
            etUserName.setText(existingName);
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String name = etUserName.getText().toString().trim();
            if (name.isEmpty()) {
                etUserName.setError("Required");
                return;
            }
            if (position == -1) {
                userList.add(name);
                adapter.notifyItemInserted(userList.size() - 1);
                Toast.makeText(this, "User added", Toast.LENGTH_SHORT).show();
            } else {
                userList.set(position, name);
                adapter.notifyItemChanged(position);
                Toast.makeText(this, "User updated", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

        private List<String> users;

        public UserAdapter(List<String> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String user = users.get(position);
            holder.tvName.setText(user);
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
            
            holder.btnEdit.setOnClickListener(v -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    showUserDialog(users.get(currentPos), currentPos);
                }
            });
            
            holder.btnDelete.setOnClickListener(v -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    new AlertDialog.Builder(UserManagementActivity.this)
                            .setTitle("Delete User")
                            .setMessage("Are you sure you want to delete '" + users.get(currentPos) + "'?")
                            .setPositiveButton("Delete", (d, w) -> {
                                users.remove(currentPos);
                                notifyItemRemoved(currentPos);
                                notifyItemRangeChanged(currentPos, users.size());
                                Toast.makeText(UserManagementActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            ImageView ivIcon;
            ImageButton btnEdit;
            ImageButton btnDelete;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_category_name);
                ivIcon = itemView.findViewById(R.id.iv_category_icon);
                btnEdit = itemView.findViewById(R.id.btn_edit);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }
}
