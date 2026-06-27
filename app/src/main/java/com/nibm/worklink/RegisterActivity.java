package com.nibm.worklink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
        
        RadioGroup rgRole = findViewById(R.id.rg_role);
        TextView tvSkillsLabel = findViewById(R.id.tv_register_skills_label);

        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_employer) {
                tvSkillsLabel.setText("Company Description / Bio");
                etSkills.setHint("e.g. Leading software engineering company...");
            } else {
                tvSkillsLabel.setText("Skills / Area of Expertise");
                etSkills.setHint("Kotlin, Android, Figma, Java...");
            }
        });

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String skills = etSkills.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int checkedId = rgRole.getCheckedRadioButtonId();
            if (checkedId == R.id.rb_employer) {
                // Create Employer Profile in DataManager
                DataManager.EmployerProfile mockProfile = new DataManager.EmployerProfile(
                        name,
                        email,
                        "071-1234567",
                        skills.isEmpty() ? "Leading provider of professional services." : skills,
                        5.0f
                );
                DataManager.setEmployerProfile(mockProfile);

                Toast.makeText(RegisterActivity.this, "Employer Registration Successful! Welcome " + name, Toast.LENGTH_LONG).show();

                // Go to Employer Dashboard
                Intent intent = new Intent(RegisterActivity.this, EmployerDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                // Create Freelancer Profile in DataManager
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

                // Go to Freelancer Dashboard
                Intent intent = new Intent(RegisterActivity.this, FreelancerDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });

        tvBackToLogin.setOnClickListener(v -> {
            finish(); // Go back to login screen
        });
    }
}
