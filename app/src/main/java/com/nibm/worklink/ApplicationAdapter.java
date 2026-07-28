package com.nibm.worklink;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ApplicationAdapter extends RecyclerView.Adapter<ApplicationAdapter.ViewHolder> {

    public interface OnApplicationActionListener {
        void onDeleteApplication(String appId);
        void onReviewApplication(String jobId, String jobTitle);
    }

    private List<Application> appsList;
    private java.util.Set<String> reviewedJobIds;
    private final OnApplicationActionListener listener;

    public ApplicationAdapter(List<Application> appsList, OnApplicationActionListener listener) {
        this.appsList = appsList;
        this.listener = listener;
        this.reviewedJobIds = new java.util.HashSet<>();
    }

    public ApplicationAdapter(List<Application> appsList, java.util.Set<String> reviewedJobIds, OnApplicationActionListener listener) {
        this.appsList = appsList;
        this.reviewedJobIds = reviewedJobIds != null ? reviewedJobIds : new java.util.HashSet<>();
        this.listener = listener;
    }

    public void updateApplications(List<Application> newList) {
        this.appsList = newList;
        notifyDataSetChanged();
    }

    public void updateReviewedJobIds(java.util.Set<String> newReviewedJobIds) {
        this.reviewedJobIds = newReviewedJobIds != null ? newReviewedJobIds : new java.util.HashSet<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_application, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Application app = appsList.get(position);
        holder.tvTitle.setText(app.getJob().getTitle());
        holder.tvCompany.setText(app.getJob().getCompany());
        
        String cv = app.getResumeFileName();
        if (cv != null && (cv.startsWith("http://") || cv.startsWith("https://"))) {
            holder.tvResume.setText("Resume: 🔗 View CV (Cloudinary)");
            holder.tvResume.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(cv));
                    v.getContext().startActivity(intent);
                } catch (Exception e) {
                    android.widget.Toast.makeText(v.getContext(), "Unable to open link", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            holder.tvResume.setText("Resume: " + (cv != null ? cv : "None"));
            holder.tvResume.setOnClickListener(null);
        }
        holder.tvStatus.setText(app.getStatus());

        // Set color based on status
        if ("Accepted".equalsIgnoreCase(app.getStatus())) {
            holder.tvStatus.setTextColor(0xFF198754); // green
            holder.tvStatus.setBackgroundColor(0xFFD1E7DD);
        } else if ("Rejected".equalsIgnoreCase(app.getStatus())) {
            holder.tvStatus.setTextColor(0xFFDC3545); // red
            holder.tvStatus.setBackgroundColor(0xFFF8D7DA);
        } else {
            holder.tvStatus.setTextColor(0xFF856404); // yellow/orange
            holder.tvStatus.setBackgroundColor(0xFFFFF3CD);
        }

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Cancel Application")
                    .setMessage("Are you sure you want to cancel your application for " + app.getJob().getTitle() + "?")
                    .setPositiveButton("Cancel Application", (dialog, which) -> {
                        listener.onDeleteApplication(app.getId());
                        Toast.makeText(v.getContext(), "Application cancelled", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Keep Application", null)
                    .show();
        });

        String jobId = app.getJob() != null ? app.getJob().getId() : "";
        if (reviewedJobIds != null && reviewedJobIds.contains(jobId)) {
            holder.btnReview.setText("Reviewed ✓");
            holder.btnReview.setEnabled(false);
            holder.btnReview.setAlpha(0.5f);
            holder.btnReview.setOnClickListener(null);
        } else {
            holder.btnReview.setText("Give Review");
            holder.btnReview.setEnabled(true);
            holder.btnReview.setAlpha(1.0f);
            holder.btnReview.setOnClickListener(v ->
                    listener.onReviewApplication(app.getJob().getId(), app.getJob().getTitle()));
        }
    }

    @Override
    public int getItemCount() {
        return appsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCompany, tvResume, tvStatus;
        Button btnReview, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle   = itemView.findViewById(R.id.tv_app_title);
            tvCompany = itemView.findViewById(R.id.tv_app_company);
            tvResume  = itemView.findViewById(R.id.tv_app_resume);
            tvStatus  = itemView.findViewById(R.id.tv_app_status);
            btnReview = itemView.findViewById(R.id.btn_app_review);
            btnDelete = itemView.findViewById(R.id.btn_app_delete);
        }
    }
}
