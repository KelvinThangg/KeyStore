package com.example.passwordmanager;

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
import androidx.appcompat.widget.SearchView; // Import SearchView
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.example.passwordmanager.PasswordGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Date;


public class DashboardActivity extends AppCompatActivity implements PasswordAdapter.OnPasswordItemInteractionListener {

    private static final String TAG = "DashboardActivity";
    public static final String EXTRA_VERIFY_PIN_MODE = "EXTRA_VERIFY_PIN_MODE";

    // Constants for sorting
    private static final int SORT_PLATFORM_ASC = 1;
    private static final String SORT_PLATFORM_ASC_KEY = "platformName";
    private static final int SORT_PLATFORM_DESC = 2;
    private static final String SORT_PLATFORM_DESC_KEY = "platformName";
    private static final int SORT_DATE_DESC = 3; // Mới nhất
    private static final String SORT_DATE_KEY = "createdAt";
    private static final int SORT_DATE_ASC = 4; // Cũ nhất
    private static final int SORT_USERNAME_ASC = 5;
    private static final String SORT_USERNAME_ASC_KEY = "username";


    private static final String PREF_SORT_ORDER = "pref_sort_order";

    private TextView tvWelcome, tvPasswordCount;
    private ExtendedFloatingActionButton fabAddPassword;
    private Toolbar toolbar;
    private RecyclerView rvPasswords;
    private PasswordAdapter passwordAdapter;
    private List<PasswordItem> allPasswordItemList; // Danh sách đầy đủ từ Firestore
    private List<PasswordItem> displayedPasswordItemList; // Danh sách hiển thị (sau khi lọc)
    private MaterialCardView cardEmptyState;
    private MaterialButton btnAddPasswordEmptyState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SearchView searchView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;

    private PasswordItem mPasswordItemToView;
    private ActivityResultLauncher<Intent> mVerifyPinLauncher;

    private int currentSortOrder = SORT_DATE_DESC; // Mặc định sắp xếp theo ngày tạo mới nhất

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("PasswordManagerPrefs", MODE_PRIVATE);

        // Lấy lựa chọn sắp xếp đã lưu
        currentSortOrder = sharedPreferences.getInt(PREF_SORT_ORDER, SORT_DATE_DESC);

        allPasswordItemList = new ArrayList<>();
        displayedPasswordItemList = new ArrayList<>();

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
        setupSwipeRefresh();
        setupVerifyPinLauncher();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "DashboardActivity onResume called");
        loadUserInfo(); // Điều này sẽ gọi loadPasswordsForCurrentUser
        if (searchView != null && !searchView.isIconified()) {
            searchView.setIconified(true); // Thu gọn searchview khi resume
            searchView.setIconified(true);
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
        // Adapter sẽ làm việc với displayedPasswordItemList
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

    private void setupVerifyPinLauncher() {
        mVerifyPinLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (mPasswordItemToView != null) {
                            String decryptedPassword = mPasswordItemToView.getPassword(); // Cần giải mã nếu đã mã hóa
                            new AlertDialog.Builder(DashboardActivity.this)
                                    .setTitle("Chi tiết mật khẩu")
                                    .setMessage("Nền tảng: " + mPasswordItemToView.getPlatformName() +
                                            "\nTên người dùng: " + mPasswordItemToView.getUsername() +
                                            "\nMật khẩu: " + decryptedPassword)
                                    .setPositiveButton("OK", null)
                                    .show();
                            mPasswordItemToView = null;
                        }
                    } else {
                        Toast.makeText(DashboardActivity.this, "Mã PIN không chính xác hoặc đã hủy.", Toast.LENGTH_SHORT).show();
                        mPasswordItemToView = null;
                    }
                });
    }


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
                            if (document != null && document.exists()) {
                                // Lấy tên từ Firestore
                                String displayName = document.getString("fullName");
                                String userEmail = document.getString("email");

                                // Cập nhật UI với tên người dùng
                                if (displayName != null && !displayName.isEmpty()) {
                                    tvWelcome.setText("Xin chào, " + displayName + "!");
                                } else if (userEmail != null && !userEmail.isEmpty()) {
                                    // Nếu không có displayName, dùng phần đầu của email
                                    String nameFromEmail = userEmail.split("@")[0];
                                    tvWelcome.setText("Xin chào, " + nameFromEmail + "!");
                                } else {
                                    tvWelcome.setText("Xin chào, User!");
                                }

                                Log.d(TAG, "Đã cập nhật tên người dùng: " + displayName);
                            } else {
                                // Document không tồn tại, sử dụng email từ FirebaseAuth
                                if (email != null && !email.isEmpty()) {
                                    String nameFromEmail = email.split("@")[0];
                                    tvWelcome.setText("Xin chào, " + nameFromEmail + "!");
                                } else {
                                    tvWelcome.setText("Xin chào, User!");
                                }
                                Log.d(TAG, "Document người dùng không tồn tại, sử dụng email");
                            }
                        } else {
                            // Lỗi khi truy vấn, sử dụng email từ FirebaseAuth
                            Log.w(TAG, "Lỗi khi lấy thông tin người dùng: ", task.getException());
                            if (email != null && !email.isEmpty()) {
                                String nameFromEmail = email.split("@")[0];
                                tvWelcome.setText("Xin chào, " + nameFromEmail + "!");
                            } else {
                                tvWelcome.setText("Xin chào, User!");
                            }
                        }

                        // Sau khi cập nhật thông tin user, tải danh sách mật khẩu
                        loadPasswordsForCurrentUser();
                    });
        } else {
            // Không có user đăng nhập
            tvWelcome.setText("Xin chào, User!");
            this.allPasswordItemList.clear();
            filterAndDisplayPasswords("");
            updatePasswordCountUI(0);
            checkEmptyState();
        }
    }

    private void loadPasswordsForCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            this.allPasswordItemList.clear();
            filterAndDisplayPasswords("");
            updatePasswordCountUI(0);
            checkEmptyState();
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }
        String userId = currentUser.getUid();
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        Log.d(TAG, "Đang tải mật khẩu cho người dùng: " + userId + " với sắp xếp: " + currentSortOrder);

        Query query = db.collection("users").document(userId).collection("passwords");

        // Áp dụng sắp xếp cho truy vấn Firestore
        switch (currentSortOrder) {
            case SORT_PLATFORM_ASC:
                query = query.orderBy(SORT_PLATFORM_ASC_KEY, Query.Direction.ASCENDING);
                break;
            case SORT_PLATFORM_DESC:
                query = query.orderBy(SORT_PLATFORM_DESC_KEY, Query.Direction.DESCENDING);
                break;
            case SORT_DATE_ASC:
                query = query.orderBy(SORT_DATE_KEY, Query.Direction.ASCENDING);
                break;
            case SORT_USERNAME_ASC:
                query = query.orderBy(SORT_USERNAME_ASC_KEY, Query.Direction.ASCENDING);
                break;
            case SORT_DATE_DESC: // Mặc định và trường hợp mới nhất
            default:
                query = query.orderBy(SORT_DATE_KEY, Query.Direction.DESCENDING);
                break;
        }


        query.get().addOnCompleteListener(task -> {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            if (task.isSuccessful()) {
                allPasswordItemList.clear(); // Xóa danh sách gốc cũ
                if (task.getResult() != null) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        PasswordItem item = document.toObject(PasswordItem.class);
                        item.setId(document.getId());
                        allPasswordItemList.add(item);
                    }
                }
                Log.d(TAG, "Mật khẩu đã được tải vào danh sách gốc: " + allPasswordItemList.size() + " items");
                // Sau khi tải, áp dụng bộ lọc hiện tại (nếu có) hoặc hiển thị tất cả
                String currentQuery = searchView != null ? searchView.getQuery().toString() : "";
                filterAndDisplayPasswords(currentQuery);

            } else {
                Log.w(TAG, "Lỗi khi lấy mật khẩu: ", task.getException());
                Toast.makeText(DashboardActivity.this, "Lỗi tải mật khẩu.", Toast.LENGTH_SHORT).show();
                allPasswordItemList.clear();
                filterAndDisplayPasswords("");
            }
            // updatePasswordCountUI và checkEmptyState sẽ được gọi trong filterAndDisplayPasswords
        });
    }

    private void filterAndDisplayPasswords(String queryText) {
        displayedPasswordItemList.clear();
        if (queryText.isEmpty()) {
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

        // Sắp xếp client-side nếu Firestore không hỗ trợ nhiều orderBy phức tạp hoặc nếu cần
        // Tuy nhiên, với các trường hợp sắp xếp đơn giản, Firestore orderBy là đủ.
        // Nếu cần sắp xếp client-side phức tạp hơn sau khi lọc:
        // applyClientSideSort(displayedPasswordItemList);

        if (passwordAdapter != null) {
            passwordAdapter.setPasswords(displayedPasswordItemList); // Cập nhật adapter với danh sách đã lọc (và sắp xếp)
        }
        updatePasswordCountUI(displayedPasswordItemList.size());
        checkEmptyState(); // Kiểm tra trạng thái trống dựa trên danh sách hiển thị
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
        // ... (code cũ) ...
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
        // ... (code cũ) ...
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để tạo mật khẩu.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        String generatedPassword = PasswordGenerator.generatePassword(16, true, true, true);
        PasswordItem newPasswordItem = new PasswordItem("Your new password", "Your new password", generatedPassword, "", "", userId);
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
                @Override
                public boolean onQueryTextSubmit(String query) {
                    // Người dùng nhấn enter hoặc nút tìm kiếm trên bàn phím
                    filterAndDisplayPasswords(query);
                    return true; // Đã xử lý
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    // Lọc ngay khi người dùng gõ
                    filterAndDisplayPasswords(newText);
                    return true; // Đã xử lý
                }
            });
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem item) { return true; } // Cho phép mở rộng

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    filterAndDisplayPasswords(""); // Xóa bộ lọc khi đóng SearchView
                    return true; // Cho phép thu gọn
                }
            });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        int newSortOrder = currentSortOrder;

        if (id == R.id.action_sort_platform_asc) {
            newSortOrder = SORT_PLATFORM_ASC;
        } else if (id == R.id.action_sort_platform_desc) {
            newSortOrder = SORT_PLATFORM_DESC;
        } else if (id == R.id.action_sort_date_desc) {
            newSortOrder = SORT_DATE_DESC;
        } else if (id == R.id.action_sort_date_asc) {
            newSortOrder = SORT_DATE_ASC;
        } else if (id == R.id.action_sort_username_asc) {
            newSortOrder = SORT_USERNAME_ASC;
        } else if (id == R.id.action_refresh_passwords) {
            loadPasswordsForCurrentUser();
            return true;
        } else if (id == R.id.action_settings) {
            Toast.makeText(this, "Cài đặt (chưa triển khai)", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_change_pin) {
            showChangePinDialog();
            return true;
        } else if (id == R.id.action_logout) {
            showLogoutDialog();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }

        if (newSortOrder != currentSortOrder) {
            currentSortOrder = newSortOrder;
            sharedPreferences.edit().putInt(PREF_SORT_ORDER, currentSortOrder).apply();
            loadPasswordsForCurrentUser(); // Tải lại dữ liệu với thứ tự sắp xếp mới
        }
        return true;
    }

    // ... (các phương thức showChangePinDialog, showLogoutDialog, logoutUser, onBackPressed, onEditPassword, onDeletePassword, onRequestPinToViewPassword như cũ) ...
    private void showChangePinDialog() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đổi mã PIN.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Đổi mã PIN")
                .setMessage("Bạn có muốn đổi mã PIN không?")
                .setPositiveButton("Có", (dialog, which) -> {
                    Intent intent = new Intent(this, PinActivity.class); // Hoặc CreatePinActivity
                    intent.putExtra("isChangingPin", true);
                    startActivity(intent);
                })
                .setNegativeButton("Không", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> logoutUser())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void logoutUser() {
        mAuth.signOut();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.putBoolean("hasPin", false);
        editor.remove("userPin");
        editor.remove("userEmail");
        editor.remove("userId");
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
            searchView.setIconified(true); // Thu gọn SearchView trước nếu đang mở
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Thoát ứng dụng")
                    .setMessage("Bạn có muốn thoát ứng dụng không?")
                    .setPositiveButton("Thoát", (dialog, which) -> finishAffinity())
                    .setNegativeButton("Hủy", null)
                    .show();
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
        if (currentUser == null) { return; }
        String userId = currentUser.getUid();
        if (passwordItem.getId() == null || passwordItem.getId().isEmpty()) { return; }
        db.collection("users").document(userId)
                .collection("passwords").document(passwordItem.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(DashboardActivity.this, "Đã xóa: " + passwordItem.getPlatformName(), Toast.LENGTH_SHORT).show();
                    loadPasswordsForCurrentUser();
                })
                .addOnFailureListener(e -> Toast.makeText(DashboardActivity.this, "Lỗi khi xóa", Toast.LENGTH_SHORT).show());
    }

   // @Override
    public void onRequestPinToViewPassword(PasswordItem passwordItem) {
        boolean hasPin = sharedPreferences.getBoolean("hasPin", false);
        if (!hasPin) {
            Toast.makeText(this, "Bạn cần tạo mã PIN trước.", Toast.LENGTH_LONG).show();
            return;
        }
        this.mPasswordItemToView = passwordItem;
        Intent intent = new Intent(this, PinActivity.class); // Hoặc CreatePinActivity
        intent.putExtra(EXTRA_VERIFY_PIN_MODE, true);
        mVerifyPinLauncher.launch(intent);
    }
}
