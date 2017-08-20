package org.oucho.radio2.categories.mvp;

public interface CommonView {
    void showProgress();

    void hideProgress();

    void showError(String error);
}
