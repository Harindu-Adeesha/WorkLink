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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ApplyJobActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 101;

    private String jobId;
    private Job currentJob;

    private TextView tvUploadedFile;
    private EditText etCoverLetter;
    private Uri selectedFileUri = null;
    private String selectedFileName = "my_resume.pdf"; // default mock filename

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_job);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

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
            intent.setType("*/*");
            String[] mimeTypes = {"application/pdf", "image/*", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(Intent.createChooser(intent, "Select Resume / CV"), PICK_FILE_REQUEST);
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

            btnSubmit.setEnabled(false);
            btnSubmit.setText("Uploading CV & Submitting...");

            if (selectedFileUri != null) {
                // Upload CV to Cloudinary
                CloudinaryUploader.uploadFile(this, selectedFileUri, null, null, new CloudinaryUploader.UploadCallback() {
                    @Override
                    public void onSuccess(String cloudinaryUrl) {
                        submitApplication(coverLetter, cloudinaryUrl, btnSubmit);
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(ApplyJobActivity.this, "Cloudinary Upload Notice: " + error, Toast.LENGTH_LONG).show();
                        submitApplication(coverLetter, selectedFileName, btnSubmit);
                    }
                });
            } else {
                submitApplication(coverLetter, selectedFileName, btnSubmit);
            }
        });
    }

    private void submitApplication(String coverLetter, String cvUrlOrName, Button btnSubmit) {
        String appId = String.valueOf(System.currentTimeMillis());
        Application app = new Application(
                appId,
                currentJob,
                coverLetter,
                cvUrlOrName,
                "Pending"
        );

        // Save in-memory DataManager
        DataManager.addApplication(app);

        // Save to Firestore DB collection "Applications"
        String freelancerUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        Map<String, Object> map = new HashMap<>();
        map.put("id", appId);
        map.put("freelancerUid", freelancerUid);
        map.put("jobId", currentJob != null ? currentJob.getId() : "");
        map.put("jobTitle", currentJob != null ? currentJob.getTitle() : "");
        map.put("company", currentJob != null ? currentJob.getCompany() : "");
        map.put("employerContact", currentJob != null ? currentJob.getEmployerContact() : "");
        map.put("coverLetter", coverLetter);
        map.put("resumeFileName", cvUrlOrName);
        map.put("resumeUrl", cvUrlOrName);
        map.put("status", "Pending");
        map.put("createdAt", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("Applications").document(appId).set(map)
            .addOnCompleteListener(task -> {
                Toast.makeText(this, "Application Submitted to " + (currentJob != null ? currentJob.getCompany() : "Employer") + "!", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(ApplyJobActivity.this, FreelancerDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
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
                selectedFileUri = uri;
                String path = uri.getPath();
                if (path != null) {
                    int cut = path.lastIndexOf('/');
                    if (cut != -1) {
                        selectedFileName = path.substring(cut + 1);
                    } else {
                        selectedFileName = path;
                    }
                    if (selectedFileName.contains(":")) {
                        selectedFileName = selectedFileName.substring(selectedFileName.indexOf(":") + 1);
                    }
                }
                tvUploadedFile.setText(selectedFileName);
                Toast.makeText(this, "CV selected: " + selectedFileName, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
