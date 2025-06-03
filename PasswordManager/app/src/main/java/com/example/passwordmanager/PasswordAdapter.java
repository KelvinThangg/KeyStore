package com.example.passwordmanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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
// import com.google.firebase.auth.FirebaseAuth; // Không cần thiết trực tiếp trong adapter này
// import com.google.firebase.auth.FirebaseUser;
// import com.google.firebase.firestore.FirebaseFirestore; // Không cần thiết trực tiếp trong adapter này

import java.util.ArrayList;
import java.util.List;

public class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.PasswordViewHolder> {

    private static final String TAG = "PasswordAdapter";
    private List<PasswordItem> passwordListInternal; // Đổi tên để rõ ràng đây là list nội bộ của adapter
    private final Context context;
    private OnPasswordItemInteractionListener listener;


    public interface OnPasswordItemInteractionListener {
        void onEditPassword(PasswordItem passwordItem);
        void onDeletePassword(PasswordItem passwordItem, int position);
    }


    public PasswordAdapter(Context context, List<PasswordItem> initialPasswordList, OnPasswordItemInteractionListener listener) {
        this.context = context;
        // Khởi tạo danh sách nội bộ của adapter với một bản sao của danh sách ban đầu (nếu có)
        // hoặc một danh sách rỗng. Điều này đảm bảo adapter có danh sách riêng.
        this.passwordListInternal = (initialPasswordList != null) ? new ArrayList<>(initialPasswordList) : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public PasswordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_password, parent, false);
        return new PasswordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PasswordViewHolder holder, int position) {
        PasswordItem item = passwordListInternal.get(position); // Sử dụng danh sách nội bộ
        holder.tvPlatformName.setText(item.getPlatformName());
        holder.tvUsername.setText(item.getUsername());

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
                Log.e(TAG, "Error parsing website URL for favicon: " + item.getWebsiteUrl(), e);
                holder.ivPlatformIcon.setImageResource(R.drawable.ic_default_platform);
            }
        } else {
            holder.ivPlatformIcon.setImageResource(R.drawable.ic_default_platform);
        }


        holder.btnCopyPassword.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("password", item.getPassword());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Đã sao chép mật khẩu!", Toast.LENGTH_SHORT).show();
            }
        });

        holder.btnMoreOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.btnMoreOptions);
            popup.inflate(R.menu.menu_password_item);
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
                    new AlertDialog.Builder(context)
                            .setTitle("Mật khẩu cho " + item.getPlatformName())
                            .setMessage("Tên người dùng: " + item.getUsername() + "\nMật khẩu: " + item.getPassword())
                            .setPositiveButton("OK", null)
                            .show();
                    return true;
                }
                return false;
            });
            popup.show();
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditPassword(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return passwordListInternal.size(); // Sử dụng danh sách nội bộ
    }

    /**
     * Cập nhật danh sách mật khẩu mà adapter hiển thị.
     * Adapter sẽ xóa danh sách cũ của nó và thêm tất cả các mục từ danh sách mới được cung cấp.
     * @param newPasswordList Danh sách mật khẩu mới cần hiển thị.
     */
    public void setPasswords(List<PasswordItem> newPasswordList) {
        this.passwordListInternal.clear(); // Xóa danh sách nội bộ hiện tại của adapter
        if (newPasswordList != null) {
            this.passwordListInternal.addAll(newPasswordList); // Thêm tất cả các mục từ danh sách mới
        }
        Log.d(TAG, "Adapter data set. New internal item count: " + this.passwordListInternal.size());
        notifyDataSetChanged(); // Thông báo cho RecyclerView cập nhật giao diện
    }

    // Phương thức này có thể không cần thiết nếu DashboardActivity tải lại toàn bộ danh sách
    // sau khi xóa, nhưng giữ lại nếu bạn muốn xóa trực tiếp từ adapter.
    public void removePassword(int position) {
        if (position >= 0 && position < passwordListInternal.size()) {
            passwordListInternal.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, passwordListInternal.size());
        }
    }


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
