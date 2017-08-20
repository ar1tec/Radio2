package org.oucho.radio2.categories.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.oucho.radio2.NavigationHelper;
import org.oucho.radio2.R;
import org.oucho.radio2.categories.model.Category;

import java.util.List;


public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.ViewHolder> implements View.OnClickListener {

    final NavigationHelper listener;
    private List<Category> categories;
    RecyclerView recyclerView;

    public CategoriesAdapter(List<Category> items, NavigationHelper listener) {
        categories = items;
        this.listener = listener;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_category, parent, false);
        view.setOnClickListener(this);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.item = categories.get(position);
        holder.title.setText(categories.get(position).getText());
    }

    @Override
    public int getItemCount() {
        return categories == null ? 0 : categories.size();
    }

    public void setItems(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        if (null != listener && recyclerView != null) {
            ViewHolder holder = (ViewHolder) recyclerView.getChildViewHolder(v);
            listener.showCategory(holder.item);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        Category item;

        ViewHolder(View view) {
            super(view);

            title = (TextView) view.findViewById(R.id.item_title);
        }

    }
}
