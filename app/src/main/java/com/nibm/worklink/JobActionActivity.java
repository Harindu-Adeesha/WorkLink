package com.nibm.worklink;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class JobActionActivity extends AppCompatActivity {

    public static final String EXTRA_ACTION_TYPE = "action_type";
    public static final String EXTRA_JOB_ID = "job_id";
    public static final String EXTRA_JOB_TITLE = "job_title";

    public static final String ACTION_VERIFY = "VERIFY";
    public static final String ACTION_WARN = "WARN";
    public static final String ACTION_REMOVE = "REMOVE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_action);

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
                String reason = etReason.getText().toString();
                if (reason.isEmpty()) {
                    etReason.setError("Required");
                    return;
                }
                Toast.makeText(this, "Warning issued for: " + jobTitle + "\nReason: " + reason, Toast.LENGTH_LONG).show();
            } else if (ACTION_VERIFY.equals(actionType)) {
                Toast.makeText(this, "Job Verified: " + jobTitle, Toast.LENGTH_SHORT).show();
            } else if (ACTION_REMOVE.equals(actionType)) {
                Toast.makeText(this, "Job Removed: " + jobTitle, Toast.LENGTH_SHORT).show();
                // In a real app we'd trigger a broadcast or callback to remove it from the list
            }
            finish();
        });
    }
}
