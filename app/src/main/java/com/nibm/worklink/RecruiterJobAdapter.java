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

public class RecruiterJobAdapter extends RecyclerView.Adapter<RecruiterJobAdapter.ViewHolder> {

    private List<Job> jobsList;

    public RecruiterJobAdapter(List<Job> jobsList) {
        this.jobsList = jobsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_verify_job, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Job job = jobsList.get(position);
        holder.tvTitle.setText(job.getTitle());
        holder.tvCompany.setText(job.getCompany());

        holder.btnVerify.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(v.getContext(), JobActionActivity.class);
            intent.putExtra(JobActionActivity.EXTRA_ACTION_TYPE, JobActionActivity.ACTION_VERIFY);
            intent.putExtra(JobActionActivity.EXTRA_JOB_ID, job.getId());
            intent.putExtra(JobActionActivity.EXTRA_JOB_TITLE, job.getTitle());
            intent.putExtra(JobActionActivity.EXTRA_EMPLOYER_CONTACT, job.getEmployerContact());
            intent.putExtra(JobActionActivity.EXTRA_COMPANY, job.getCompany());
            v.getContext().startActivity(intent);
        });

        holder.btnWarn.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(v.getContext(), JobActionActivity.class);
            intent.putExtra(JobActionActivity.EXTRA_ACTION_TYPE, JobActionActivity.ACTION_WARN);
            intent.putExtra(JobActionActivity.EXTRA_JOB_ID, job.getId());
            intent.putExtra(JobActionActivity.EXTRA_JOB_TITLE, job.getTitle());
            intent.putExtra(JobActionActivity.EXTRA_EMPLOYER_CONTACT, job.getEmployerContact());
            intent.putExtra(JobActionActivity.EXTRA_COMPANY, job.getCompany());
            v.getContext().startActivity(intent);
        });

        holder.btnRemove.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(v.getContext(), JobActionActivity.class);
            intent.putExtra(JobActionActivity.EXTRA_ACTION_TYPE, JobActionActivity.ACTION_REMOVE);
            intent.putExtra(JobActionActivity.EXTRA_JOB_ID, job.getId());
            intent.putExtra(JobActionActivity.EXTRA_JOB_TITLE, job.getTitle());
            intent.putExtra(JobActionActivity.EXTRA_EMPLOYER_CONTACT, job.getEmployerContact());
            intent.putExtra(JobActionActivity.EXTRA_COMPANY, job.getCompany());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return jobsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCompany;
        Button btnVerify, btnWarn, btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_verify_job_title);
            tvCompany = itemView.findViewById(R.id.tv_verify_job_company);
            btnVerify = itemView.findViewById(R.id.btn_verify_job);
            btnWarn = itemView.findViewById(R.id.btn_warn_job);
            btnRemove = itemView.findViewById(R.id.btn_remove_fake_job);
        }
    }
}
