package com.nibm.worklink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextInputEditText etEmail, etPassword;
    private Button btnSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // Always start fresh — force user to log in every time
        mAuth.signOut();

        etEmail   = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnSignIn  = findViewById(R.id.btn_sign_in);
        Button btnSignUp = findViewById(R.id.btn_sign_up);

        btnSignIn.setOnClickListener(v -> {
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty()) {
                etEmail.setError("Email is required");
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Password is required");
                return;
            }

            btnSignIn.setEnabled(false);
            btnSignIn.setText("Signing in...");

            mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        btnSignIn.setEnabled(true);
                        btnSignIn.setText("Sign In ➔");
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Sign in failed";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Auth OK — look up role in Firestore
                    String uid = mAuth.getCurrentUser().getUid();
                    db.collection("Users").document(uid).get()
                        .addOnSuccessListener(doc -> {
                            if (!doc.exists()) {
                                // Document missing — user has no profile stored
                                btnSignIn.setEnabled(true);
                                btnSignIn.setText("Sign In ➔");
                                Toast.makeText(this,
                                    "No user profile found. Please sign up first or contact your admin.",
                                    Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                                return;
                            }
                            String role = doc.getString("role");
                            Intent intent;
                            if ("Admin".equals(role)) {
                                intent = new Intent(this, AdminDashboardActivity.class);
                            } else if ("Employer".equals(role)) {
                                intent = new Intent(this, EmployerDashboardActivity.class);
                            } else if ("Recruiter".equals(role)) {
                                intent = new Intent(this, RecruiterDashboardActivity.class);
                            } else {
                                intent = new Intent(this, FreelancerDashboardActivity.class);
                            }
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            btnSignIn.setEnabled(true);
                            btnSignIn.setText("Sign In ➔");
                            Toast.makeText(this,
                                "Could not load profile: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                        });
                });
        });

        btnSignUp.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class))
        );
    }
}
