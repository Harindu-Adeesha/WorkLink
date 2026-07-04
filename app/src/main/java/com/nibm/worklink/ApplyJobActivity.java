package com.nibm.worklink;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ApplyJobActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 101;

    private String jobId;
    private Job currentJob;

    private TextView tvUploadedFile;
    private EditText etCoverLetter;
    private String selectedFileName = "my_resume.pdf"; // default mock filename

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_job);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        jobId = getIntent().getStringExtra("job_id");
        if (jobId == null) {
            Toast.makeText(this, "Job not specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentJob = DataManager.getJobById(jobId);
        if (currentJob == null) {
            Toast.makeText(this, "Job not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView tvJobTitle = findViewById(R.id.tv_apply_job_title);
        TextView tvCompany = findViewById(R.id.tv_apply_company);
        tvUploadedFile = findViewById(R.id.tv_uploaded_file);
        etCoverLetter = findViewById(R.id.et_cover_letter);

        tvJobTitle.setText(currentJob.getTitle());
        tvCompany.setText(currentJob.getCompany());
        tvUploadedFile.setText(selectedFileName); // default

        Button btnUpload = findViewById(R.id.btn_upload);
        btnUpload.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(Intent.createChooser(intent, "Select Resume (PDF)"), PICK_FILE_REQUEST);
            } catch (Exception e) {
                Toast.makeText(this, "No file browser found. Using default resume.", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnSubmit = findViewById(R.id.btn_submit_application);
        btnSubmit.setOnClickListener(v -> {
            String coverLetter = etCoverLetter.getText().toString().trim();
            if (coverLetter.isEmpty()) {
                etCoverLetter.setError("Required");
                return;
            }

            // Create application object
            String appId = String.valueOf(DataManager.getApplications().size() + 1);
            Application app = new Application(
                    appId,
                    currentJob,
                    coverLetter,
                    selectedFileName,
                    "Pending"
            );

            DataManager.addApplication(app);

            Toast.makeText(this, "Application Submitted to " + currentJob.getCompany() + "!", Toast.LENGTH_LONG).show();

            // Redirect back to Freelancer Dashboard (clear and launch)
            Intent intent = new Intent(ApplyJobActivity.this, FreelancerDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String path = uri.getPath();
                if (path != null) {
                    int cut = path.lastIndexOf('/');
                    if (cut != -1) {
                        selectedFileName = path.substring(cut + 1);
                    } else {
                        selectedFileName = path;
                    }
                    // Simplify mock files naming
                    if (selectedFileName.contains(":")) {
                        selectedFileName = selectedFileName.substring(selectedFileName.indexOf(":") + 1);
                    }
                    if (!selectedFileName.toLowerCase().endsWith(".pdf")) {
                        selectedFileName += ".pdf";
                    }
                }
                tvUploadedFile.setText(selectedFileName);
                Toast.makeText(this, "File loaded: " + selectedFileName, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
