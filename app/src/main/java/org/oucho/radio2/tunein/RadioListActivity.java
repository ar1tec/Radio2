package org.oucho.radio2.tunein;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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
import java.util.List;


public class RadioListActivity extends AppCompatActivity implements RadioKeys {


    String TAG = "RadioListActivity";

    private TuneInAdapter mAdapter;

    RecyclerView mRecyclerView;

    Context mContext;

    private LoaderManager.LoaderCallbacks<List<String>> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<String>>() {

        @Override
        public Loader<List<String>> onCreateLoader(int id, Bundle args) {

            Log.d(TAG, "Loader link: " + args.getString("url"));

            return new TuneInLoader(mContext, args.getString("url"));
        }

        @Override
        public void onLoadFinished(Loader<List<String>> loader, List<String> list) {
            mAdapter.setData(list);
        }

        @Override
        public void onLoaderReset(Loader<List<String>> loader) {
            //  Auto-generated method stub
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tune_in);
       // Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
       // setSupportActionBar(toolbar);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final int mUIFlag = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            getWindow().getDecorView().setSystemUiVisibility(mUIFlag);
        }

        mContext = getApplicationContext();


        int titleColor = ContextCompat.getColor(mContext, R.color.colorAccent);
        String title = mContext.getString(R.string.app_name);

        if (android.os.Build.VERSION.SDK_INT >= 24) {
            setTitle(Html.fromHtml("<font color='" + titleColor + "'>" + title + "</font>", Html.FROM_HTML_MODE_LEGACY));
        } else {
            //noinspection deprecation
            setTitle(Html.fromHtml("<font color='" + titleColor + "'>" + title + "</font>"));
        }


        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        mAdapter = new TuneInAdapter();

        mAdapter.setOnItemClickListener(mOnItemClickListener);
        mAdapter.setOnItemLongClickListener(mOnLongClickListener);

        mRecyclerView.setAdapter(mAdapter);


        Bundle args = new Bundle();
        args.putString("url", "http://opml.radiotime.com");

        load(args);

    }

    @Override
    protected void onResume() {
        super.onResume();

        broadcasteReceiver = new BroadcasteReceiver();
        IntentFilter filter = new IntentFilter(INTENT_STATE);
        registerReceiver(broadcasteReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(broadcasteReceiver);
    }

    private void load(Bundle args) {

        getSupportLoaderManager().restartLoader(0, args, mLoaderCallbacks);

   //     getSupportLoaderManager().initLoader(0, args, mLoaderCallbacks);
    }


    /* **********************************************************************************************
     *
     * Broadcast receiver
     *
     * *********************************************************************************************/
    String INTENT_STATE = "org.oucho.radio2.INTENT_TITRE";
    private BroadcasteReceiver broadcasteReceiver;

    private class BroadcasteReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {


            String receiveIntent = intent.getAction();

            if (INTENT_STATE.equals(receiveIntent)) {


                String titre = intent.getStringExtra("titre");


                int titleColor = ContextCompat.getColor(mContext, R.color.colorAccent);

                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    setTitle(Html.fromHtml("<font color='" + titleColor + "'>" + titre + "</font>", Html.FROM_HTML_MODE_LEGACY));
                } else {
                    //noinspection deprecation
                    setTitle(Html.fromHtml("<font color='" + titleColor + "'>" + titre + "</font>"));
                }
            }
        }
    }

    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position, View view) {
            String item = mAdapter.getItem(position);

            String[] parts = item.split("\" ");

            if (item.contains("type=\"link\"")) {

                String text = parts[1];

                String name = text.replace("text=\"" , "");

                String url = null;



                for (int i = 0; i < parts.length; i++) {

                    if (parts[i].contains("URL=\"")) {

                        url = parts[i].replace("URL=\"", "");

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


                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].contains("URL=\"")) {
                        url = parts[i].replace("URL=\"", "");
                        Log.d(TAG, "url: " + url);
                    }
                }

                new playItem().execute(url, name);
                /*Intent player = new Intent(mContext, PlayerService.class);

                player.putExtra("action", ACTION_PLAY);
                player.putExtra("url", url);
                player.putExtra("name", name);
                startService(player);*/
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

                TuneInAdapter.TuneInViewHolder holder = (TuneInAdapter.TuneInViewHolder) mRecyclerView.getChildViewHolder(view);

                String[] parts = item.split("\" ");


                if  (item.contains("type=\"audio\"")) {

                    String text = parts[1];

                    String name = text.replace("text=\"" , "");

                    String url = null;

                    String url_image = null;

                    for (int i = 0; i < parts.length; i++) {

                        if (parts[i].contains("URL=\"")) {

                            url = parts[i].replace("URL=\"", "");
                            Log.d(TAG, "url: " + url);
                        }


                        if (parts[i].contains("image=\"")) {

                            url_image = parts[i].replace("image=\"", "");
                            Log.d(TAG, "image url: " + url_image);
                        }
                    }

                    new saveItem().execute(url, name, url_image);
                }


                return true;
            }
        });

        mCoverPopupMenu.show();
    }


    private class saveItem extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... urlstring) {


            String httpRequest = urlstring[0].replace(" ", "%20");

            String url_radio;
            String name_radio = urlstring[1];

            String url_image = urlstring[2];

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

                String img = ImageFactory.byteToString(ImageFactory.getBytes(ImageFactory.getResizedBitmap(mContext, bmImg)));

                Intent radio = new Intent();
                radio.setAction("org.oucho.radio2.ADD_RADIO");
                radio.putExtra("url", url_radio);
                radio.putExtra("name", name_radio);
                radio.putExtra("image", img);
                mContext.sendBroadcast(radio);


            } catch (SocketTimeoutException e) {

                Toast.makeText(mContext, "Erreur de connexion: " + e, Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;

        }
    }

    private static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    //                                              <Params, Progress, Result>
    private class playItem extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... urlstring) {

            String data = null;

            String httpRequest = urlstring[0].replace(" ", "%20");

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

                Intent player = new Intent(mContext, PlayerService.class);

                player.putExtra("action", ACTION_PLAY);
                player.putExtra("url", data);
                player.putExtra("name", urlstring[1]);
                mContext.startService(player);

                stream.close();

            }catch(SocketTimeoutException e){

                Toast.makeText(mContext, "Erreur de connexion: " + e, Toast.LENGTH_SHORT).show();

            }
            catch (Exception e) {
                e.printStackTrace();
            }

            return data;


        }

    }

}
