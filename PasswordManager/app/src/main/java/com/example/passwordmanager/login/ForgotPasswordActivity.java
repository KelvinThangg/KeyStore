package com.example.passwordmanager.login;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.passwordmanager.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText etEmail;
    private Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void setupClickListeners() {
        btnSubmit.setOnClickListener(v -> checkAndSendResetEmail());
    }

    private void checkAndSendResetEmail() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        sendPasswordResetEmail(email);
                    } else {
                        Toast.makeText(this, "Email chưa được đăng ký", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error checking email in Firestore", e);
                    Toast.makeText(this, "Lỗi kiểm tra email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Email đặt lại mật khẩu đã được gửi!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Log.w(TAG, "sendPasswordResetEmail:failure", task.getException());
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Không xác định";
                        Toast.makeText(this, "Gửi email thất bại: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}