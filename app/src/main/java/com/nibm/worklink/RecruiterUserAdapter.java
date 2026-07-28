package com.nibm.worklink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class RecruiterUserAdapter extends RecyclerView.Adapter<RecruiterUserAdapter.ViewHolder> {

    public static class UserModel {
        String uid;
        String name;
        String email;
        String role;
        boolean isVerified = false;

        public UserModel(String uid, String name, String email, String role, boolean isVerified) {
            this.uid = uid;
            this.name = name;
            this.email = email;
            this.role = role;
            this.isVerified = isVerified;
        }

        public UserModel(String name, String role) {
            this.name = name;
            this.role = role;
        }
    }

    private List<UserModel> usersList;

    public RecruiterUserAdapter(List<UserModel> usersList) {
        this.usersList = usersList;
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
        holder.tvName.setText(user.name);
        holder.tvRole.setText(user.role);

        if (user.isVerified) {
            holder.btnVerify.setVisibility(View.GONE);
            holder.tvVerified.setVisibility(View.VISIBLE);
        } else {
            holder.btnVerify.setVisibility(View.VISIBLE);
            holder.tvVerified.setVisibility(View.GONE);
        }

        holder.btnVerify.setOnClickListener(v -> {
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

            Toast.makeText(v.getContext(), "Verification Mark Granted to: " + user.name, Toast.LENGTH_SHORT).show();

            // Remove from unverified users list
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && pos < usersList.size()) {
                usersList.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, usersList.size());
            }
        });
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRole, tvVerified;
        Button btnVerify;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_verify_user_name);
            tvRole = itemView.findViewById(R.id.tv_verify_user_role);
            btnVerify = itemView.findViewById(R.id.btn_give_verify_mark);
            tvVerified = itemView.findViewById(R.id.tv_verified_mark);
        }
    }
}
