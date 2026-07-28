package com.nibm.worklink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class RecruiterUserAdapter extends RecyclerView.Adapter<RecruiterUserAdapter.ViewHolder> {

    public static class UserModel {
        String uid;
        String name;
        String email;
        String role;
        String skills;
        String bio;
        boolean isVerified = false;

        public UserModel(String uid, String name, String email, String role, String skills, String bio, boolean isVerified) {
            this.uid = uid;
            this.name = name;
            this.email = email;
            this.role = role;
            this.skills = skills;
            this.bio = bio;
            this.isVerified = isVerified;
        }

        public UserModel(String uid, String name, String email, String role, boolean isVerified) {
            this(uid, name, email, role, "", "", isVerified);
        }

        public UserModel(String name, String role) {
            this("", name, "", role, "", "", false);
        }
    }

    private List<UserModel> usersList;

    public RecruiterUserAdapter(List<UserModel> usersList) {
        this.usersList = usersList;
    }

    public void updateList(List<UserModel> newList) {
        this.usersList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_verify_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserModel user = usersList.get(position);
        holder.tvName.setText(user.name != null ? user.name : "User");
        holder.tvRole.setText(user.role != null ? user.role : "Member");
        holder.tvEmail.setText(user.email != null && !user.email.isEmpty() ? user.email : "No email available");

        StringBuilder details = new StringBuilder();
        if (user.skills != null && !user.skills.trim().isEmpty()) {
            details.append("Skills: ").append(user.skills);
        }
        if (user.bio != null && !user.bio.trim().isEmpty()) {
            if (details.length() > 0) details.append(" | ");
            details.append("Bio: ").append(user.bio);
        }
        if (details.length() == 0) {
            details.append("No additional bio/skills details provided.");
        }
        holder.tvDetails.setText(details.toString());

        if (user.isVerified) {
            holder.btnVerify.setVisibility(View.GONE);
            holder.tvVerified.setVisibility(View.VISIBLE);
        } else {
            holder.btnVerify.setVisibility(View.VISIBLE);
            holder.tvVerified.setVisibility(View.GONE);
        }

        holder.btnViewDetails.setOnClickListener(v -> showUserDetailsDialog(v.getContext(), user, holder));

        holder.btnVerify.setOnClickListener(v -> grantUserVerification(v.getContext(), user, holder));
    }

    private void showUserDetailsDialog(android.content.Context context, UserModel user, ViewHolder holder) {
        StringBuilder msg = new StringBuilder();
        msg.append("Full Name: ").append(user.name).append("\n\n");
        msg.append("Email: ").append(user.email != null && !user.email.isEmpty() ? user.email : "N/A").append("\n\n");
        msg.append("Role: ").append(user.role).append("\n\n");
        msg.append("Skills / Expertise:\n").append(user.skills != null && !user.skills.trim().isEmpty() ? user.skills : "Not specified").append("\n\n");
        msg.append("Bio / Description:\n").append(user.bio != null && !user.bio.trim().isEmpty() ? user.bio : "Not specified").append("\n\n");
        msg.append("Verification Status: ").append(user.isVerified ? "Verified" : "Pending Verification");

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("User Profile Details")
                .setMessage(msg.toString())
                .setNegativeButton("Close", null);

        if (!user.isVerified) {
            builder.setPositiveButton("Grant Verification Mark", (dialog, which) -> {
                grantUserVerification(context, user, holder);
            });
        }

        builder.show();
    }

    private void grantUserVerification(android.content.Context context, UserModel user, ViewHolder holder) {
        user.isVerified = true;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Update DB / Firestore Users collection
        if (user.uid != null && !user.uid.isEmpty()) {
            db.collection("Users").document(user.uid).update("isVerified", true);
        }

        // 2. Create Notification in new Firestore collection "Notifications"
        String recipientId = (user.email != null && !user.email.isEmpty()) ? user.email : (user.uid != null ? user.uid : user.name);
        String notifId = db.collection("Notifications").document().getId();
        long timestamp = System.currentTimeMillis();

        Notification notification = new Notification(
                notifId,
                recipientId,
                "User Account Verified",
                "Congratulations " + user.name + "! Your account profile has been verified by a recruiter.",
                "VERIFY",
                null,
                null,
                timestamp
        );

        db.collection("Notifications").document(notifId).set(notification);
        DataManager.addNotification(notification);

        Toast.makeText(context, "Verification Mark Granted to: " + user.name, Toast.LENGTH_SHORT).show();

        // Remove from unverified users list
        int pos = holder.getAdapterPosition();
        if (pos != RecyclerView.NO_POSITION && pos < usersList.size()) {
            usersList.remove(pos);
            notifyItemRemoved(pos);
            notifyItemRangeChanged(pos, usersList.size());
        }
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRole, tvEmail, tvDetails, tvVerified;
        Button btnViewDetails, btnVerify;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_verify_user_name);
            tvRole = itemView.findViewById(R.id.tv_verify_user_role);
            tvEmail = itemView.findViewById(R.id.tv_verify_user_email);
            tvDetails = itemView.findViewById(R.id.tv_verify_user_details);
            btnViewDetails = itemView.findViewById(R.id.btn_view_user_details);
            btnVerify = itemView.findViewById(R.id.btn_give_verify_mark);
            tvVerified = itemView.findViewById(R.id.tv_verified_mark);
        }
    }
}
