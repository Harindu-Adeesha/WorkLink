package com.nibm.worklink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText etUsername = findViewById(R.id.et_username);
        Button btnSignIn = findViewById(R.id.btn_sign_in);

        btnSignIn.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            if ("admin".equals(username)) {
                Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                startActivity(intent);
                finish(); // Optional: finish login activity
            } else {
                Toast.makeText(LoginActivity.this, "Invalid credentials or not an admin", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
