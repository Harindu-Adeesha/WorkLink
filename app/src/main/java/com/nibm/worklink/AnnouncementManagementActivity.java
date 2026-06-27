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

public class AnnouncementManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AnnouncementAdapter adapter;
    private List<String> announcementList;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcement_management);

        announcementList = new ArrayList<>();
        announcementList.add("System Maintenance at Midnight");
        announcementList.add("New Features Deployed");

        recyclerView = findViewById(R.id.recycler_announcements);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AnnouncementAdapter(announcementList);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add_announcement);
        fab.setOnClickListener(v -> showAnnouncementDialog(null, -1));

        bottomNavigationView = findViewById(R.id.bottom_navigation);
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
    }

    private void showAnnouncementDialog(String existingName, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_category, null);
        builder.setView(view);

        TextView dialogTitle = view.findViewById(R.id.dialog_title);
        EditText etAnnName = view.findViewById(R.id.et_category_name);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnSave = view.findViewById(R.id.btn_save);

        dialogTitle.setText(existingName != null ? "Edit Announcement" : "Add Announcement");
        etAnnName.setHint("Announcement Title");
        if (existingName != null) {
            etAnnName.setText(existingName);
        }

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String name = etAnnName.getText().toString().trim();
            if (name.isEmpty()) {
                etAnnName.setError("Required");
                return;
            }
            if (position == -1) {
                announcementList.add(name);
                adapter.notifyItemInserted(announcementList.size() - 1);
                Toast.makeText(this, "Announcement added", Toast.LENGTH_SHORT).show();
            } else {
                announcementList.set(position, name);
                adapter.notifyItemChanged(position);
                Toast.makeText(this, "Announcement updated", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.ViewHolder> {

        private List<String> announcements;

        public AnnouncementAdapter(List<String> announcements) {
            this.announcements = announcements;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String ann = announcements.get(position);
            holder.tvName.setText(ann);
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_agenda);
            
            holder.btnEdit.setOnClickListener(v -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    showAnnouncementDialog(announcements.get(currentPos), currentPos);
                }
            });
            
            holder.btnDelete.setOnClickListener(v -> {
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    new AlertDialog.Builder(AnnouncementManagementActivity.this)
                            .setTitle("Delete Announcement")
                            .setMessage("Are you sure you want to delete '" + announcements.get(currentPos) + "'?")
                            .setPositiveButton("Delete", (d, w) -> {
                                announcements.remove(currentPos);
                                notifyItemRemoved(currentPos);
                                notifyItemRangeChanged(currentPos, announcements.size());
                                Toast.makeText(AnnouncementManagementActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return announcements.size();
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
