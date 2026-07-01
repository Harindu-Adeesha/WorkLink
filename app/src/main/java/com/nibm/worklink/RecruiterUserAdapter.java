package com.nibm.worklink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecruiterUserAdapter extends RecyclerView.Adapter<RecruiterUserAdapter.ViewHolder> {

    public static class UserModel {
        String name;
        String role;
        boolean isVerified = false;
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
            notifyItemChanged(position);
            Toast.makeText(v.getContext(), "Verification Mark Granted to: " + user.name, Toast.LENGTH_SHORT).show();
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
