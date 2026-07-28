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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class JobActionActivity extends AppCompatActivity {

    public static final String EXTRA_ACTION_TYPE      = "action_type";
    public static final String EXTRA_JOB_ID           = "job_id";
    public static final String EXTRA_JOB_TITLE        = "job_title";
    public static final String EXTRA_EMPLOYER_CONTACT = "employer_contact"; // email used for display / fallback
    public static final String EXTRA_EMPLOYER_UID     = "employer_uid";     // direct UID (preferred)
    public static final String EXTRA_COMPANY          = "company";

    public static final String ACTION_VERIFY = "VERIFY";
    public static final String ACTION_WARN   = "WARN";
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

        String actionType      = getIntent().getStringExtra(EXTRA_ACTION_TYPE);
        String jobId           = getIntent().getStringExtra(EXTRA_JOB_ID);
        String jobTitle        = getIntent().getStringExtra(EXTRA_JOB_TITLE);
        String employerContact = getIntent().getStringExtra(EXTRA_EMPLOYER_CONTACT); // email
        String employerUid     = getIntent().getStringExtra(EXTRA_EMPLOYER_UID);     // uid (may be null for old callers)
        String company         = getIntent().getStringExtra(EXTRA_COMPANY);

        TextView tvTitle    = findViewById(R.id.tv_action_title);
        TextView tvJobTitle = findViewById(R.id.tv_action_job_title);
        TextView tvMessage  = findViewById(R.id.tv_action_message);
        TextInputLayout    layoutReason = findViewById(R.id.layout_warn_reason);
        TextInputEditText  etReason     = findViewById(R.id.et_warn_reason);
        Button btnCancel  = findViewById(R.id.btn_action_cancel);
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
                resolveEmployerUidThenAct(employerUid, employerContact, uid ->
                        executeWarnAction(jobId, jobTitle, uid, reason));
            } else if (ACTION_VERIFY.equals(actionType)) {
                resolveEmployerUidThenAct(employerUid, employerContact, uid ->
                        executeVerifyAction(jobId, jobTitle, uid));
            } else if (ACTION_REMOVE.equals(actionType)) {
                resolveEmployerUidThenAct(employerUid, employerContact, uid ->
                        executeRemoveAction(jobId, jobTitle, uid));
            }
        });
    }

    /** If we already have the UID (new callers pass EXTRA_EMPLOYER_UID), use it directly.
     *  Otherwise look it up from Firestore by email, then call the action. */
    private void resolveEmployerUidThenAct(String knownUid, String email, UidCallback callback) {
        if (knownUid != null && !knownUid.isEmpty()) {
            callback.onResolved(knownUid);
            return;
        }
        if (email == null || email.isEmpty()) {
            callback.onResolved("unknown_employer");
            return;
        }
        db.collection("Users")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener(snap -> {
                String uid = "unknown_employer";
                for (QueryDocumentSnapshot doc : snap) {
                    uid = doc.getId();
                    break;
                }
                callback.onResolved(uid);
            })
            .addOnFailureListener(e -> callback.onResolved(email)); // fallback to email if lookup fails
    }

    interface UidCallback {
        void onResolved(String uid);
    }

    private void executeVerifyAction(String jobId, String jobTitle, String recipientUid) {
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

        createNotification(recipientUid, "Job Posting Verified ✅",
                "Your job posting '" + jobTitle + "' has been verified by a recruiter and is now live for freelancers.",
                ACTION_VERIFY, jobId, jobTitle);

        Toast.makeText(this, "Job Verified: " + jobTitle, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void executeWarnAction(String jobId, String jobTitle, String recipientUid, String reason) {
        if (jobId != null) {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", "Warned");
            updateData.put("warningReason", reason);
            db.collection("Jobs").document(jobId).update(updateData);

            Job localJob = DataManager.getJobById(jobId);
            if (localJob != null) localJob.setStatus("Warned");
        }

        createNotification(recipientUid, "⚠️ Warning Issued for Job",
                "Warning for your job posting '" + jobTitle + "': " + reason,
                ACTION_WARN, jobId, jobTitle);

        Toast.makeText(this, "Warning issued for: " + jobTitle, Toast.LENGTH_LONG).show();
        finish();
    }

    private void executeRemoveAction(String jobId, String jobTitle, String recipientUid) {
        if (jobId != null) {
            db.collection("Jobs").document(jobId).delete();
            DataManager.deleteJob(jobId);
        }

        createNotification(recipientUid, "❌ Job Posting Removed",
                "Your job posting '" + jobTitle + "' was removed by a recruiter for violating platform guidelines.",
                ACTION_REMOVE, jobId, jobTitle);

        Toast.makeText(this, "Job Removed: " + jobTitle, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void createNotification(String recipientUid, String title, String message,
                                    String type, String jobId, String jobTitle) {
        String notifId = db.collection("Notifications").document().getId();
        long timestamp = System.currentTimeMillis();

        Notification notification = new Notification(
                notifId, recipientUid, title, message, type, jobId, jobTitle, timestamp);

        db.collection("Notifications").document(notifId).set(notification);
        DataManager.addNotification(notification);
    }
}
