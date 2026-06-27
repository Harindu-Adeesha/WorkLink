package com.nibm.worklink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText etName = findViewById(R.id.et_register_name);
        EditText etEmail = findViewById(R.id.et_register_email);
        EditText etPassword = findViewById(R.id.et_register_password);
        EditText etSkills = findViewById(R.id.et_register_skills);
        Button btnRegister = findViewById(R.id.btn_register);
        TextView tvBackToLogin = findViewById(R.id.tv_back_to_login);

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String skills = etSkills.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create Profile in DataManager
            DataManager.Profile mockProfile = new DataManager.Profile(
                    name,
                    email,
                    "Freelance Consultant",
                    "Experienced freelancer specialized in creative solutions and professional delivery.",
                    skills.isEmpty() ? "Generalist" : skills,
                    "$45.00 / hr"
            );
            DataManager.setProfile(mockProfile);

            Toast.makeText(RegisterActivity.this, "Registration Successful! Welcome " + name, Toast.LENGTH_LONG).show();

            // Log in directly
            Intent intent = new Intent(RegisterActivity.this, FreelancerDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        tvBackToLogin.setOnClickListener(v -> {
            finish(); // Go back to login screen
        });
    }
}
