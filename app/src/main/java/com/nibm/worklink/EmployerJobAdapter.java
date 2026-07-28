package com.nibm.worklink;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EmployerJobAdapter extends RecyclerView.Adapter<EmployerJobAdapter.ViewHolder> {

    public interface OnJobActionListener {
        void onEditJob(Job job);
        void onDeleteJob(String jobId);
    }

    private List<Job> jobsList;
    private Map<String, Float> ratingsMap;
    private final OnJobActionListener listener;

    public EmployerJobAdapter(List<Job> jobsList, OnJobActionListener listener) {
        this.jobsList   = jobsList;
        this.listener   = listener;
        this.ratingsMap = Collections.emptyMap();
    }

    public EmployerJobAdapter(List<Job> jobsList, Map<String, Float> ratingsMap, OnJobActionListener listener) {
        this.jobsList   = jobsList;
        this.ratingsMap = ratingsMap != null ? ratingsMap : Collections.emptyMap();
        this.listener   = listener;
    }

    public void updateJobs(List<Job> newList) {
        this.jobsList = newList;
        notifyDataSetChanged();
    }

    public void updateRatings(Map<String, Float> newRatings) {
        this.ratingsMap = newRatings != null ? newRatings : Collections.emptyMap();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employer_job, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Job job = jobsList.get(position);
        holder.tvTitle.setText(job.getTitle());
        holder.tvCompany.setText(job.getCompany());
        holder.tvCategory.setText(job.getCategory());
        holder.tvSalary.setText(job.getSalary());
        holder.tvDeadline.setText("Deadline: " + job.getDeadline());

        // Show average rating from reviews; fall back to employerRating if no reviews yet
        Float avgRating = ratingsMap.get(job.getId());
        if (avgRating != null) {
            holder.tvRating.setText(String.format("★ %.1f", avgRating));
        } else if (job.getEmployerRating() > 0) {
            holder.tvRating.setText(String.format("★ %.1f", job.getEmployerRating()));
        } else {
            holder.tvRating.setText("★ New");
        }

        holder.btnEdit.setOnClickListener(v -> listener.onEditJob(job));

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Job Listing")
                    .setMessage("Are you sure you want to delete the job post for \"" + job.getTitle() + "\"? All related applications will be deleted too.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        listener.onDeleteJob(job.getId());
                        Toast.makeText(v.getContext(), "Job listing deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return jobsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCompany, tvCategory, tvSalary, tvDeadline, tvRating;
        Button btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle    = itemView.findViewById(R.id.tv_employer_job_title);
            tvCompany  = itemView.findViewById(R.id.tv_employer_job_company);
            tvCategory = itemView.findViewById(R.id.tv_employer_job_category);
            tvSalary   = itemView.findViewById(R.id.tv_employer_job_salary);
            tvDeadline = itemView.findViewById(R.id.tv_employer_job_deadline);
            tvRating   = itemView.findViewById(R.id.tv_employer_job_rating);
            btnEdit    = itemView.findViewById(R.id.btn_employer_job_edit);
            btnDelete  = itemView.findViewById(R.id.btn_employer_job_delete);
        }
    }
}
