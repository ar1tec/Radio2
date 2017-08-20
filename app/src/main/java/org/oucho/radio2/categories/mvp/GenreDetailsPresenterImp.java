package org.oucho.radio2.categories.mvp;

import android.text.TextUtils;

import org.oucho.radio2.categories.model.BrowsableItem;
import org.oucho.radio2.categories.service.TuneInRepository;

import java.util.List;

public class GenreDetailsPresenterImp implements GenreDetailsContract.Presenter, TuneInRepository.TuneInResponseListener<List<BrowsableItem>> {

    private GenreDetailsContract.View view;
    private TuneInRepository tuneInRepository;

    public GenreDetailsPresenterImp(GenreDetailsContract.View view, TuneInRepository tuneInRepository) {
        this.view = view;
        this.tuneInRepository = tuneInRepository;
    }

    @Override
    public void init(String url) {
        view.showProgress();
        if (!TextUtils.isEmpty(url)) {
            tuneInRepository.requestGenreDetails(url, this);
        } else {
            view.showError("Url is empty");
            view.hideProgress();
        }
    }


    @Override
    public void onSuccess(List<BrowsableItem> response) {
        if (response != null) {
            view.showDetails(response);
        }
        view.hideProgress();
    }

    @Override
    public void onError(String errorMessage) {
        view.showError(errorMessage);
        view.hideProgress();
    }
}
