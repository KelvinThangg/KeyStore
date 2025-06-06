package com.example.passwordmanager.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler; // Import Handler
import android.os.Looper;  // Import Looper
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.passwordmanager.R;
import com.example.passwordmanager.dashboard.DashboardActivity;
// import androidx.core.content.ContextCompat; // Không cần thiết nếu bạn đã có màu trong drawable

public class PinActivity extends AppCompatActivity {

    // private TextView tvPinDisplay; // Đã xóa
    private View[] pinDots = new View[6]; // Mảng chứa các View chấm PIN
    private TextView tvTitle;
    private TextView tvErrorMessage; // TextView để hiển thị thông báo lỗi

    private Button[] btnNumbers = new Button[10];
    private Button btnDelete;
    private TextView btnForgotPin; // Vẫn là TextView như đã sửa trước đó

    private StringBuilder pinBuilder = new StringBuilder();
    private SharedPreferences sharedPreferences;
    private int failedAttempts = 0;
    private static final int MAX_ATTEMPTS = 3;
    private Handler handler; // Handler để thực hiện các tác vụ trì hoãn

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        sharedPreferences = getSharedPreferences("PasswordManagerPrefs", MODE_PRIVATE);
        handler = new Handler(Looper.getMainLooper());

        initViews();
        setupClickListeners();
        updatePinDisplay(); // Hiển thị trạng thái ban đầu của các chấm PIN (trống)
    }

    private void initViews() {
        // Khởi tạo các View chấm PIN
        pinDots[0] = findViewById(R.id.dot1);
        pinDots[1] = findViewById(R.id.dot2);
        pinDots[2] = findViewById(R.id.dot3);
        pinDots[3] = findViewById(R.id.dot4);
        pinDots[4] = findViewById(R.id.dot5);
        pinDots[5] = findViewById(R.id.dot6);

        tvTitle = findViewById(R.id.tvTitle);
        tvErrorMessage = findViewById(R.id.tvErrorMessage); // Khởi tạo TextView thông báo lỗi

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
        btnForgotPin = findViewById(R.id.tvForgotPin);

        if (tvTitle != null) {
            tvTitle.setText("Nhập mã PIN");
        }
        if (tvErrorMessage != null) {
            tvErrorMessage.setVisibility(View.GONE); // Ban đầu ẩn thông báo lỗi
        }
    }

    private void setupClickListeners() {
        for (int i = 0; i < btnNumbers.length; i++) {
            // Kiểm tra null phòng trường hợp ID nút không có trong layout
            if (btnNumbers[i] != null) {
                final int number = i; // Biến final để sử dụng trong lambda
                btnNumbers[i].setOnClickListener(v -> addDigit(String.valueOf(number)));
            }
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> deleteDigit());
        }

        if (btnForgotPin != null) {
            btnForgotPin.setOnClickListener(v -> handleForgotPin());
        }
    }

    private void addDigit(String digit) {
        // Ẩn thông báo lỗi nếu đang hiển thị khi người dùng nhập số mới
        if (tvErrorMessage != null && tvErrorMessage.getVisibility() == View.VISIBLE) {
            tvErrorMessage.setVisibility(View.GONE);
        }

        if (pinBuilder.length() < 6) {
            pinBuilder.append(digit);
            updatePinDisplay();


            // Tự động kiểm tra PIN khi đủ 6 số
            if (pinBuilder.length() == 6) {
                // Trì hoãn một chút để người dùng thấy chấm cuối cùng được điền
                handler.postDelayed(this::checkPin, 100);
            }
        }
    }

    private void deleteDigit() {
        // Ẩn thông báo lỗi nếu đang hiển thị khi người dùng xóa số
        if (tvErrorMessage != null && tvErrorMessage.getVisibility() == View.VISIBLE) {
            tvErrorMessage.setVisibility(View.GONE);
        }

        if (pinBuilder.length() > 0) {
            pinBuilder.deleteCharAt(pinBuilder.length() - 1);
            updatePinDisplay();
        }
    }

    private void updatePinDisplay() {
        for (int i = 0; i < pinDots.length; i++) {
            if (pinDots[i] != null) { // Luôn kiểm tra null trước khi sử dụng
                if (i < pinBuilder.length()) {
                    // Đặt background cho chấm đã điền
                    pinDots[i].setBackgroundResource(R.drawable.pin_dot_filled);
                } else {
                    // Đặt background cho chấm trống
                    pinDots[i].setBackgroundResource(R.drawable.pin_dot_empty);
                }
            }
        }
    }

    private void checkPin() {
        String enteredPin = pinBuilder.toString();
        String savedPin = sharedPreferences.getString("userPin", null);

        if (savedPin == null) {
            Toast.makeText(this, "Mã PIN chưa được thiết lập.", Toast.LENGTH_LONG).show();
            if (tvErrorMessage != null) {
                tvErrorMessage.setText("Mã PIN chưa được thiết lập. Vui lòng đăng nhập lại.");
                tvErrorMessage.setVisibility(View.VISIBLE);
            }
            // Chuyển về màn hình đăng nhập sau một khoảng trễ ngắn
            handler.postDelayed(this::handleForgotPin, 2000); // 2 giây trễ
            return;
        }

        if (enteredPin.equals(savedPin)) {
            // PIN đúng
            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
            failedAttempts = 0;
            if (tvErrorMessage != null) {
                tvErrorMessage.setVisibility(View.GONE); // Ẩn thông báo lỗi nếu có
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isLoggedIn", true);
            editor.apply();

            Intent intent = new Intent(this, DashboardActivity.class);
            startActivity(intent);
            finish();

        } else {
            // PIN sai
            failedAttempts++;


            String errorMessageText;
            if (failedAttempts >= MAX_ATTEMPTS) {
                errorMessageText = "Quá số lần thử cho phép. Vui lòng đăng nhập lại.";
                Toast.makeText(this, errorMessageText, Toast.LENGTH_LONG).show();
                if (tvErrorMessage != null) {
                    tvErrorMessage.setText(errorMessageText);
                    tvErrorMessage.setVisibility(View.VISIBLE);
                }
                // Trì hoãn trước khi thực hiện hành động đăng xuất
                handler.postDelayed(this::handleForgotPin, 2000); // 2 giây trễ
            } else {
                errorMessageText = "Mã PIN không đúng. Còn " + (MAX_ATTEMPTS - failedAttempts) + " lần thử.";
                Toast.makeText(this, errorMessageText, Toast.LENGTH_SHORT).show();
                if (tvErrorMessage != null) {
                    tvErrorMessage.setText(errorMessageText);
                    tvErrorMessage.setVisibility(View.VISIBLE);
                }

                // Reset PIN input sau một khoảng trễ ngắn để người dùng thấy thông báo lỗi
                handler.postDelayed(() -> {
                    pinBuilder.setLength(0);
                    updatePinDisplay();
                    // Có thể ẩn tvErrorMessage ở đây, hoặc để nó hiển thị cho đến khi người dùng nhập tiếp
                    // if (tvErrorMessage != null) {
                    //    tvErrorMessage.setVisibility(View.GONE);
                    // }
                }, 1500); // 1.5 giây trễ
            }
        }
    }

    private void handleForgotPin() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.remove("userPin");
        editor.apply();

        Toast.makeText(this, "Đã đăng xuất. Vui lòng đăng nhập lại và thiết lập PIN mới nếu cần.", Toast.LENGTH_LONG).show();
        // Ẩn thông báo lỗi nếu đang hiển thị
        if (tvErrorMessage != null) {
            tvErrorMessage.setVisibility(View.GONE);
        }

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed(); // Bạn có thể giữ lại hoặc bỏ đi tùy theo logic mong muốn
        super.onBackPressed();
        Toast.makeText(this, "Vui lòng nhập mã PIN để tiếp tục hoặc sử dụng nút 'Quên PIN'", Toast.LENGTH_SHORT).show();
        // Xem xét việc hiển thị dialog xác nhận thoát nếu bạn muốn vô hiệu hóa hoàn toàn nút back
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Gỡ bỏ tất cả các callbacks và messages của handler để tránh rò rỉ bộ nhớ
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}