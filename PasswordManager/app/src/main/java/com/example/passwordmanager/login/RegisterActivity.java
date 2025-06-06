package com.example.passwordmanager.login;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.passwordmanager.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private static final long VERIFICATION_TIMEOUT = 60000; // 60 seconds

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Handler verificationHandler;
    private Runnable verificationTimeoutRunnable;

    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        verificationHandler = new Handler(Looper.getMainLooper());

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvSignIn);
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Return to login screen
            }
        });
    }

    private void registerUser() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validate input
        if (fullName.isEmpty()) {
            etFullName.setError("Vui lòng nhập họ tên");
            etFullName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        // Create account with Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Send verification email
                                sendVerificationEmail(user, fullName);
                            }
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(RegisterActivity.this,
                                    "Đăng ký thất bại: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void sendVerificationEmail(FirebaseUser user, String fullName) {
        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this,
                                    "Email xác nhận đã được gửi. Vui lòng xác nhận trong 30 giây!",
                                    Toast.LENGTH_LONG).show();

                            // Start verification timeout
                            startVerificationCheck(user, fullName);
                        } else {
                            Log.w(TAG, "sendEmailVerification:failure", task.getException());
                            Toast.makeText(RegisterActivity.this,
                                    "Lỗi khi gửi email xác nhận: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                            // Delete user if email verification sending fails
                            user.delete();
                        }
                    }
                });
    }

    private void startVerificationCheck(FirebaseUser user, String fullName) {
        verificationTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                user.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (user.isEmailVerified()) {
                            // Email verified, save to Firestore
                            saveUserToFirestore(user, fullName);
                        } else {
                            // Timeout and not verified, delete user
                            user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "User deleted due to verification timeout");
                                        Toast.makeText(RegisterActivity.this,
                                                "Xác nhận email quá thời gian. Vui lòng thử lại.",
                                                Toast.LENGTH_LONG).show();
                                        clearInputFields();
                                    }
                                }
                            });
                        }
                    }
                });
            }
        };

        // Check verification status every second for 30 seconds
        verificationHandler.postDelayed(verificationTimeoutRunnable, VERIFICATION_TIMEOUT);
    }

    private void saveUserToFirestore(FirebaseUser user, String fullName) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("fullName", fullName);
        userData.put("email", user.getEmail());
        userData.put("createdAt", System.currentTimeMillis());

        db.collection("users")
                .document(user.getUid())
                .set(userData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // Cancel verification timeout
                        verificationHandler.removeCallbacks(verificationTimeoutRunnable);

                        if (task.isSuccessful()) {
                            Log.d(TAG, "User data saved to Firestore");
                            Toast.makeText(RegisterActivity.this,
                                    "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "Error saving user data", task.getException());
                            Toast.makeText(RegisterActivity.this,
                                    "Lỗi khi lưu thông tin: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }

                        // Navigate to LoginActivity
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private void clearInputFields() {
        etFullName.setText("");
        etEmail.setText("");
        etPassword.setText("");
        etConfirmPassword.setText("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up handler callbacks
        if (verificationHandler != null && verificationTimeoutRunnable != null) {
            verificationHandler.removeCallbacks(verificationTimeoutRunnable);
        }
    }
}