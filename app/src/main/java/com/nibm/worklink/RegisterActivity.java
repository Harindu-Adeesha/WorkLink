package com.nibm.worklink;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        EditText etName     = findViewById(R.id.et_register_name);
        EditText etEmail    = findViewById(R.id.et_register_email);
        EditText etPassword = findViewById(R.id.et_register_password);
        EditText etSkills   = findViewById(R.id.et_register_skills);
        Button btnRegister  = findViewById(R.id.btn_register);
        TextView tvBackToLogin = findViewById(R.id.tv_back_to_login);

        RadioGroup rgRole = findViewById(R.id.rg_role);
        TextView tvSkillsLabel = findViewById(R.id.tv_register_skills_label);

        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_employer) {
                tvSkillsLabel.setText("Company Description / Bio");
                etSkills.setHint("e.g. Leading software engineering company...");
            } else if (checkedId == R.id.rb_recruiter) {
                tvSkillsLabel.setText("Recruitment Agency / Bio");
                etSkills.setHint("e.g. Tech Talent Acquisition...");
            } else {
                tvSkillsLabel.setText("Skills / Area of Expertise");
                etSkills.setHint("Kotlin, Android, Figma, Java...");
            }
        });

        btnRegister.setOnClickListener(v -> {
            String name     = etName.getText().toString().trim();
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String skills   = etSkills.getText().toString().trim();

            // --- All validation BEFORE any Firebase call ---
            boolean hasError = false;

            if (name.isEmpty()) {
                etName.setError("Full name is required");
                hasError = true;
            } else {
                etName.setError(null);
            }

            if (email.isEmpty()) {
                etEmail.setError("Email is required");
                hasError = true;
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email address");
                hasError = true;
            } else {
                etEmail.setError(null);
            }

            if (password.isEmpty()) {
                etPassword.setError("Password is required");
                hasError = true;
            } else if (password.length() < 6) {
                etPassword.setError("Password must be at least 6 characters");
                hasError = true;
            } else {
                etPassword.setError(null);
            }

            // Stop here — Firebase is NEVER called if any field is invalid
            if (hasError) return;

            int checkedId = rgRole.getCheckedRadioButtonId();
            String role = "Freelancer";
            if (checkedId == R.id.rb_employer)       role = "Employer";
            else if (checkedId == R.id.rb_recruiter) role = "Recruiter";

            final String finalRole   = role;
            final String finalName   = name;
            final String finalEmail  = email;
            final String finalSkills = skills;

            btnRegister.setEnabled(false);
            btnRegister.setText("Registering...");

            // Step 1: Create Firebase Auth account
            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, authTask -> {
                    if (!authTask.isSuccessful()) {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Register Account ➔");
                        String errMsg = authTask.getException() != null
                                ? authTask.getException().getMessage()
                                : "Registration failed";
                        Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Step 2: Auth succeeded — navigate to dashboard immediately
                    String uid = mAuth.getCurrentUser().getUid();
                    Log.d(TAG, "Auth created UID=" + uid + ", routing to dashboard.");
                    Toast.makeText(this, "Welcome, " + finalName + "!", Toast.LENGTH_SHORT).show();
                    routeToDashboard(finalRole);

                    // Step 3: Save profile to Firestore in background
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("name", finalName);
                    userMap.put("email", finalEmail);
                    userMap.put("role", finalRole);
                    if ("Employer".equals(finalRole) || "Recruiter".equals(finalRole)) {
                        userMap.put("bio", finalSkills);
                    } else {
                        userMap.put("skills", finalSkills);
                    }

                    db.collection("Users").document(uid).set(userMap)
                        .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "User profile saved to Firestore."))
                        .addOnFailureListener(e ->
                            Log.e(TAG, "Firestore write failed: " + e.getMessage()));
                });
        });

        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void routeToDashboard(String role) {
        Intent intent;
        if ("Employer".equals(role)) {
            intent = new Intent(this, EmployerDashboardActivity.class);
        } else if ("Recruiter".equals(role)) {
            intent = new Intent(this, RecruiterDashboardActivity.class);
        } else {
            intent = new Intent(this, FreelancerDashboardActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
