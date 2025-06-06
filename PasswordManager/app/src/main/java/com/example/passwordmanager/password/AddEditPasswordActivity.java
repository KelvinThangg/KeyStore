package com.example.passwordmanager.password; // Package name từ file bạn cung cấp

import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.passwordmanager.R;
import com.example.passwordmanager.dashboard.PasswordItem;
import com.example.passwordmanager.model.PlatformSuggestion;
import com.example.passwordmanager.model.PlatformSuggestionAdapter; // Sửa đổi đường dẫn import nếu cần
import com.example.passwordmanager.utils.EncryptionUtils;       // Import EncryptionUtils
import com.example.passwordmanager.password.PasswordGenerator;       // Import PasswordGenerator
import com.example.passwordmanager.password.PasswordStrengthChecker; // Import PasswordStrengthChecker

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AddEditPasswordActivity extends AppCompatActivity {

    private static final String TAG = "AddEditPasswordActivity";
    public static final String EXTRA_PASSWORD_ID = "com.example.passwordmanager.EXTRA_PASSWORD_ID";

    private Toolbar toolbar;
    private TextInputLayout tilPlatformName, tilUsername, tilPassword, tilWebsiteUrl, tilNotes;
    private AutoCompleteTextView actPlatformName;
    private ImageView ivSelectedPlatformLogo;

    private TextInputEditText etUsername, etPassword, etWebsiteUrl, etNotes;
    private TextView tvPasswordStrength, tvPasswordLength;
    private ProgressBar pbPasswordStrength;
    private SeekBar sbPasswordLength;
    private SwitchMaterial swUppercase, swNumeric, swSpecialChars;
    private MaterialButton btnGeneratePassword, btnSavePassword;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentPasswordId;
    private PasswordItem existingPasswordItem;

    private List<PlatformSuggestion> platformSuggestionsList;
    private PlatformSuggestionAdapter platformSuggestionAdapter;
    private EncryptionUtils encryptionUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_password);

        try {
            encryptionUtils = new EncryptionUtils();
        } catch (RuntimeException e) {
            Toast.makeText(this, "Lỗi bảo mật khởi tạo. Không thể tiếp tục.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Không thể khởi tạo EncryptionUtils", e);
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupToolbar();
        initializePlatformSuggestions();
        setupPlatformAutoComplete();
        setupPasswordGenerationControls();
        setupPasswordStrengthChecker();
        setupSaveButton();

        if (getIntent().hasExtra(EXTRA_PASSWORD_ID)) {
            currentPasswordId = getIntent().getStringExtra(EXTRA_PASSWORD_ID);
            if (toolbar != null) toolbar.setTitle("Sửa mật khẩu");
            if (btnSavePassword != null) btnSavePassword.setText("Cập nhật mật khẩu");
            loadPasswordDetails(currentPasswordId);
        } else {
            if (toolbar != null) toolbar.setTitle("Thêm mật khẩu mới");
            if (sbPasswordLength != null) sbPasswordLength.setProgress(12);
            if (tvPasswordLength != null) tvPasswordLength.setText(String.valueOf(12));
            if(ivSelectedPlatformLogo != null) ivSelectedPlatformLogo.setVisibility(View.GONE);
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarAddEditPassword);
        tilPlatformName = findViewById(R.id.tilPlatformName);
        actPlatformName = findViewById(R.id.actPlatformName);
        ivSelectedPlatformLogo = findViewById(R.id.ivSelectedPlatformLogo);

        tilUsername = findViewById(R.id.tilUsername);
        etUsername = findViewById(R.id.etUsername);
        tilPassword = findViewById(R.id.tilPassword);
        etPassword = findViewById(R.id.etPassword);
        tilWebsiteUrl = findViewById(R.id.tilWebsiteUrl);
        etWebsiteUrl = findViewById(R.id.etWebsiteUrl);
        tilNotes = findViewById(R.id.tilNotes);
        etNotes = findViewById(R.id.etNotes);

        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);
        pbPasswordStrength = findViewById(R.id.pbPasswordStrength);
        sbPasswordLength = findViewById(R.id.sbPasswordLength);
        tvPasswordLength = findViewById(R.id.tvPasswordLength);
        swUppercase = findViewById(R.id.swUppercase);
        swNumeric = findViewById(R.id.swNumeric);
        swSpecialChars = findViewById(R.id.swSpecialChars);
        btnGeneratePassword = findViewById(R.id.btnGeneratePassword);
        btnSavePassword = findViewById(R.id.btnSavePassword);
    }

    private void setupToolbar() {
        if (toolbar == null) return;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void initializePlatformSuggestions() {
        platformSuggestionsList = new ArrayList<>();
        // Đảm bảo bạn đã có các drawable logo trong res/drawable
        platformSuggestionsList.add(new PlatformSuggestion("Google", R.drawable.ic_logo_google, "https://accounts.google.com"));
        platformSuggestionsList.add(new PlatformSuggestion("Facebook", R.drawable.ic_logo_facebook, "https://www.facebook.com"));
        platformSuggestionsList.add(new PlatformSuggestion("Instagram", R.drawable.ic_logo_instagram, "https://www.instagram.com"));
        platformSuggestionsList.add(new PlatformSuggestion("GitHub", R.drawable.ic_logo_github, "https://www.github.com"));
        platformSuggestionsList.add(new PlatformSuggestion("Apple", R.drawable.ic_logo_apple, "https://appleid.apple.com"));
        // Thêm các nền tảng khác nếu muốn
    }

    private void setupPlatformAutoComplete() {
        if (actPlatformName == null || platformSuggestionsList == null) {
            Log.e(TAG, "AutoCompleteTextView hoặc danh sách gợi ý là null. Không thể thiết lập.");
            return;
        }

        platformSuggestionAdapter = new PlatformSuggestionAdapter(this, platformSuggestionsList);
        actPlatformName.setAdapter(platformSuggestionAdapter);
        actPlatformName.setThreshold(1); // Bắt đầu gợi ý sau khi nhập 1 ký tự

        actPlatformName.setOnItemClickListener((parent, view, position, id) -> {
            PlatformSuggestion selected = platformSuggestionAdapter.getItem(position);
            if (selected != null) {
                if (etWebsiteUrl != null && (etWebsiteUrl.getText() == null || etWebsiteUrl.getText().toString().isEmpty())) {
                    etWebsiteUrl.setText(selected.getWebsiteUrl());
                }
                if (ivSelectedPlatformLogo != null) {
                    if (selected.getLogoResId() != 0) {
                        ivSelectedPlatformLogo.setImageResource(selected.getLogoResId());
                        ivSelectedPlatformLogo.setVisibility(View.VISIBLE);
                    } else {
                        ivSelectedPlatformLogo.setImageResource(R.drawable.ic_default_platform);
                        ivSelectedPlatformLogo.setVisibility(View.VISIBLE);
                    }
                }
                actPlatformName.setSelection(actPlatformName.getText().length());
            }
        });

        actPlatformName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().isEmpty() && ivSelectedPlatformLogo != null) {
                    ivSelectedPlatformLogo.setVisibility(View.GONE);
                } else {
                    boolean matched = false;
                    if (ivSelectedPlatformLogo != null && platformSuggestionsList != null) {
                        for (PlatformSuggestion suggestion : platformSuggestionsList) {
                            if (suggestion.getName().equalsIgnoreCase(s.toString())) {
                                if (suggestion.getLogoResId() != 0) {
                                    ivSelectedPlatformLogo.setImageResource(suggestion.getLogoResId());
                                } else {
                                    ivSelectedPlatformLogo.setImageResource(R.drawable.ic_default_platform);
                                }
                                ivSelectedPlatformLogo.setVisibility(View.VISIBLE);
                                matched = true;
                                break;
                            }
                        }
                        if (!matched) {
                            ivSelectedPlatformLogo.setVisibility(View.GONE);
                        }
                    }
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupPasswordGenerationControls() {
        if (sbPasswordLength == null || tvPasswordLength == null || btnGeneratePassword == null || etPassword == null ||
                swUppercase == null || swNumeric == null || swSpecialChars == null) {
            Log.e(TAG, "Một hoặc nhiều view cho việc tạo mật khẩu là null.");
            return;
        }
        sbPasswordLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 8) { seekBar.setProgress(8); tvPasswordLength.setText("8"); }
                else { tvPasswordLength.setText(String.valueOf(progress)); }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        btnGeneratePassword.setOnClickListener(v -> {
            int length = sbPasswordLength.getProgress();
            if (length < 8) length = 8;
            String generatedPassword = PasswordGenerator.generatePassword(length, swUppercase.isChecked(), swNumeric.isChecked(), swSpecialChars.isChecked());
            etPassword.setText(generatedPassword);
            etPassword.requestFocus();
        });
    }

    private void setupPasswordStrengthChecker() {
        if (etPassword == null) { Log.e(TAG, "EditText cho mật khẩu là null."); return; }
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updatePasswordStrengthUI(s.toString()); }
        });
        if (etPassword.getText() != null && !etPassword.getText().toString().isEmpty()) {
            updatePasswordStrengthUI(etPassword.getText().toString());
        } else {
            updatePasswordStrengthUI("");
        }
    }

    private void updatePasswordStrengthUI(String password) {
        if (tvPasswordStrength == null || pbPasswordStrength == null) { Log.e(TAG, "View cho độ mạnh mật khẩu là null."); return; }
        PasswordStrengthChecker.StrengthLevel strength = PasswordStrengthChecker.calculateStrength(password);
        int strengthColor = ContextCompat.getColor(this, strength.getColorResId());
        tvPasswordStrength.setText("Độ mạnh: " + strength.getDisplayName());
        tvPasswordStrength.setTextColor(strengthColor);
        LayerDrawable progressBarDrawable = (LayerDrawable) pbPasswordStrength.getProgressDrawable();
        if (progressBarDrawable != null) progressBarDrawable.setColorFilter(strengthColor, PorterDuff.Mode.SRC_IN);
        switch (strength) {
            case EMPTY: pbPasswordStrength.setProgress(0); break;
            case VERY_WEAK: pbPasswordStrength.setProgress(20); break;
            case WEAK: pbPasswordStrength.setProgress(40); break;
            case MEDIUM: pbPasswordStrength.setProgress(60); break;
            case STRONG: pbPasswordStrength.setProgress(80); break;
            case VERY_STRONG: pbPasswordStrength.setProgress(100); break;
        }
    }

    private void loadPasswordDetails(String passwordId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) { Toast.makeText(this, "Người dùng chưa đăng nhập.", Toast.LENGTH_SHORT).show(); finish(); return; }
        String userId = currentUser.getUid();
        db.collection("users").document(userId).collection("passwords").document(passwordId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        existingPasswordItem = documentSnapshot.toObject(PasswordItem.class);
                        if (existingPasswordItem != null) {
                            if (actPlatformName != null) actPlatformName.setText(existingPasswordItem.getPlatformName());
                            if (etUsername != null) etUsername.setText(existingPasswordItem.getUsername());
                            if (etPassword != null) {
                                etPassword.setText("");
                                etPassword.setHint("Nhập mật khẩu mới nếu muốn thay đổi");
                            }
                            if (etWebsiteUrl != null) etWebsiteUrl.setText(existingPasswordItem.getWebsiteUrl());
                            if (etNotes != null) etNotes.setText(existingPasswordItem.getNotes());

                            if (ivSelectedPlatformLogo != null && platformSuggestionsList != null) {
                                boolean logoSet = false;
                                for (PlatformSuggestion suggestion : platformSuggestionsList) {
                                    if (suggestion.getName().equalsIgnoreCase(existingPasswordItem.getPlatformName())) {
                                        if (suggestion.getLogoResId() != 0) {
                                            ivSelectedPlatformLogo.setImageResource(suggestion.getLogoResId());
                                            ivSelectedPlatformLogo.setVisibility(View.VISIBLE);
                                            logoSet = true;
                                        }
                                        break;
                                    }
                                }
                                if (!logoSet && existingPasswordItem.getPlatformName() != null && !existingPasswordItem.getPlatformName().isEmpty()) {
                                    ivSelectedPlatformLogo.setImageResource(R.drawable.ic_default_platform);
                                    ivSelectedPlatformLogo.setVisibility(View.VISIBLE);
                                } else if (!logoSet) {
                                    ivSelectedPlatformLogo.setVisibility(View.GONE);
                                }
                            }
                        }
                    } else { Toast.makeText(this, "Không tìm thấy chi tiết mật khẩu.", Toast.LENGTH_SHORT).show(); finish(); }
                })
                .addOnFailureListener(e -> { Toast.makeText(this, "Lỗi khi tải mật khẩu: " + e.getMessage(), Toast.LENGTH_SHORT).show(); Log.e(TAG, "Error loading password", e); finish(); });
    }

    private void setupSaveButton() {
        if (btnSavePassword == null) { Log.e(TAG, "Nút Lưu là null."); return; }
        btnSavePassword.setOnClickListener(v -> savePassword());
    }

    private void savePassword() {
        String platformName = (actPlatformName != null && actPlatformName.getText() != null) ? actPlatformName.getText().toString().trim() : "";
        String username = (etUsername != null && etUsername.getText() != null) ? etUsername.getText().toString().trim() : "";
        String plainPassword = (etPassword != null && etPassword.getText() != null) ? etPassword.getText().toString().trim() : "";
        String websiteUrl = (etWebsiteUrl != null && etWebsiteUrl.getText() != null) ? etWebsiteUrl.getText().toString().trim() : "";
        String notes = (etNotes != null && etNotes.getText() != null) ? etNotes.getText().toString().trim() : "";

        if (platformName.isEmpty()) { if (tilPlatformName != null) tilPlatformName.setError("Tên nền tảng không được để trống"); if (actPlatformName != null) actPlatformName.requestFocus(); return; }
        else { if (tilPlatformName != null) tilPlatformName.setError(null); }
        if (username.isEmpty()) { if (tilUsername != null) tilUsername.setError("Tên người dùng không được để trống"); if (etUsername != null) etUsername.requestFocus(); return; }
        else { if (tilUsername != null) tilUsername.setError(null); }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) { Toast.makeText(this, "Người dùng chưa đăng nhập. Không thể lưu.", Toast.LENGTH_LONG).show(); return; }
        String userId = currentUser.getUid();

        String passwordToSave;
        if (encryptionUtils == null) {
            Log.e(TAG, "EncryptionUtils is null. Cannot save password.");
            Toast.makeText(this, "Lỗi hệ thống mã hóa. Không thể lưu mật khẩu.", Toast.LENGTH_LONG).show();
            return;
        }

        if (currentPasswordId != null && existingPasswordItem != null) { // Chế độ sửa
            if (plainPassword.isEmpty()) {
                passwordToSave = existingPasswordItem.getPassword();
                Log.d(TAG, "Mật khẩu không thay đổi, giữ nguyên mật khẩu đã mã hóa.");
            } else {
                passwordToSave = encryptionUtils.encrypt(plainPassword);
                if (passwordToSave == null) {
                    Log.e(TAG, "EncryptionUtils.encrypt() returned null for new password during edit. Plain password length: " + plainPassword.length());
                    Toast.makeText(this, "Lỗi mã hóa mật khẩu mới. Không thể lưu.", Toast.LENGTH_LONG).show();
                    return;
                }
                Log.d(TAG, "Mật khẩu mới đã được mã hóa khi sửa.");
            }
        } else { // Chế độ thêm mới
            if (plainPassword.isEmpty()) {
                if (tilPassword != null) tilPassword.setError("Mật khẩu không được để trống");
                if (etPassword != null) etPassword.requestFocus();
                return;
            }
            passwordToSave = encryptionUtils.encrypt(plainPassword);
            if (passwordToSave == null) {
                Log.e(TAG, "EncryptionUtils.encrypt() returned null for new password during add. Plain password length: " + plainPassword.length());
                Toast.makeText(this, "Lỗi mã hóa mật khẩu. Không thể lưu.", Toast.LENGTH_LONG).show();
                return;
            }
            Log.d(TAG, "Mật khẩu mới đã được mã hóa khi thêm.");
        }
        if (tilPassword != null) tilPassword.setError(null);


        PasswordItem passwordItem = new PasswordItem(platformName, username, passwordToSave, websiteUrl, notes, userId);
        passwordItem.setUpdatedAt(new Date());

        if (currentPasswordId != null && existingPasswordItem != null) {
            passwordItem.setCreatedAt(existingPasswordItem.getCreatedAt());
            DocumentReference docRef = db.collection("users").document(userId)
                    .collection("passwords").document(currentPasswordId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("platformName", passwordItem.getPlatformName());
            updates.put("username", passwordItem.getUsername());
            updates.put("password", passwordItem.getPassword());
            updates.put("websiteUrl", passwordItem.getWebsiteUrl());
            updates.put("notes", passwordItem.getNotes());
            updates.put("updatedAt", passwordItem.getUpdatedAt());

            docRef.set(updates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddEditPasswordActivity.this, "Đã cập nhật mật khẩu!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddEditPasswordActivity.this, "Lỗi khi cập nhật: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error updating password", e);
                    });
        } else {
            passwordItem.setCreatedAt(new Date());
            db.collection("users").document(userId)
                    .collection("passwords")
                    .add(passwordItem)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(AddEditPasswordActivity.this, "Đã lưu mật khẩu!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AddEditPasswordActivity.this, "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Error saving password", e);
                    });
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
