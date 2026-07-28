package com.nibm.worklink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvName  = findViewById(R.id.tv_profile_name);
        TextView tvEmail = findViewById(R.id.tv_profile_email);
        TextView tvRole  = findViewById(R.id.tv_profile_role);
        MaterialButton btnLogout = findViewById(R.id.btn_logout);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Load real user data from Firestore
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("Users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name  = doc.getString("name");
                        String email = doc.getString("email");
                        String role  = doc.getString("role");
                        tvName.setText(name  != null ? name  : "Unknown");
                        tvEmail.setText(email != null ? email : "");
                        tvRole.setText(role   != null ? role  : "");
                    } else {
                        tvName.setText("No profile found");
                    }
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                );
        }

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
