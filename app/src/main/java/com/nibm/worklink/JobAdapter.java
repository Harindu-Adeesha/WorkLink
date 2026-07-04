package com.nibm.worklink;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class JobAdapter extends RecyclerView.Adapter<JobAdapter.ViewHolder> {

    private List<Job> jobsList;

    public JobAdapter(List<Job> jobsList) {
        this.jobsList = jobsList;
    }

    public void updateJobs(List<Job> newList) {
        this.jobsList = newList;
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
        holder.tvRating.setText("★ " + job.getEmployerRating());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), JobDetailsActivity.class);
            intent.putExtra("job_id", job.getId());
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
