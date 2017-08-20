package org.oucho.radio2.categories.mvp;


import org.oucho.radio2.categories.model.Category;

import java.util.List;

public interface CategoriesContract {
    interface View extends CommonView {
        void showItems(List<Category> categories);
    }

    interface Presenter {
        void init();
    }
}
