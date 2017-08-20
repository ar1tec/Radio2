package org.oucho.radio2.categories;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.oucho.radio2.NavigationHelper;
import org.oucho.radio2.R;
import org.oucho.radio2.categories.adapter.CategoriesAdapter;
import org.oucho.radio2.categories.model.Category;
import org.oucho.radio2.categories.mvp.CategoriesContract;
import org.oucho.radio2.categories.mvp.CategoriesPresenterImp;
import org.oucho.radio2.categories.service.TuneInRepository;

import java.util.List;


public class CategoriesFragment extends Fragment implements CategoriesContract.View {

    private RecyclerView categoriesRecyclerView;
    private ProgressBar progressBar;
    private TextView errorTextView;

    private NavigationHelper listener;
    private CategoriesContract.Presenter presenter;

    public CategoriesFragment() {
    }

    public static CategoriesFragment newInstance() {
        return new CategoriesFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = new CategoriesPresenterImp(this, TuneInRepository.getInstance());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_list, container, false);

        categoriesRecyclerView = (RecyclerView) view.findViewById(R.id.categories_recycler_view);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        errorTextView = (TextView) view.findViewById(R.id.error_textview);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        categoriesRecyclerView.setAdapter(new CategoriesAdapter(null, listener));
        presenter.init();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof NavigationHelper) {
            listener = (NavigationHelper) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement NavigationHelper");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void showItems(List<Category> categories) {
        RecyclerView.Adapter adapter = categoriesRecyclerView.getAdapter();
        if (adapter == null) {
            categoriesRecyclerView.setAdapter(new CategoriesAdapter(categories,listener));
        } else {
            ((CategoriesAdapter)adapter).setItems(categories);
        }
    }

    @Override
    public void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
        errorTextView.setVisibility(View.GONE);
        categoriesRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void hideProgress() {
        progressBar.setVisibility(View.GONE);
        errorTextView.setVisibility(View.GONE);
        categoriesRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void showError(String errorMessage) {
        errorTextView.setVisibility(View.VISIBLE);
    }
}
