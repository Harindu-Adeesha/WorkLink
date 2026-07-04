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

import java.util.List;

public class EmployerApplicationAdapter extends RecyclerView.Adapter<EmployerApplicationAdapter.ViewHolder> {

    public interface OnApplicationStatusListener {
        void onGiveStatus(Application app);
        void onDismissApplication(String appId);
    }

    private List<Application> appsList;
    private final OnApplicationStatusListener listener;

    public EmployerApplicationAdapter(List<Application> appsList, OnApplicationStatusListener listener) {
        this.appsList = appsList;
        this.listener = listener;
    }

    public void updateApplications(List<Application> newList) {
        this.appsList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employer_application, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Application app = appsList.get(position);
        holder.tvTitle.setText(app.getJob().getTitle());
        holder.tvResume.setText("Resume: " + app.getResumeFileName());
        holder.tvCover.setText(app.getCoverLetter());
        holder.tvStatus.setText(app.getStatus());

        // Set dynamic applicant info
        Profile fp = DataManager.getProfile();
        if (fp != null) {
            holder.tvApplicant.setText("Applicant: " + fp.getName() + " (" + fp.getEmail() + ")");
        } else {
            holder.tvApplicant.setText("Applicant: Mock Freelancer (freelancer@worklink.com)");
        }

        // Set status tag styling
        if ("Accepted".equalsIgnoreCase(app.getStatus())) {
            holder.tvStatus.setTextColor(0xFF198754);
            holder.tvStatus.setBackgroundColor(0xFFD1E7DD);
        } else if ("Rejected".equalsIgnoreCase(app.getStatus())) {
            holder.tvStatus.setTextColor(0xFFDC3545);
            holder.tvStatus.setBackgroundColor(0xFFF8D7DA);
        } else {
            holder.tvStatus.setTextColor(0xFF856404);
            holder.tvStatus.setBackgroundColor(0xFFFFF3CD);
        }

        holder.btnGiveStatus.setOnClickListener(v -> listener.onGiveStatus(app));

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Dismiss Application")
                    .setMessage("Are you sure you want to dismiss this application from the feed?")
                    .setPositiveButton("Dismiss", (dialog, which) -> {
                        listener.onDismissApplication(app.getId());
                        Toast.makeText(v.getContext(), "Application dismissed", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return appsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvApplicant, tvResume, tvCover, tvStatus;
        Button btnGiveStatus, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle       = itemView.findViewById(R.id.tv_employer_app_title);
            tvApplicant   = itemView.findViewById(R.id.tv_employer_app_applicant);
            tvResume      = itemView.findViewById(R.id.tv_employer_app_resume);
            tvCover       = itemView.findViewById(R.id.tv_employer_app_cover);
            tvStatus      = itemView.findViewById(R.id.tv_employer_app_status);
            btnGiveStatus = itemView.findViewById(R.id.btn_employer_app_give_status);
            btnDelete     = itemView.findViewById(R.id.btn_employer_app_delete);
        }
    }
}
