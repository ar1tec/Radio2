package org.oucho.radio2;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;

import org.oucho.radio2.categories.CategoriesFragment;
import org.oucho.radio2.categories.GenreDetailsFragment;
import org.oucho.radio2.categories.model.BrowsableItem;


public class RadioListActivity extends AppCompatActivity implements NavigationHelper {

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tune_in);

        mContext = getApplicationContext();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final int mUIFlag = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            getWindow().getDecorView().setSystemUiVisibility(mUIFlag);
        }


        int titleColor = ContextCompat.getColor(mContext, R.color.colorAccent);
        int backgroundColor = ContextCompat.getColor(mContext, R.color.colorPrimary);
        String title = mContext.getString(R.string.app_name);

        ColorDrawable colorDrawable = new ColorDrawable(backgroundColor);

       // Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setBackgroundDrawable(colorDrawable);

        if (android.os.Build.VERSION.SDK_INT >= 24) {
            actionBar.setTitle(Html.fromHtml("<font color='" + titleColor + "'>" + title + "</font>", Html.FROM_HTML_MODE_LEGACY));
        } else {
            //noinspection deprecation
            actionBar.setTitle(Html.fromHtml("<font color='" + titleColor + "'>" + title + "</font>"));
        }

        showCategories();
    }


    @Override
    public void showCategory(BrowsableItem category) {
        String fragmentTag = GenreDetailsFragment.class.getSimpleName();
        Fragment fragment = GenreDetailsFragment.newInstance(category.getUrl());
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_content, fragment, fragmentTag)
                .addToBackStack(fragmentTag)
                .commitAllowingStateLoss();
    }

    private void showCategories() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(CategoriesFragment.class.getSimpleName());
        if (fragment == null) {
            fragment = CategoriesFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_content, fragment, CategoriesFragment.class.getSimpleName())
                    .commitAllowingStateLoss();
        }
    }
}
