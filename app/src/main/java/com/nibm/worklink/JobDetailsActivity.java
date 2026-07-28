package com.nibm.worklink;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class JobDetailsActivity extends AppCompatActivity {

    private String jobId;
    private Job currentJob;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        jobId = getIntent().getStringExtra("job_id");
        if (jobId == null) {
            Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentJob = DataManager.getJobById(jobId);
        if (currentJob != null) {
            displayJobDetails();
        } else {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("Jobs").document(jobId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Job job = doc.toObject(Job.class);
                        if (job != null) {
                            currentJob = new Job(
                                doc.getId(), job.getTitle(), job.getCompany(), job.getDescription(),
                                job.getSalary(), job.getCategory(), job.getEmployerDescription(),
                                job.getEmployerRating(), job.getEmployerContact(), job.getDeadline(),
                                job.getStatus(), job.isVerified()
                            );
                            displayJobDetails();
                            return;
                        }
                    }
                    Toast.makeText(this, "Job detail empty", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load job details", Toast.LENGTH_SHORT).show();
                    finish();
                });
        }
    }

    private void displayJobDetails() {
        if (currentJob == null) return;
        TextView tvTitle = findViewById(R.id.tv_detail_title);
        TextView tvCategory = findViewById(R.id.tv_detail_category);
        TextView tvSalary = findViewById(R.id.tv_detail_salary);
        TextView tvDeadline = findViewById(R.id.tv_detail_deadline);
        TextView tvDesc = findViewById(R.id.tv_detail_desc);

        tvTitle.setText(currentJob.getTitle());
        tvCategory.setText(currentJob.getCategory());
        tvSalary.setText(currentJob.getSalary());
        tvDeadline.setText("Apply Before: " + currentJob.getDeadline());
        tvDesc.setText(currentJob.getDescription());

        // Bind Employer Views
        TextView tvEmployerName = findViewById(R.id.tv_employer_name);
        TextView tvEmployerRating = findViewById(R.id.tv_employer_rating);
        TextView tvEmployerContact = findViewById(R.id.tv_employer_contact);
        TextView tvEmployerDesc = findViewById(R.id.tv_employer_desc);

        tvEmployerName.setText(currentJob.getCompany());
        if (currentJob.getEmployerRating() > 0) {
            tvEmployerRating.setText("★ " + String.format("%.1f", currentJob.getEmployerRating()) + " Rating");
        } else {
            tvEmployerRating.setText("★ New Rating");
        }
        tvEmployerContact.setText("Contact: " + currentJob.getEmployerContact());
        tvEmployerDesc.setText(currentJob.getEmployerDescription());

        // Fetch live calculated average rating for this specific job
        RatingRepository.fetchJobRatings(ratingsMap -> {
            if (currentJob != null) {
                Float avgRating = ratingsMap.get(currentJob.getId());
                if (avgRating != null) {
                    tvEmployerRating.setText(String.format("★ %.1f Rating", avgRating));
                }
            }
        });

        Button btnApply = findViewById(R.id.btn_apply);
        btnApply.setOnClickListener(v -> {
            Intent intent = new Intent(JobDetailsActivity.this, ApplyJobActivity.class);
            intent.putExtra("job_id", currentJob.getId());
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
    }
}
