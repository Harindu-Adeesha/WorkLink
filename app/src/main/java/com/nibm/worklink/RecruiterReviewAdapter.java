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

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

        // 1. Resolve Job Title and Company
        String jobTitle = review.getJobTitle();
        String company = review.getCompany();

        if (jobTitle != null && !jobTitle.trim().isEmpty()) {
            holder.tvJobTitle.setText(jobTitle);
        } else {
            Job job = DataManager.getJobById(review.getJobId());
            if (job != null && job.getTitle() != null) {
                holder.tvJobTitle.setText(job.getTitle());
            } else {
                holder.tvJobTitle.setText("Loading Job details...");
            }
        }

        if (company != null && !company.trim().isEmpty()) {
            holder.tvJobCompany.setText(company);
        } else {
            Job job = DataManager.getJobById(review.getJobId());
            if (job != null && job.getCompany() != null) {
                holder.tvJobCompany.setText(job.getCompany());
            } else {
                holder.tvJobCompany.setText("WorkLink");
            }
        }

        // Async fetch job if title/company missing
        if ((jobTitle == null || jobTitle.trim().isEmpty() || company == null || company.trim().isEmpty())
                && review.getJobId() != null && !review.getJobId().isEmpty()) {
            FirebaseFirestore.getInstance().collection("Jobs").document(review.getJobId()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String fetchedTitle = doc.getString("title");
                        String fetchedCompany = doc.getString("company");
                        if (fetchedTitle != null) holder.tvJobTitle.setText(fetchedTitle);
                        if (fetchedCompany != null) holder.tvJobCompany.setText(fetchedCompany);
                    } else if (jobTitle == null) {
                        holder.tvJobTitle.setText("Job Review");
                    }
                });
        }

        // 2. Resolve Reviewer Info
        String reviewerUid = review.getReviewerUid();
        if (reviewerUid != null && !reviewerUid.trim().isEmpty()) {
            holder.tvReviewerName.setText("By: Loading...");
            FirebaseFirestore.getInstance().collection("Users").document(reviewerUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String role = doc.getString("role");
                        if (name == null || name.isEmpty()) name = "User";
                        String reviewerStr = "By: " + name + (role != null && !role.isEmpty() ? " (" + role + ")" : "");
                        holder.tvReviewerName.setText(reviewerStr);
                    } else {
                        holder.tvReviewerName.setText("By: Unknown User");
                    }
                })
                .addOnFailureListener(e -> holder.tvReviewerName.setText("By: Anonymous"));
        } else {
            holder.tvReviewerName.setText("By: Anonymous");
        }

        // 3. Date Formatting
        if (review.getCreatedAt() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
            holder.tvReviewDate.setText(sdf.format(new Date(review.getCreatedAt())));
        } else {
            holder.tvReviewDate.setText("");
        }

        holder.tvText.setText(review.getReviewText());
        holder.ratingBar.setRating(review.getRating());

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Review toDelete = reviewList.get(pos);
            new AlertDialog.Builder(v.getContext())
                .setTitle("Delete Review")
                .setMessage("Are you sure you want to permanently delete this review?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Remove from Firestore
                    FirebaseFirestore.getInstance()
                        .collection("Reviews")
                        .document(toDelete.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            reviewList.remove(pos);
                            notifyItemRemoved(pos);
                            notifyItemRangeChanged(pos, reviewList.size());
                            Toast.makeText(v.getContext(), "Review deleted", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                            Toast.makeText(v.getContext(), "Failed to delete review", Toast.LENGTH_SHORT).show()
                        );
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
        TextView tvJobTitle, tvJobCompany, tvReviewerName, tvReviewDate, tvText;
        RatingBar ratingBar;
        Button btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvJobTitle = itemView.findViewById(R.id.tv_review_job_title);
            tvJobCompany = itemView.findViewById(R.id.tv_review_job_company);
            tvReviewerName = itemView.findViewById(R.id.tv_reviewer_name);
            tvReviewDate = itemView.findViewById(R.id.tv_review_date);
            tvText = itemView.findViewById(R.id.tv_review_text);
            ratingBar = itemView.findViewById(R.id.rb_review_rating);
            btnDelete = itemView.findViewById(R.id.btn_delete_review);
        }
    }
}
