package ve.guiafarmaceutica.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import ve.guiafarmaceutica.app.R;
import ve.guiafarmaceutica.app.data.CategoryItem;

public class CategoryItemAdapter extends RecyclerView.Adapter<CategoryItemAdapter.ViewHolder> {

    public interface OnCategoryItemClickListener {
        void onItemClick(CategoryItem item);
    }

    private final List<CategoryItem> items = new ArrayList<>();
    private final OnCategoryItemClickListener listener;

    public CategoryItemAdapter(OnCategoryItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<CategoryItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle;

        ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.text_group_title);
            subtitle = view.findViewById(R.id.text_group_subtitle);
        }

        void bind(CategoryItem item, OnCategoryItemClickListener listener) {
            title.setText(item.titulo);
            subtitle.setText(item.subtitulo);
            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
