package org.oucho.radio2.categories.service;

import org.oucho.radio2.categories.model.BrowsableItem;
import org.oucho.radio2.categories.model.Category;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

interface TuneInService {

    @GET("/")
    Call<List<Category>> requestCategories(@Query("render") String render);

    @GET("/{browse}")
    Call<List<Category>> requestGenres(@Path("browse") String browse, @QueryMap Map<String, String> params);

    @GET("/{browse}")
    Call<List<BrowsableItem>> requestGenreDetails(@Path("browse") String path, @QueryMap Map<String, String> params);
}
