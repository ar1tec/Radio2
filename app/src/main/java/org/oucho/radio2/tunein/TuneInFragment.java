package org.oucho.radio2.tunein;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.oucho.radio2.PlayerService;
import org.oucho.radio2.R;
import org.oucho.radio2.interfaces.RadioKeys;
import org.oucho.radio2.tunein.adapters.BaseAdapter;
import org.oucho.radio2.tunein.adapters.TuneInAdapter;
import org.oucho.radio2.tunein.loaders.TuneInLoader;
import org.oucho.radio2.utils.ImageFactory;

import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class TuneInFragment extends Fragment implements RadioKeys {

    private final List<String> historique = new ArrayList<>();
    private static final String TAG = "TuneInFragment";
    private ProgressBar mProgressBar;
    private TuneInAdapter mAdapter;
    private Context mContext;


    public TuneInFragment() {
    }


    public static TuneInFragment newInstance() {
        return new TuneInFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_tunein, container, false);

        RecyclerView mRecyclerView = rootView.findViewById(R.id.recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        mAdapter = new TuneInAdapter();
        mAdapter.setOnItemClickListener(mOnItemClickListener);
        mAdapter.setOnItemLongClickListener(mOnLongClickListener);

        mRecyclerView.setAdapter(mAdapter);

        mProgressBar = rootView.findViewById(R.id.progressBar);

        Bundle args = new Bundle();
        args.putString("url", "http://opml.radiotime.com");

        load(args);

        return rootView;
    }



    private void load(Bundle args) {
        mProgressBar.setVisibility(View.VISIBLE);
        getLoaderManager().restartLoader(0, args, mLoaderCallbacks);
    }

    private final LoaderManager.LoaderCallbacks<List<String>> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<String>>() {

        @Override
        public Loader<List<String>> onCreateLoader(int id, Bundle args) {

            String url = args.getString("url");

            historique.add(url);

            Log.d(TAG, "Loader historique.size: " + historique.size());

            return new TuneInLoader(mContext, args.getString("url"));
        }

        @Override
        public void onLoadFinished(Loader<List<String>> loader, List<String> list) {
            if (list != null)
                mAdapter.setData(list);
            mProgressBar.setVisibility(View.GONE);

        }

        @Override
        public void onLoaderReset(Loader<List<String>> loader) {}
    };


    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {

            String item = mAdapter.getItem(position);
            String[] parts = item.split("\" ");

            if (item.contains("type=\"link\"")) {

                String url = null;

                for (String part : parts) {

                    if (part.contains("URL=\"")) {
                        url = part.replace("URL=\"", "");
                        Log.d(TAG, "url link : " + url);
                    }
                }

                Bundle args = new Bundle();
                args.putString("url", url);

                load(args);
            }


            if  (item.contains("type=\"audio\"")) {

                String text = parts[1];
                String name = text.replace("text=\"" , "");

                String url = null;

                for (String part : parts) {
                    if (part.contains("URL=\"")) {
                        url = part.replace("URL=\"", "");
                        Log.d(TAG, "url: " + url);
                    }
                }

                new playItem().execute(url, name, mContext);
            }
        }
    };


    private final BaseAdapter.OnItemLongClickListener mOnLongClickListener = new BaseAdapter.OnItemLongClickListener() {
        @Override
        public void onItemLongClick(int position, View view) {

            String item = mAdapter.getItem(position);
            showPopup(view, item);
        }
    };


    private void showPopup(final View view,final String item) {

        android.widget.PopupMenu mCoverPopupMenu = new android.widget.PopupMenu(mContext, view);

        mCoverPopupMenu.getMenu().add(1, 1, 0, "Ajouter à la liste");
        mCoverPopupMenu.setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item0) {

                String[] parts = item.split("\" ");

                if  (item.contains("type=\"audio\"")) {

                    String text = parts[1];
                    String name = text.replace("text=\"" , "");
                    String url = null;
                    String url_image = null;

                    Log.d(TAG, "name: " + name);

                    for (String part : parts) {

                        if (part.contains("URL=\"")) {
                            url = part.replace("URL=\"", "");
                            Log.d(TAG, "url: " + url);
                        }

                        if (part.contains("image=\"")) {
                            url_image = part.replace("image=\"", "");
                            Log.d(TAG, "image url: " + url_image);
                        }
                    }

                    new saveItem().execute(url, name, url_image, mContext);
                }

                return true;
            }
        });

        mCoverPopupMenu.show();
    }


    private static class saveItem extends AsyncTask<Object, Void, String> {

        protected String doInBackground(Object... objects) {

            String httpRequest = objects[0].toString().replace(" ", "%20");

            String url_radio;
            String name_radio = objects[1].toString();
            String url_image = objects[2].toString();
            Context context = (Context) objects[3];
            Bitmap bmImg;



            try {

                OkHttpClient getUrl = new OkHttpClient();
                Request requestUrl = new Request.Builder()
                        .url(httpRequest)
                        .build();
                Response response = getUrl.newCall(requestUrl).execute();
                url_radio = response.body().string();


                OkHttpClient getImg = new OkHttpClient();
                Request requestImg = new Request.Builder()
                        .url(url_image)
                        .build();
                Response responseImg = getImg.newCall(requestImg).execute();
                InputStream inputStream = responseImg.body().byteStream();
                bmImg = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                String img = ImageFactory.byteToString(ImageFactory.getBytes(ImageFactory.getResizedBitmap(context, bmImg)));
                String[] rustine = url_radio.split("\n"); // a tendance à doubler l'url

                Log.d(TAG, "saveItem name_radio: " + name_radio + ", url: " + rustine[0]);

                Intent radio = new Intent();
                radio.setAction(INTENT_ADD_RADIO);
                radio.putExtra("url", rustine[0]);
                radio.putExtra("name", name_radio);
                radio.putExtra("image", img);
                context.sendBroadcast(radio);

            } catch (SocketTimeoutException e) {
                Intent error = new Intent();
                error.setAction(INTENT_ERROR);
                error.putExtra("error", e);
                context.sendBroadcast(error);

                //Toast.makeText(context, "Erreur de connexion: " + e, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    //                                   <Params, Progress, Result>
    private static class playItem extends AsyncTask<Object, Void, String> {

        protected String doInBackground(Object... objects) {

            String data = null;
            String httpRequest = objects[0].toString().replace(" ", "%20");
            Context context = (Context) objects[2];

            try {

                OkHttpClient getUrl = new OkHttpClient();
                Request requestUrl = new Request.Builder()
                        .url(httpRequest)
                        .build();
                Response responseUrl = getUrl.newCall(requestUrl).execute();
                data = responseUrl.body().string();

                Log.d(TAG, "playItem url: " + data);

                String[] rustine = data.split("\n"); // a tendance à doubler l'url

                Intent player = new Intent(context, PlayerService.class);

                player.putExtra("action", ACTION_PLAY);
                player.putExtra("url", rustine[0]);
                player.putExtra("name", objects[1].toString());
                context.startService(player);

            } catch (SocketTimeoutException e) {

                Intent error = new Intent();
                error.setAction(INTENT_ERROR);
                error.putExtra("error", e);
                context.sendBroadcast(error);

                //Toast.makeText(context, "Erreur de connexion: " + e, Toast.LENGTH_SHORT).show();

            }
            catch (Exception e) {
                e.printStackTrace();
            }

            return data;
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        // Active la touche back
        if (getView() == null) {
            return;
        }

        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {

                    if (historique.size() > 1) {

                        String last = historique.get(historique.size() -2);

                        Bundle args = new Bundle();
                        args.putString("url", last);

                        // supprime les 2 derniers.
                        historique.remove(historique.size() - 1);
                        historique.remove(historique.size() - 1);

                        load(args);

                        return true;

                    } else {

                        Intent intent = new Intent();
                        intent.setAction(INTENT_TITRE);
                        intent.putExtra("titre", getResources().getString(R.string.app_name));
                        mContext.sendBroadcast(intent);

                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.setCustomAnimations(R.anim.slide_out_bottom, R.anim.slide_out_bottom);
                        ft.remove(TuneInFragment.this);
                        ft.commit();
                    }

                }
                return true;
            }
        });
    }
}
