package com.example.passwordmanager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreatePinActivity extends AppCompatActivity {

    private static final String TAG = "CreatePinActivity";

    private TextView tvPinDisplay, tvTitle;
    private Button[] btnNumbers = new Button[10];
    private Button btnDelete, btnConfirm;

    private StringBuilder pinBuilder = new StringBuilder();
    private String firstPin = "";
    private boolean isConfirmMode = false;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_create_pin);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("PasswordManagerPrefs", MODE_PRIVATE);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        tvPinDisplay = findViewById(R.id.tvPinDisplay);
        tvTitle = findViewById(R.id.tvTitle);

        // Khởi tạo các nút số
        btnNumbers[0] = findViewById(R.id.btn0);
        btnNumbers[1] = findViewById(R.id.btn1);
        btnNumbers[2] = findViewById(R.id.btn2);
        btnNumbers[3] = findViewById(R.id.btn3);
        btnNumbers[4] = findViewById(R.id.btn4);
        btnNumbers[5] = findViewById(R.id.btn5);
        btnNumbers[6] = findViewById(R.id.btn6);
        btnNumbers[7] = findViewById(R.id.btn7);
        btnNumbers[8] = findViewById(R.id.btn8);
        btnNumbers[9] = findViewById(R.id.btn9);

        btnDelete = findViewById(R.id.btnDelete);
        btnConfirm = findViewById(R.id.btnConfirm);
    }

    private void setupClickListeners() {
        // Thiết lập click listener cho các nút số
        for (int i = 0; i < 10; i++) {
            final int number = i;
            btnNumbers[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addDigit(String.valueOf(number));
                }
            });
        }

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteDigit();
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmPin();
            }
        });
    }

    private void addDigit(String digit) {
        if (pinBuilder.length() < 6) {
            pinBuilder.append(digit);
            updatePinDisplay();

            if (pinBuilder.length() == 6) {
                btnConfirm.setVisibility(View.VISIBLE);
            }
        }
    }

    private void deleteDigit() {
        if (pinBuilder.length() > 0) {
            pinBuilder.deleteCharAt(pinBuilder.length() - 1);
            updatePinDisplay();

            if (pinBuilder.length() < 6) {
                btnConfirm.setVisibility(View.GONE);
            }
        }
    }

    private void updatePinDisplay() {
        StringBuilder display = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            if (i < pinBuilder.length()) {
                display.append("● ");
            } else {
                display.append("○ ");
            }
        }
        tvPinDisplay.setText(display.toString().trim());
    }

    private void confirmPin() {
        if (pinBuilder.length() != 6) {
            Toast.makeText(this, "Vui lòng nhập đủ 6 số", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isConfirmMode) {
            // Lần nhập đầu tiên
            firstPin = pinBuilder.toString();
            isConfirmMode = true;

            // Reset giao diện để nhập lại
            pinBuilder.setLength(0);
            updatePinDisplay();
            btnConfirm.setVisibility(View.GONE);

            tvTitle.setText("Xác nhận mã PIN");
            Toast.makeText(this, "Vui lòng nhập lại mã PIN để xác nhận", Toast.LENGTH_SHORT).show();

        } else {
            // Lần nhập thứ hai - xác nhận
            String confirmPin = pinBuilder.toString();

            if (firstPin.equals(confirmPin)) {
                // PIN khớp - lưu vào SharedPreferences và Firestore
                savePinAndProceed(firstPin);
            } else {
                // PIN không khớp - reset
                Toast.makeText(this, "Mã PIN không trùng khớp. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                resetPinCreation();
            }
        }
    }

    private void savePinAndProceed(String pin) {
        // Lưu vào SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userPin", pin);
        editor.putBoolean("hasPin", true);
        editor.apply();

        // Lưu vào Firestore
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("pin", pin);
            userData.put("pinCreatedAt", System.currentTimeMillis());

            // Kiểm tra xem tài liệu người dùng đã tồn tại chưa
            db.collection("users").document(user.getUid()).get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                Task<Void> firestoreTask;
                                if (document.exists()) {
                                    // Tài liệu đã tồn tại, sử dụng update
                                    firestoreTask = db.collection("users")
                                            .document(user.getUid())
                                            .update(userData);
                                } else {
                                    // Tài liệu chưa tồn tại, sử dụng set
                                    firestoreTask = db.collection("users")
                                            .document(user.getUid())
                                            .set(userData);
                                }

                                firestoreTask.addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "PIN saved to Firestore");
                                            Toast.makeText(CreatePinActivity.this,
                                                    "Tạo mã PIN thành công!", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Log.w(TAG, "Error saving PIN to Firestore", task.getException());
                                            Toast.makeText(CreatePinActivity.this,
                                                    "Lỗi khi lưu mã PIN: " + task.getException().getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        }

                                        // Chuyển đến DashboardActivity
                                        navigateToDashboard();
                                    }
                                });
                            } else {
                                Log.w(TAG, "Error checking user document", task.getException());
                                Toast.makeText(CreatePinActivity.this,
                                        "Lỗi khi kiểm tra dữ liệu người dùng: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                                navigateToDashboard(); // Vẫn chuyển hướng để không làm gián đoạn
                            }
                        }
                    });
        } else {
            Log.w(TAG, "No user logged in");
            Toast.makeText(this, "Không tìm thấy người dùng. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            resetPinCreation();
            // Chuyển đến LoginActivity nếu người dùng chưa đăng nhập
            Intent intent = new Intent(CreatePinActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void navigateToDashboard() {
        Log.d(TAG, "Navigating to DashboardActivity");
        Intent intent = new Intent(CreatePinActivity.this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void resetPinCreation() {
        firstPin = "";
        isConfirmMode = false;
        pinBuilder.setLength(0);
        updatePinDisplay();
        btnConfirm.setVisibility(View.GONE);
        tvTitle.setText("Tạo mã PIN");
    }
}