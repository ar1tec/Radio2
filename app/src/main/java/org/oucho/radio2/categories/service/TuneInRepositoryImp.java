package org.oucho.radio2.categories.service;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.oucho.radio2.categories.model.BrowsableItem;
import org.oucho.radio2.categories.model.Category;
import org.oucho.radio2.categories.model.Genre;
import org.oucho.radio2.categories.model.TuneInItem;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TuneInRepositoryImp extends TuneInRepository {

    private static final String RENDER_JSON = "json";
    private TuneInService tuneInService;

    public TuneInRepositoryImp() {
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(getGson()))
                .baseUrl("http://opml.radiotime.com/")
                .build();
        this.tuneInService = retrofit.create(TuneInService.class);
    }

    @Override
    public Call<List<Category>> requestCategories(final TuneInResponseListener<List<Category>> tuneInResponseListener) {
        Call<List<Category>> call = tuneInService.requestCategories(RENDER_JSON);
        call.enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (call.isCanceled()) {
                    return;
                }
                if (response.isSuccessful()) {
                    tuneInResponseListener.onSuccess(response.body());

                } else {
                    tuneInResponseListener.onError("Error: " + response.code() + " \nmessage: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                tuneInResponseListener.onError(t.getMessage());
            }
        });
        return call;
    }

    @Override
    public Call<List<Category>> requestGenres(String url, final TuneInResponseListener<List<Category>> listener) {
        Uri uri = Uri.parse(url);
        TuneInService service = generateService(uri);
        Map<String, String> params = getParamsMap(uri);
        Call<List<Category>> call = service.requestGenres(uri.getPath(), params);
        call.enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (call.isCanceled()) {
                    return;
                }
                if (response.isSuccessful()) {
                    listener.onSuccess(response.body());

                } else {
                    listener.onError("Error: " + response.code() + " \nmessage: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                listener.onError(t.getMessage());
            }
        });
        return call;
    }

    @Override
    public Call<List<BrowsableItem>> requestGenreDetails(String url, final TuneInResponseListener<List<BrowsableItem>> listener) {
        Uri uri = Uri.parse(url);
        TuneInService service = generateService(uri);
        Map<String, String> map = getParamsMap(uri);
        Call<List<BrowsableItem>> call = service.requestGenreDetails(uri.getPath(), map);
        call.enqueue(new Callback<List<BrowsableItem>>() {
            @Override
            public void onResponse(Call<List<BrowsableItem>> call, Response<List<BrowsableItem>> response) {
                if (call.isCanceled()) {
                    return;
                }
                if (response.isSuccessful()) {
                    listener.onSuccess(response.body());

                } else {
                    listener.onError("Error: " + response.code() + " \nmessage: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<BrowsableItem>> call, Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                listener.onError(t.getMessage());
            }
        });
        return call;
    }

    @NonNull
    private Map<String, String> getParamsMap(Uri uri) {
        Map<String, String> map = new ArrayMap<>();
        for (String key : uri.getQueryParameterNames()) {
            map.put(key, uri.getQueryParameter(key));
        }
        map.put("render", RENDER_JSON);
        return map;
    }

    private TuneInService generateService(Uri uri) {
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(getGson()))
                .baseUrl(uri.getScheme() + "://" + uri.getHost())
                .build();
        return retrofit.create(TuneInService.class);
    }

    private Gson getGson() {
        Type listCategoryType = new TypeToken<List<Category>>() {
        }.getType();
        Type listBrowseItemType = new TypeToken<List<BrowsableItem>>() {
        }.getType();
        return new GsonBuilder()
                .registerTypeAdapter(listCategoryType, new ListCategoryAdapter())
                .registerTypeAdapter(listBrowseItemType, new ListBrowseItemAdapter())
                .create();
    }

    private class ListCategoryAdapter implements JsonDeserializer<List<Category>> {

        @Override
        public List<Category> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonArray body = json.getAsJsonObject().getAsJsonArray("body");
            List<Category> categories = new ArrayList<>(body.size());
            for (JsonElement element : body) {
                categories.add(context.<Category>deserialize(element, Category.class));
            }
            return categories;
        }
    }

    private class ListBrowseItemAdapter implements JsonDeserializer<List<BrowsableItem>> {

        @Override
        public List<BrowsableItem> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonArray body = json.getAsJsonObject().getAsJsonArray("body");
            List<BrowsableItem> categories = new ArrayList<>(body.size());
            for (JsonElement element : body) {
                if (element.getAsJsonObject().has("children")) {
                    for (JsonElement childElement : element.getAsJsonObject().getAsJsonArray("children")) {
                        if (childElement.getAsJsonObject().has("preset_id")) {
                            categories.add(context.<TuneInItem>deserialize(childElement, TuneInItem.class));
                        } else {
                            categories.add(context.<Genre>deserialize(childElement, Genre.class));
                        }
                    }
                } else {
                    categories.add(context.<Genre>deserialize(element, Genre.class));
                }
            }
            return categories;
        }
    }
}
