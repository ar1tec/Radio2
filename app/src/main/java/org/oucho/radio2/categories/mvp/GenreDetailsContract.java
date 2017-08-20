package org.oucho.radio2.categories.mvp;


import org.oucho.radio2.categories.model.BrowsableItem;

import java.util.List;

public interface GenreDetailsContract {
    interface View extends CommonView {

        void showDetails(List<BrowsableItem> browsableItems);
    }

    interface Presenter {
        void init(String url);
    }
}
