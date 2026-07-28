package com.nibm.worklink;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JobAdapter extends RecyclerView.Adapter<JobAdapter.ViewHolder> {

    private List<Job> jobsList;
    private Map<String, Float> ratingsMap;

    public JobAdapter(List<Job> jobsList) {
        this.jobsList   = jobsList;
        this.ratingsMap = Collections.emptyMap();
    }

    public JobAdapter(List<Job> jobsList, Map<String, Float> ratingsMap) {
        this.jobsList   = jobsList;
        this.ratingsMap = ratingsMap != null ? ratingsMap : Collections.emptyMap();
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_job, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Job job = jobsList.get(position);
        holder.tvTitle.setText(job.getTitle());
        holder.tvCompany.setText(job.getCompany());
        holder.tvCategory.setText(job.getCategory());
        holder.tvSalary.setText(job.getSalary());

        // Show average rating from reviews; fall back to employerRating if no reviews yet
        Float avgRating = ratingsMap.get(job.getId());
        if (avgRating != null) {
            holder.tvRating.setText(String.format("★ %.1f", avgRating));
            holder.tvRating.setVisibility(View.VISIBLE);
        } else if (job.getEmployerRating() > 0) {
            holder.tvRating.setText(String.format("★ %.1f", job.getEmployerRating()));
            holder.tvRating.setVisibility(View.VISIBLE);
        } else {
            holder.tvRating.setText("★ New");
            holder.tvRating.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), JobDetailsActivity.class);
            intent.putExtra("job_id", job.getId());
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return jobsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCompany, tvCategory, tvSalary, tvRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle    = itemView.findViewById(R.id.tv_job_title);
            tvCompany  = itemView.findViewById(R.id.tv_job_company);
            tvCategory = itemView.findViewById(R.id.tv_job_category);
            tvSalary   = itemView.findViewById(R.id.tv_job_salary);
            tvRating   = itemView.findViewById(R.id.tv_job_rating);
        }
    }
}
