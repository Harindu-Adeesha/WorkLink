package com.nibm.worklink;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class JobActionActivity extends AppCompatActivity {

    public static final String EXTRA_ACTION_TYPE = "action_type";
    public static final String EXTRA_JOB_ID = "job_id";
    public static final String EXTRA_JOB_TITLE = "job_title";
    public static final String EXTRA_EMPLOYER_CONTACT = "employer_contact";
    public static final String EXTRA_COMPANY = "company";

    public static final String ACTION_VERIFY = "VERIFY";
    public static final String ACTION_WARN = "WARN";
    public static final String ACTION_REMOVE = "REMOVE";

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_action);

        db = FirebaseFirestore.getInstance();

        // Set up Toolbar with back button
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar_job_action);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        String actionType = getIntent().getStringExtra(EXTRA_ACTION_TYPE);
        String jobId = getIntent().getStringExtra(EXTRA_JOB_ID);
        String jobTitle = getIntent().getStringExtra(EXTRA_JOB_TITLE);
        String employerContact = getIntent().getStringExtra(EXTRA_EMPLOYER_CONTACT);
        String company = getIntent().getStringExtra(EXTRA_COMPANY);

        String recipientId = (employerContact != null && !employerContact.isEmpty()) ? employerContact : (company != null ? company : "unknown_user");

        TextView tvTitle = findViewById(R.id.tv_action_title);
        TextView tvJobTitle = findViewById(R.id.tv_action_job_title);
        TextView tvMessage = findViewById(R.id.tv_action_message);
        TextInputLayout layoutReason = findViewById(R.id.layout_warn_reason);
        TextInputEditText etReason = findViewById(R.id.et_warn_reason);
        Button btnCancel = findViewById(R.id.btn_action_cancel);
        Button btnConfirm = findViewById(R.id.btn_action_confirm);

        tvJobTitle.setText("Job: " + jobTitle);

        if (ACTION_VERIFY.equals(actionType)) {
            tvTitle.setText("Verify Job");
            tvMessage.setText("Are you sure you want to verify this job post?");
            btnConfirm.setText("Verify");
            btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#035DD6")));
        } else if (ACTION_WARN.equals(actionType)) {
            tvTitle.setText("Warn Employer");
            tvMessage.setText("Please provide a reason for the warning below.");
            layoutReason.setVisibility(View.VISIBLE);
            btnConfirm.setText("Send Warning");
            btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFC107")));
        } else if (ACTION_REMOVE.equals(actionType)) {
            tvTitle.setText("Remove Fake Job");
            tvMessage.setText("Are you sure you want to completely remove this job?");
            btnConfirm.setText("Remove");
            btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#DC3545")));
        }

        btnCancel.setOnClickListener(v -> finish());

        btnConfirm.setOnClickListener(v -> {
            if (ACTION_WARN.equals(actionType)) {
                String reason = etReason.getText().toString().trim();
                if (reason.isEmpty()) {
                    etReason.setError("Reason is required");
                    return;
                }
                executeWarnAction(jobId, jobTitle, recipientId, reason);
            } else if (ACTION_VERIFY.equals(actionType)) {
                executeVerifyAction(jobId, jobTitle, recipientId);
            } else if (ACTION_REMOVE.equals(actionType)) {
                executeRemoveAction(jobId, jobTitle, recipientId);
            }
        });
    }

    private void executeVerifyAction(String jobId, String jobTitle, String recipientId) {
        // 1. Update DB / Firestore Jobs collection & DataManager
        if (jobId != null) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", "Verified");
            updateData.put("isVerified", true);
            db.collection("Jobs").document(jobId).update(updateData);

            Job localJob = DataManager.getJobById(jobId);
            if (localJob != null) {
                localJob.setStatus("Verified");
                localJob.setVerified(true);
            }
        }

        // 2. Create Notification in new Firestore collection "Notifications"
        createNotification(recipientId, "Job Posting Verified",
                "Your job posting '" + jobTitle + "' has been verified by a recruiter.",
                ACTION_VERIFY, jobId, jobTitle);

        Toast.makeText(this, "Job Verified: " + jobTitle, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void executeWarnAction(String jobId, String jobTitle, String recipientId, String reason) {
        // 1. Update DB / Firestore Jobs collection & DataManager
        if (jobId != null) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", "Warned");
            updateData.put("warningReason", reason);
            db.collection("Jobs").document(jobId).update(updateData);

            Job localJob = DataManager.getJobById(jobId);
            if (localJob != null) {
                localJob.setStatus("Warned");
            }
        }

        // 2. Create Notification in new Firestore collection "Notifications"
        createNotification(recipientId, "Warning Issued for Job",
                "Warning for job '" + jobTitle + "': " + reason,
                ACTION_WARN, jobId, jobTitle);

        Toast.makeText(this, "Warning issued for: " + jobTitle, Toast.LENGTH_LONG).show();
        finish();
    }

    private void executeRemoveAction(String jobId, String jobTitle, String recipientId) {
        // 1. Delete from DB / Firestore & DataManager
        if (jobId != null) {
            db.collection("Jobs").document(jobId).delete();
            DataManager.deleteJob(jobId);
        }

        // 2. Create Notification in new Firestore collection "Notifications"
        createNotification(recipientId, "Job Posting Removed",
                "Your job posting '" + jobTitle + "' was removed by a recruiter.",
                ACTION_REMOVE, jobId, jobTitle);

        Toast.makeText(this, "Job Removed: " + jobTitle, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void createNotification(String recipientId, String title, String message, String type, String jobId, String jobTitle) {
        String notifId = db.collection("Notifications").document().getId();
        long timestamp = System.currentTimeMillis();

        Notification notification = new Notification(notifId, recipientId, title, message, type, jobId, jobTitle, timestamp);

        // Save to Firestore "Notifications" collection
        db.collection("Notifications").document(notifId).set(notification);

        // Save to in-memory DataManager
        DataManager.addNotification(notification);
    }
}
