package com.nibm.worklink;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecruiterReviewAdapter extends RecyclerView.Adapter<RecruiterReviewAdapter.ViewHolder> {

    private List<Review> reviewList;

    public RecruiterReviewAdapter(List<Review> reviewList) {
        this.reviewList = reviewList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recruiter_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviewList.get(position);

        // Resolve job title and company from the job ID
        Job job = DataManager.getJobById(review.getJobId());
        if (job != null) {
            holder.tvJobTitle.setText(job.getTitle());
            holder.tvJobCompany.setText(job.getCompany());
        } else {
            holder.tvJobTitle.setText("Job ID: " + review.getJobId());
            holder.tvJobCompany.setText("Unknown Company");
        }

        holder.tvText.setText(review.getReviewText());
        holder.ratingBar.setRating(review.getRating());

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                .setTitle("Delete Review")
                .setMessage("Are you sure you want to permanently delete this review?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    reviewList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, reviewList.size());
                    Toast.makeText(v.getContext(), "Review deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvJobTitle, tvJobCompany, tvText;
        RatingBar ratingBar;
        Button btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvJobTitle = itemView.findViewById(R.id.tv_review_job_title);
            tvJobCompany = itemView.findViewById(R.id.tv_review_job_company);
            tvText = itemView.findViewById(R.id.tv_review_text);
            ratingBar = itemView.findViewById(R.id.rb_review_rating);
            btnDelete = itemView.findViewById(R.id.btn_delete_review);
        }
    }
}
