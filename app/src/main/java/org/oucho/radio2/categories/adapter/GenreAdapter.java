package org.oucho.radio2.categories.adapter;

import android.view.View;

import org.oucho.radio2.NavigationHelper;
import org.oucho.radio2.categories.model.Category;

import java.util.List;

public class GenreAdapter extends CategoriesAdapter {
    public GenreAdapter(List<Category> items, NavigationHelper listener) {
        super(items, listener);
    }

    @Override
    public void onClick(View v) {
        if (null != listener && recyclerView != null) {
            ViewHolder holder = (ViewHolder) recyclerView.getChildViewHolder(v);
            listener.showCategory(holder.item);
        }
    }
}
