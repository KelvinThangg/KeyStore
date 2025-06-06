package com.example.passwordmanager.dashboard; // Package name từ file bạn cung cấp

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

// Import theo file bạn cung cấp và các file cần thiết khác
import com.example.passwordmanager.R;
import com.example.passwordmanager.login.CreatePinActivity; // Từ file bạn cung cấp
import com.example.passwordmanager.login.LoginActivity;     // Từ file bạn cung cấp
import com.example.passwordmanager.login.PinActivity;       // Từ file bạn cung cấp
import com.example.passwordmanager.password.AddEditPasswordActivity; // Từ file bạn cung cấp
import com.example.passwordmanager.password.PasswordAdapter;         // Từ file bạn cung cấp
import com.example.passwordmanager.utils.EncryptionUtils;           // Giả định package
import com.example.passwordmanager.password.PasswordGenerator;         // Giả định package

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class DashboardActivity extends AppCompatActivity implements PasswordAdapter.OnPasswordItemInteractionListener {

    private static final String TAG = "DashboardActivity";
    public static final String EXTRA_VERIFY_PIN_MODE = "EXTRA_VERIFY_PIN_MODE";

    // Constants for sorting
    private static final int SORT_PLATFORM_ASC = 1;
    private static final String SORT_PLATFORM_ASC_KEY = "platformName";
    private static final int SORT_PLATFORM_DESC = 2;
    // private static final String SORT_PLATFORM_DESC_KEY = "platformName"; // Dùng chung key
    private static final int SORT_DATE_DESC = 3; // Mới nhất
    private static final String SORT_DATE_KEY = "createdAt"; // Sử dụng PasswordItem.FIELD_CREATED_AT nếu có
    private static final int SORT_DATE_ASC = 4; // Cũ nhất
    private static final int SORT_USERNAME_ASC = 5;
    private static final String SORT_USERNAME_ASC_KEY = "username";


    private static final String PREF_SORT_ORDER = "pref_sort_order";

    private TextView tvWelcome, tvPasswordCount;
    private ExtendedFloatingActionButton fabAddPassword;
    private Toolbar toolbar;
    private RecyclerView rvPasswords;
    private PasswordAdapter passwordAdapter;
    private List<PasswordItem> allPasswordItemList;
    private List<PasswordItem> displayedPasswordItemList;
    private MaterialCardView cardEmptyState;
    private MaterialButton btnAddPasswordEmptyState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SearchView searchView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;

    private PasswordItem mPasswordItemToView;
    private ActivityResultLauncher<Intent> mVerifyPinLauncher;
    private EncryptionUtils encryptionUtils;

    private int currentSortOrder = SORT_DATE_DESC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        try {
            encryptionUtils = new EncryptionUtils();
        } catch (RuntimeException e) {
            Toast.makeText(this, "Lỗi bảo mật khởi tạo. Một số tính năng có thể không hoạt động.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Không thể khởi tạo EncryptionUtils trong Dashboard", e);
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("PasswordManagerPrefs", MODE_PRIVATE);
        currentSortOrder = sharedPreferences.getInt(PREF_SORT_ORDER, SORT_DATE_DESC);

        allPasswordItemList = new ArrayList<>();
        displayedPasswordItemList = new ArrayList<>();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
        setupSwipeRefresh();
        //setupVerifyPinLauncher();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "DashboardActivity onResume called");
        loadUserInfo();
        if (searchView != null && !searchView.isIconified()) {
            searchView.setIconified(true);
            searchView.setIconified(true); // Gọi 2 lần để đảm bảo nó thu gọn
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvPasswordCount = findViewById(R.id.tvPasswordCount);
        fabAddPassword = findViewById(R.id.fabAddPassword);
        rvPasswords = findViewById(R.id.rvPasswords);
        cardEmptyState = findViewById(R.id.cardEmptyState);
        btnAddPasswordEmptyState = findViewById(R.id.btnAddPasswordEmptyState);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Trình quản lý mật khẩu");
        }
    }

    private void setupRecyclerView() {
        passwordAdapter = new PasswordAdapter(this, displayedPasswordItemList, this);
        rvPasswords.setLayoutManager(new LinearLayoutManager(this));
        rvPasswords.setAdapter(passwordAdapter);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadPasswordsForCurrentUser);
            swipeRefreshLayout.setColorSchemeResources(R.color.primary_color,
                    R.color.holo_green_light,
                    R.color.holo_orange_light,
                    R.color.holo_red_light);
        }
    }

//    private void setupVerifyPinLauncher() {
//        mVerifyPinLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    if (result.getResultCode() == Activity.RESULT_OK) {
//                        if (mPasswordItemToView != null) {
//                            if (encryptionUtils == null) {
//                                Toast.makeText(this, "Lỗi giải mã. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
//                                mPasswordItemToView = null; return;
//                            }
//                            String encryptedPassword = mPasswordItemToView.getPassword();
//                            String decryptedPassword = encryptionUtils.decrypt(encryptedPassword);
//
//                            if (decryptedPassword != null) {
//                                new AlertDialog.Builder(DashboardActivity.this)
//                                        .setTitle("Chi tiết mật khẩu")
//                                        .setMessage("Nền tảng: " + mPasswordItemToView.getPlatformName() +
//                                                "\nTên người dùng: " + mPasswordItemToView.getUsername() +
//                                                "\nMật khẩu: " + decryptedPassword)
//                                        .setPositiveButton("OK", null)
//                                        .show();
//                            } else {
//                                Toast.makeText(DashboardActivity.this, "Không thể giải mã mật khẩu.", Toast.LENGTH_SHORT).show();
//                                Log.e(TAG, "Giải mã thất bại cho: " + mPasswordItemToView.getPlatformName());
//                                new AlertDialog.Builder(DashboardActivity.this)
//                                        .setTitle("Lỗi giải mã")
//                                        .setMessage("Không thể hiển thị mật khẩu gốc.\nDữ liệu đã mã hóa (phần đầu): " +
//                                                (encryptedPassword != null && encryptedPassword.length() > 30 ? encryptedPassword.substring(0,30)+"..." : "N/A"))
//                                        .setPositiveButton("OK", null)
//                                        .show();
//                            }
//                            mPasswordItemToView = null;
//                        }
//                    } else {
//                        Toast.makeText(DashboardActivity.this, "Mã PIN không chính xác hoặc đã hủy.", Toast.LENGTH_SHORT).show();
//                        mPasswordItemToView = null;
//                    }
//                });
//    }

    private void loadUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            String uid = currentUser.getUid();
            Log.d(TAG, "Đang tải thông tin người dùng cho UID: " + uid);
            db.collection("users").document(uid).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            String displayNameToShow = "User"; // Mặc định
                            if (document != null && document.exists()) {
                                String firestoreFullName = document.getString("fullName");
                                String firestoreEmail = document.getString("email"); // Email từ Firestore (nếu có)

                                if (firestoreFullName != null && !firestoreFullName.isEmpty()) {
                                    displayNameToShow = firestoreFullName;
                                } else if (firestoreEmail != null && !firestoreEmail.isEmpty()) {
                                    displayNameToShow = firestoreEmail.split("@")[0];
                                } else if (email != null && !email.isEmpty()){ // Fallback email từ Auth
                                    displayNameToShow = email.split("@")[0];
                                }
                                Log.d(TAG, "Lấy tên từ Firestore: " + displayNameToShow);
                            } else {
                                Log.d(TAG, "Document người dùng không tồn tại, dùng email từ Auth");
                                if (email != null && !email.isEmpty()){
                                    displayNameToShow = email.split("@")[0];
                                }
                            }
                            tvWelcome.setText("Xin chào, " + displayNameToShow + "!");
                        } else {
                            Log.w(TAG, "Lỗi khi lấy thông tin người dùng: ", task.getException());
                            String displayNameToShow = "User";
                            if (email != null && !email.isEmpty()){
                                displayNameToShow = email.split("@")[0];
                            }
                            tvWelcome.setText("Xin chào, " + displayNameToShow + "!");
                        }
                        loadPasswordsForCurrentUser();
                    });
        } else {
            tvWelcome.setText("Xin chào, Khách!");
            this.allPasswordItemList.clear();
            filterAndDisplayPasswords("");
        }
    }

    private void loadPasswordsForCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            this.allPasswordItemList.clear();
            filterAndDisplayPasswords("");
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) swipeRefreshLayout.setRefreshing(false);
            return;
        }
        String userId = currentUser.getUid();
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        Log.d(TAG, "Đang tải mật khẩu cho người dùng: " + userId + " với sắp xếp: " + currentSortOrder);
        Query query = db.collection("users").document(userId).collection("passwords");
        String sortKey;
        Query.Direction direction;

        switch (currentSortOrder) {
            case SORT_PLATFORM_ASC:
                sortKey = SORT_PLATFORM_ASC_KEY; direction = Query.Direction.ASCENDING; break;
            case SORT_PLATFORM_DESC:
                sortKey = SORT_PLATFORM_ASC_KEY; direction = Query.Direction.DESCENDING; break; // Dùng chung key
            case SORT_DATE_ASC:
                sortKey = SORT_DATE_KEY; direction = Query.Direction.ASCENDING; break;
            case SORT_USERNAME_ASC:
                sortKey = SORT_USERNAME_ASC_KEY; direction = Query.Direction.ASCENDING; break;
            case SORT_DATE_DESC:
            default:
                sortKey = SORT_DATE_KEY; direction = Query.Direction.DESCENDING; break;
        }
        query = query.orderBy(sortKey, direction);

        query.get().addOnCompleteListener(task -> {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            if (task.isSuccessful()) {
                allPasswordItemList.clear();
                if (task.getResult() != null) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        PasswordItem item = document.toObject(PasswordItem.class);
                        item.setId(document.getId());
                        allPasswordItemList.add(item);
                    }
                }
                Log.d(TAG, "Mật khẩu đã được tải vào danh sách gốc: " + allPasswordItemList.size() + " items");
                filterAndDisplayPasswords(searchView != null ? searchView.getQuery().toString() : "");
            } else {
                Log.w(TAG, "Lỗi khi lấy mật khẩu: ", task.getException());
                Toast.makeText(DashboardActivity.this, "Lỗi tải mật khẩu.", Toast.LENGTH_SHORT).show();
                allPasswordItemList.clear();
                filterAndDisplayPasswords("");
            }
        });
    }


    private void filterAndDisplayPasswords(String queryText) {
        displayedPasswordItemList.clear();
        if (queryText == null || queryText.isEmpty()) { // Kiểm tra null cho queryText
            displayedPasswordItemList.addAll(allPasswordItemList);
        } else {
            String filterPattern = queryText.toLowerCase().trim();
            for (PasswordItem item : allPasswordItemList) {
                if ((item.getPlatformName() != null && item.getPlatformName().toLowerCase().contains(filterPattern)) ||
                        (item.getUsername() != null && item.getUsername().toLowerCase().contains(filterPattern))) {
                    displayedPasswordItemList.add(item);
                }
            }
        }
        if (passwordAdapter != null) {
            passwordAdapter.setPasswords(displayedPasswordItemList);
        }
        updatePasswordCountUI(displayedPasswordItemList.size());
        checkEmptyState();
    }

    private void updatePasswordCountUI(int count) {
        tvPasswordCount.setText("Bạn đã lưu " + count + " mật khẩu");
    }

    private void checkEmptyState() {
        if (rvPasswords == null || cardEmptyState == null) return;
        if (displayedPasswordItemList.isEmpty()) {
            rvPasswords.setVisibility(View.GONE);
            cardEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvPasswords.setVisibility(View.VISIBLE);
            cardEmptyState.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        View.OnClickListener addPasswordClickListener = v -> {
            Intent intent = new Intent(DashboardActivity.this, AddEditPasswordActivity.class);
            startActivity(intent);
        };
        if (fabAddPassword != null) fabAddPassword.setOnClickListener(addPasswordClickListener);
        if (btnAddPasswordEmptyState != null) btnAddPasswordEmptyState.setOnClickListener(addPasswordClickListener);
        MaterialCardView cardAddQuick = findViewById(R.id.cardAddPasswordQuickAction);
        if (cardAddQuick != null) cardAddQuick.setOnClickListener(addPasswordClickListener);
        MaterialCardView cardGenerateQuick = findViewById(R.id.cardGeneratePasswordQuickAction);
        if (cardGenerateQuick != null) cardGenerateQuick.setOnClickListener(v -> generateAndSaveNewPassword());
    }

    private void generateAndSaveNewPassword() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || encryptionUtils == null) {
            Toast.makeText(this, "Không thể tạo mật khẩu nhanh lúc này.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        String plainGeneratedPassword = PasswordGenerator.generatePassword(16, true, true, true);
        String encryptedGeneratedPassword = encryptionUtils.encrypt(plainGeneratedPassword);

        if (encryptedGeneratedPassword == null) {
            Toast.makeText(this, "Lỗi mã hóa mật khẩu nhanh.", Toast.LENGTH_SHORT).show();
            return;
        }
        PasswordItem newPasswordItem = new PasswordItem("Your new password", "Your new password", encryptedGeneratedPassword, "", "", userId);
        // Constructor của PasswordItem tự động đặt createdAt và updatedAt bằng new Date()
        db.collection("users").document(userId).collection("passwords").add(newPasswordItem)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(DashboardActivity.this, "Đã tạo và lưu mật khẩu mới!", Toast.LENGTH_SHORT).show();
                    loadPasswordsForCurrentUser();
                })
                .addOnFailureListener(e -> Toast.makeText(DashboardActivity.this, "Lỗi khi lưu mật khẩu mới: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search_password);
        searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint("Tìm theo nền tảng, username...");
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String query) { filterAndDisplayPasswords(query); return true; }
                @Override public boolean onQueryTextChange(String newText) { filterAndDisplayPasswords(newText); return true; }
            });
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override public boolean onMenuItemActionExpand(MenuItem item) { return true; }
                @Override public boolean onMenuItemActionCollapse(MenuItem item) { filterAndDisplayPasswords(""); return true; }
            });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        int newSortOrder = currentSortOrder;

        if (id == R.id.action_sort_platform_asc) newSortOrder = SORT_PLATFORM_ASC;
        else if (id == R.id.action_sort_platform_desc) newSortOrder = SORT_PLATFORM_DESC;
        else if (id == R.id.action_sort_date_desc) newSortOrder = SORT_DATE_DESC;
        else if (id == R.id.action_sort_date_asc) newSortOrder = SORT_DATE_ASC;
        else if (id == R.id.action_sort_username_asc) newSortOrder = SORT_USERNAME_ASC;
        else if (id == R.id.action_refresh_passwords) { loadPasswordsForCurrentUser(); return true; }
        else if (id == R.id.action_settings) { Toast.makeText(this, "Cài đặt (chưa triển khai)", Toast.LENGTH_SHORT).show(); return true; }
        else if (id == R.id.action_change_pin) { showChangePinDialog(); return true; }
        else if (id == R.id.action_logout) { showLogoutDialog(); return true; }
        else return super.onOptionsItemSelected(item);

        if (newSortOrder != currentSortOrder) {
            currentSortOrder = newSortOrder;
            sharedPreferences.edit().putInt(PREF_SORT_ORDER, currentSortOrder).apply();
            loadPasswordsForCurrentUser();
        }
        return true;
    }

    private void showChangePinDialog() {
        if (mAuth.getCurrentUser() == null) { Toast.makeText(this, "Vui lòng đăng nhập để đổi mã PIN.", Toast.LENGTH_SHORT).show(); return; }
        new AlertDialog.Builder(this)
                .setTitle("Đổi mã PIN")
                .setMessage("Bạn có muốn đổi mã PIN không?")
                .setPositiveButton("Có", (dialog, which) -> {
                    Intent intent = new Intent(this, PinActivity.class); // Sử dụng PinActivity
                    intent.putExtra("isChangingPin", true);
                    startActivity(intent);
                })
                .setNegativeButton("Không", null).show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> logoutUser())
                .setNegativeButton("Hủy", null).show();
    }

    private void logoutUser() {
        mAuth.signOut();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.putBoolean("hasPin", false);
        editor.remove("userPin"); editor.remove("userEmail"); editor.remove("userId");
        editor.apply();
        Toast.makeText(this, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (searchView != null && !searchView.isIconified()) {
            searchView.setIconified(true);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Thoát ứng dụng")
                    .setMessage("Bạn có muốn thoát ứng dụng không?")
                    .setPositiveButton("Thoát", (dialog, which) -> finishAffinity())
                    .setNegativeButton("Hủy", null).show();
        }
    }

    @Override
    public void onEditPassword(PasswordItem passwordItem) {
        Intent intent = new Intent(this, AddEditPasswordActivity.class);
        intent.putExtra(AddEditPasswordActivity.EXTRA_PASSWORD_ID, passwordItem.getId());
        startActivity(intent);
    }

    @Override
    public void onDeletePassword(PasswordItem passwordItem, int position) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) { Toast.makeText(this, "Lỗi xác thực.", Toast.LENGTH_SHORT).show(); return; }
        String userId = currentUser.getUid();
        if (passwordItem.getId() == null || passwordItem.getId().isEmpty()) { Toast.makeText(this, "Lỗi ID mật khẩu.", Toast.LENGTH_SHORT).show(); return; }
        db.collection("users").document(userId).collection("passwords").document(passwordItem.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(DashboardActivity.this, "Đã xóa: " + passwordItem.getPlatformName(), Toast.LENGTH_SHORT).show();
                    loadPasswordsForCurrentUser();
                })
                .addOnFailureListener(e -> Toast.makeText(DashboardActivity.this, "Lỗi khi xóa: "+ e.getMessage(), Toast.LENGTH_SHORT).show());
    }

//    @Override
//    public void onRequestPinToViewPassword(PasswordItem passwordItem) {
//        boolean hasPin = sharedPreferences.getBoolean("hasPin", false);
//        if (!hasPin) {
//            Toast.makeText(this, "Bạn cần tạo mã PIN trước khi xem mật khẩu.", Toast.LENGTH_LONG).show();
//            return;
//        }
//        if (encryptionUtils == null) {
//            Toast.makeText(this, "Lỗi hệ thống mã hóa. Không thể xem mật khẩu.", Toast.LENGTH_LONG).show();
//            return;
//        }
//        this.mPasswordItemToView = passwordItem;
//        Intent intent = new Intent(this, PinActivity.class);
//        intent.putExtra(EXTRA_VERIFY_PIN_MODE, true);
//        mVerifyPinLauncher.launch(intent);
//    }
}
