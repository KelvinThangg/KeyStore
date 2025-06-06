package com.example.passwordmanager.password;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.passwordmanager.R;
import com.example.passwordmanager.dashboard.PasswordItem;
import com.example.passwordmanager.utils.EncryptionUtils;

import java.util.ArrayList;
import java.util.List;

public class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.PasswordViewHolder> {

    private static final String TAG = "PasswordAdapter";
    private List<PasswordItem> passwordListInternal;
    private final Context context;
    private OnPasswordItemInteractionListener listener;
    private EncryptionUtils encryptionUtils; // Biến để giải mã

    // Interface để giao tiếp với Activity
    public interface OnPasswordItemInteractionListener {
        void onEditPassword(PasswordItem passwordItem);
        void onDeletePassword(PasswordItem passwordItem, int position);
        // Đã loại bỏ onRequestPinToViewPassword theo yêu cầu
    }

    // Constructor
    public PasswordAdapter(Context context, List<PasswordItem> initialPasswordList, OnPasswordItemInteractionListener listener) {
        this.context = context;
        // Khởi tạo danh sách nội bộ của adapter với một bản sao của danh sách ban đầu (nếu có)
        // hoặc một danh sách rỗng. Điều này đảm bảo adapter có danh sách riêng.
        this.passwordListInternal = (initialPasswordList != null) ? new ArrayList<>(initialPasswordList) : new ArrayList<>();
        this.listener = listener;
        try {
            this.encryptionUtils = new EncryptionUtils(); // Khởi tạo EncryptionUtils
        } catch (RuntimeException e) {
            Log.e(TAG, "Không thể khởi tạo EncryptionUtils trong PasswordAdapter", e);
            Toast.makeText(context, "Lỗi hệ thống mã hóa. Một số tính năng có thể không hoạt động.", Toast.LENGTH_LONG).show();
            // encryptionUtils sẽ là null, cần kiểm tra trước khi dùng
        }
    }

    @NonNull
    @Override
    public PasswordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_password, parent, false);
        return new PasswordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PasswordViewHolder holder, int position) {
        PasswordItem item = passwordListInternal.get(position);
        holder.tvPlatformName.setText(item.getPlatformName());
        holder.tvUsername.setText(item.getUsername());

        // Tải favicon
        if (item.getWebsiteUrl() != null && !item.getWebsiteUrl().isEmpty()) {
            try {
                Uri uri = Uri.parse(item.getWebsiteUrl());
                String domain = uri.getHost();
                if (domain != null && !domain.isEmpty()) {
                    String faviconUrl = "https://www.google.com/s2/favicons?domain=" + domain + "&sz=128";
                    Glide.with(context)
                            .load(faviconUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_default_platform)
                            .error(R.drawable.ic_default_platform)
                            .into(holder.ivPlatformIcon);
                } else {
                    holder.ivPlatformIcon.setImageResource(R.drawable.ic_default_platform);
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi phân tích URL website cho favicon: " + item.getWebsiteUrl(), e);
                holder.ivPlatformIcon.setImageResource(R.drawable.ic_default_platform);
            }
        } else {
            holder.ivPlatformIcon.setImageResource(R.drawable.ic_default_platform);
        }

        // Xử lý sự kiện sao chép mật khẩu
        holder.btnCopyPassword.setOnClickListener(v -> {
            if (encryptionUtils == null) {
                Toast.makeText(context, "Lỗi giải mã. Không thể sao chép.", Toast.LENGTH_SHORT).show();
                return;
            }
            String encryptedPassword = item.getPassword();
            String decryptedPassword = encryptionUtils.decrypt(encryptedPassword);

            if (decryptedPassword != null) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("password", decryptedPassword); // Sao chép mật khẩu đã giải mã
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, "Đã sao chép mật khẩu!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Không thể giải mã mật khẩu để sao chép.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Giải mã thất bại khi sao chép cho: " + item.getPlatformName());
            }
        });

        // Xử lý sự kiện cho nút tùy chọn thêm (more options)
        holder.btnMoreOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.btnMoreOptions);
            popup.inflate(R.menu.menu_password_item); // Menu cho mỗi item
            popup.setOnMenuItemClickListener(menuItem -> {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.action_edit_password) {
                    if (listener != null) {
                        listener.onEditPassword(item);
                    }
                    return true;
                } else if (itemId == R.id.action_delete_password) {
                    if (listener != null) {
                        new AlertDialog.Builder(context)
                                .setTitle("Xóa mật khẩu")
                                .setMessage("Bạn có chắc chắn muốn xóa mật khẩu cho " + item.getPlatformName() + "?")
                                .setPositiveButton("Xóa", (dialog, which) -> {
                                    listener.onDeletePassword(item, holder.getAdapterPosition());
                                })
                                .setNegativeButton("Hủy", null)
                                .show();
                    }
                    return true;
                } else if (itemId == R.id.action_view_password) {
                    // Hiển thị mật khẩu trực tiếp (sau khi giải mã)
                    if (encryptionUtils == null) {
                        Toast.makeText(context, "Lỗi giải mã. Không thể xem.", Toast.LENGTH_SHORT).show();
                        return true; // Đã xử lý (bằng cách báo lỗi)
                    }
                    String encryptedPassword = item.getPassword();
                    String decryptedPassword = encryptionUtils.decrypt(encryptedPassword);

                    if (decryptedPassword != null) {
                        new AlertDialog.Builder(context)
                                .setTitle("Mật khẩu cho " + item.getPlatformName())
                                .setMessage("Tên người dùng: " + item.getUsername() + "\nMật khẩu: " + decryptedPassword)
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        Toast.makeText(context, "Không thể giải mã mật khẩu để xem.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Giải mã thất bại khi xem cho: " + item.getPlatformName());
                        // Hiển thị thông báo lỗi rõ ràng hơn cho người dùng
                        new AlertDialog.Builder(context)
                                .setTitle("Lỗi Giải Mã")
                                .setMessage("Không thể hiển thị mật khẩu gốc do lỗi giải mã. Mật khẩu có thể đã bị hỏng hoặc khóa mã hóa không đúng.\nDữ liệu mã hóa (phần đầu): " +
                                        (encryptedPassword != null && encryptedPassword.length() > 30 ? encryptedPassword.substring(0, 30) + "..." : "N/A"))
                                .setPositiveButton("OK", null)
                                .show();
                    }
                    return true;
                }
                return false;
            });
            popup.show();
        });

        // Xử lý sự kiện khi nhấp vào toàn bộ item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditPassword(item); // Mở màn hình sửa khi nhấp vào item
            }
        });
    }

    @Override
    public int getItemCount() {
        return passwordListInternal.size();
    }

    /**
     * Cập nhật danh sách mật khẩu mà adapter hiển thị.
     * Adapter sẽ xóa danh sách cũ của nó và thêm tất cả các mục từ danh sách mới được cung cấp.
     * @param newPasswordList Danh sách mật khẩu mới cần hiển thị.
     */
    public void setPasswords(List<PasswordItem> newPasswordList) {
        this.passwordListInternal.clear();
        if (newPasswordList != null) {
            this.passwordListInternal.addAll(newPasswordList);
        }
        Log.d(TAG, "Adapter data set. New internal item count: " + this.passwordListInternal.size());
        notifyDataSetChanged();
    }


//    public void removePassword(int position) {
//        if (position >= 0 && position < passwordListInternal.size()) {
//            passwordListInternal.remove(position);
//            notifyItemRemoved(position);
//            notifyItemRangeChanged(position, passwordListInternal.size());
//        }
//    }


    static class PasswordViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPlatformIcon;
        TextView tvPlatformName, tvUsername, tvItemPasswordProtected;
        ImageButton btnCopyPassword, btnMoreOptions;

        PasswordViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPlatformIcon = itemView.findViewById(R.id.ivPlatformIcon);
            tvPlatformName = itemView.findViewById(R.id.tvItemPlatformName);
            tvUsername = itemView.findViewById(R.id.tvItemUsername);
            tvItemPasswordProtected = itemView.findViewById(R.id.tvItemPasswordProtected);
            btnCopyPassword = itemView.findViewById(R.id.btnCopyPassword);
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
        }
    }
}
