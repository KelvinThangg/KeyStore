package com.example.passwordmanager; // Hoặc package chính của bạn

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.passwordmanager.R;
import com.example.passwordmanager.model.PlatformSuggestion;

import java.util.ArrayList;
import java.util.List;

public class PlatformSuggestionAdapter extends ArrayAdapter<PlatformSuggestion> {

    private List<PlatformSuggestion> platformListFull; // Danh sách đầy đủ để filter

    public PlatformSuggestionAdapter(@NonNull Context context, @NonNull List<PlatformSuggestion> platformList) {
        super(context, 0, platformList);
        platformListFull = new ArrayList<>(platformList); // Tạo bản sao cho việc filter
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return platformFilter;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Gọi getView cho dropdown items
        return initView(position, convertView, parent);
    }

    // AutoCompleteTextView cũng gọi getView cho item được chọn hiển thị trong EditText,
    // nhưng chúng ta muốn nó chỉ hiển thị text.
    // Tuy nhiên, với MaterialComponents AutoCompleteTextView, nó thường xử lý tốt.
    // Nếu có vấn đề, bạn có thể cần override getDropDownView.

    private View initView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.dropdown_item_platform_suggestion, parent, false
            );
        }

        ImageView ivLogo = convertView.findViewById(R.id.ivPlatformLogoSuggestion);
        TextView tvName = convertView.findViewById(R.id.tvPlatformNameSuggestion);

        PlatformSuggestion currentItem = getItem(position);

        if (currentItem != null) {
            if (currentItem.getLogoResId() != 0) { // Kiểm tra nếu có logo ID hợp lệ
                ivLogo.setImageResource(currentItem.getLogoResId());
            } else {
                ivLogo.setImageResource(R.drawable.ic_default_platform); // Icon mặc định
            }
            tvName.setText(currentItem.getName());
        }
        return convertView;
    }

    private Filter platformFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            List<PlatformSuggestion> suggestions = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                suggestions.addAll(platformListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (PlatformSuggestion item : platformListFull) {
                    if (item.getName().toLowerCase().contains(filterPattern)) {
                        suggestions.add(item);
                    }
                }
            }
            results.values = suggestions;
            results.count = suggestions.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            clear();
            if (results.values != null) {
                addAll((List) results.values);
            }
            notifyDataSetChanged();
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            // Đây là những gì sẽ được hiển thị trong AutoCompleteTextView sau khi chọn
            return ((PlatformSuggestion) resultValue).getName();
        }
    };
}
