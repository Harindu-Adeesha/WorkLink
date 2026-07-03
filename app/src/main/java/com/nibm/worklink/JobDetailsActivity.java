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
    private DataManager.Job currentJob;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        jobId = getIntent().getStringExtra("job_id");
        if (jobId == null) {
            Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentJob = DataManager.getJobById(jobId);
        if (currentJob == null) {
            Toast.makeText(this, "Job detail empty", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Bind Job Views
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
        tvEmployerRating.setText("★ " + currentJob.getEmployerRating() + " Rating");
        tvEmployerContact.setText("Contact: " + currentJob.getEmployerContact());
        tvEmployerDesc.setText(currentJob.getEmployerDescription());

        Button btnApply = findViewById(R.id.btn_apply);
        btnApply.setOnClickListener(v -> {
            Intent intent = new Intent(JobDetailsActivity.this, ApplyJobActivity.class);
            intent.putExtra("job_id", currentJob.getId());
            startActivity(intent);
        });
    }
}
