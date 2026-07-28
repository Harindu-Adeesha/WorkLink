package com.nibm.worklink;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    public interface OnUserActionListener {
        void onEditUser(User user, int position);
        void onDeleteUser(User user, int position);
    }

    private final List<User> users;
    private final OnUserActionListener listener;

    public UserAdapter(List<User> users, OnUserActionListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.tvName.setText(user.getName() != null ? user.getName() : "No Name");
        holder.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        
        String role = user.getRole() != null ? user.getRole() : "Freelancer";
        holder.tvRole.setText(role);
        
        // Color coding badges by role
        if ("Admin".equalsIgnoreCase(role)) {
            holder.tvRole.setTextColor(Color.parseColor("#DC3545"));
            holder.tvRole.setBackgroundColor(Color.parseColor("#F8D7DA"));
        } else if ("Employer".equalsIgnoreCase(role)) {
            holder.tvRole.setTextColor(Color.parseColor("#198754"));
            holder.tvRole.setBackgroundColor(Color.parseColor("#D1E7DD"));
        } else if ("Recruiter".equalsIgnoreCase(role)) {
            holder.tvRole.setTextColor(Color.parseColor("#6F42C1"));
            holder.tvRole.setBackgroundColor(Color.parseColor("#E2D9F3"));
        } else {
            // Freelancer / Default
            holder.tvRole.setTextColor(Color.parseColor("#035DD6"));
            holder.tvRole.setBackgroundColor(Color.parseColor("#E7F1FF"));
        }

        holder.tvDetails.setText(user.getDetails());

        holder.btnEdit.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                listener.onEditUser(users.get(currentPos), currentPos);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                User userToDelete = users.get(currentPos);
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Delete User")
                        .setMessage("Are you sure you want to delete '" + userToDelete.getName() + "'?")
                        .setPositiveButton("Delete", (d, w) -> {
                            listener.onDeleteUser(userToDelete, currentPos);
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole, tvDetails;
        ImageView ivAvatar;
        ImageButton btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvName    = itemView.findViewById(R.id.tv_user_name);
            tvEmail   = itemView.findViewById(R.id.tv_user_email);
            tvRole    = itemView.findViewById(R.id.tv_user_role);
            tvDetails = itemView.findViewById(R.id.tv_user_details);
            ivAvatar  = itemView.findViewById(R.id.iv_user_avatar);
            btnEdit   = itemView.findViewById(R.id.btn_edit_user);
            btnDelete = itemView.findViewById(R.id.btn_delete_user);
        }
    }
}

