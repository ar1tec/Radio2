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
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


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


        RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        mAdapter = new TuneInAdapter();

        mAdapter.setOnItemClickListener(mOnItemClickListener);
        mAdapter.setOnItemLongClickListener(mOnLongClickListener);

        mRecyclerView.setAdapter(mAdapter);

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

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
        public void onLoaderReset(Loader<List<String>> loader) {
            //  Auto-generated method stub
        }
    };


    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {

            String item = mAdapter.getItem(position);
            String[] parts = item.split("\" ");

            String url_image = null;

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

                    if (part.contains("image=\"")) {

                        url_image = part.replace("image=\"", "");
                        Log.d(TAG, "image url: " + url_image);
                    }

                }

                new playItem().execute(url, name, url_image, mContext);
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


    private static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }


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
                URL url = new URL(httpRequest);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.connect();

                InputStream stream = conn.getInputStream();

                url_radio = convertStreamToString(stream);

                stream.close();

                URL url1 = new URL(url_image);
                HttpURLConnection conn1 = (HttpURLConnection) url1.openConnection();
                conn1.setDoInput(true);
                conn1.connect();

                InputStream is = conn1.getInputStream();

                bmImg = BitmapFactory.decodeStream(is);

                is.close();

                String img = ImageFactory.byteToString(ImageFactory.getBytes(ImageFactory.getResizedBitmap(context, bmImg)));

                Intent radio = new Intent();
                radio.setAction("org.oucho.radio2.ADD_RADIO");
                radio.putExtra("url", url_radio);
                radio.putExtra("name", name_radio);
                radio.putExtra("image", img);
                context.sendBroadcast(radio);

            } catch (SocketTimeoutException e) {
                Toast.makeText(context, "Erreur de connexion: " + e, Toast.LENGTH_SHORT).show();
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

            String img_URL = objects[2].toString();

            Log.d(TAG, "url img = " + img_URL);


            Context context = (Context) objects[3];

            Bitmap bmImg;

            try {
                URL url = new URL(httpRequest);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.connect();

                InputStream stream = conn.getInputStream();

                data = convertStreamToString(stream);


                URL url1 = new URL(img_URL);
                HttpURLConnection conn1 = (HttpURLConnection) url1.openConnection();
                conn1.setDoInput(true);
                conn1.connect();

                InputStream isImg = conn1.getInputStream();

                bmImg = BitmapFactory.decodeStream(isImg);

                String img = ImageFactory.byteToString(ImageFactory.getBytes(ImageFactory.getResizedBitmap(context, bmImg)));

                Intent player = new Intent(context, PlayerService.class);

                player.putExtra("action", ACTION_PLAY);
                player.putExtra("url", data);
                player.putExtra("name", objects[1].toString());
                context.startService(player);

                Intent intent = new Intent();
                intent.setAction(INTENT_UPDATENOTIF);
                intent.putExtra("name", objects[1].toString());
                intent.putExtra("state", "Play");
                intent.putExtra("logo", img);
                context.sendBroadcast(intent);


                isImg.close();
                stream.close();

            }catch(SocketTimeoutException e){

                Toast.makeText(context, "Erreur de connexion: " + e, Toast.LENGTH_SHORT).show();

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

                        Log.d(TAG, "historique.size: " + historique.size());

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
