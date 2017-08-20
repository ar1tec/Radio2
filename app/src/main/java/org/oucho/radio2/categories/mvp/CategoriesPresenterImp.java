package org.oucho.radio2.categories.mvp;

import org.oucho.radio2.categories.model.Category;
import org.oucho.radio2.categories.service.TuneInRepository;

import java.util.List;

public class CategoriesPresenterImp implements CategoriesContract.Presenter, TuneInRepository.TuneInResponseListener<List<Category>> {
    private CategoriesContract.View view;
    private final TuneInRepository tuneInRepository;

    public CategoriesPresenterImp(CategoriesContract.View view, TuneInRepository tuneInRepository) {
        this.view = view;
        this.tuneInRepository = tuneInRepository;
    }

    @Override
    public void init() {
        view.showProgress();
        tuneInRepository.requestCategories(this);
    }

    @Override
    public void onSuccess(List<Category> response) {
        if (response != null) {
            view.showItems(response);
        }
        view.hideProgress();
    }

    @Override
    public void onError(String errorMessage) {
        view.showError(errorMessage);
        view.hideProgress();
    }
}
