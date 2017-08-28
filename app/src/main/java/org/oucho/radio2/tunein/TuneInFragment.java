package org.oucho.radio2.tunein;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import org.oucho.radio2.PlayerService;
import org.oucho.radio2.R;
import org.oucho.radio2.interfaces.RadioKeys;
import org.oucho.radio2.tunein.adapters.BaseAdapter;
import org.oucho.radio2.tunein.adapters.TuneInAdapter;
import org.oucho.radio2.tunein.loaders.TuneInLoader;
import org.oucho.radio2.utils.CustomLayoutManager;
import org.oucho.radio2.utils.ImageFactory;
import org.oucho.radio2.utils.fastscroll.FastScrollRecyclerView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class TuneInFragment extends Fragment implements RadioKeys {

    private final List<String> historique = new ArrayList<>();
    private static final String TAG = "TuneInFragment";
    private TuneInAdapter mAdapter;
    private Context mContext;
    private LinearLayout progressBar;

    private Receiver receiver;
    private FastScrollRecyclerView mRecyclerView;

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

        mRecyclerView = rootView.findViewById(R.id.recyclerview);

        mRecyclerView.setLayoutManager(new CustomLayoutManager(mContext));

        mAdapter = new TuneInAdapter();
        mAdapter.setOnItemClickListener(mOnItemClickListener);
        mAdapter.setOnItemLongClickListener(mOnLongClickListener);

        mRecyclerView.setAdapter(mAdapter);

        progressBar = rootView.findViewById(R.id.progressBar_layout);

        Bundle args = new Bundle();
        args.putString("url", "http://opml.radiotime.com");

        load(args);

        return rootView;
    }



    private void load(Bundle args) {
        Animation animFadeIn = AnimationUtils.loadAnimation(mContext, android.R.anim.fade_in);
        animFadeIn.setDuration(200);
        progressBar.setAnimation(animFadeIn);
        progressBar.setVisibility(View.VISIBLE);
        getLoaderManager().restartLoader(0, args, mLoaderCallbacks);
    }

    private final LoaderManager.LoaderCallbacks<List<String>> mLoaderCallbacks = new LoaderManager.LoaderCallbacks<List<String>>() {

        @Override
        public Loader<List<String>> onCreateLoader(int id, Bundle args) {

            String url = args.getString("url");
            historique.add(url);
            return new TuneInLoader(mContext, args.getString("url"));
        }

        @Override
        public void onLoadFinished(Loader<List<String>> loader, List<String> list) {
            if (list != null)
                mAdapter.setData(list);

            Animation animFadeOut = AnimationUtils.loadAnimation(mContext, android.R.anim.fade_out);
            animFadeOut.setDuration(200);
            progressBar.setAnimation(animFadeOut);
            progressBar.setVisibility(View.GONE);

            mRecyclerView.scrollToPosition(0);
        }

        @Override
        public void onLoaderReset(Loader<List<String>> loader) {}
    };


    private final BaseAdapter.OnItemClickListener mOnItemClickListener = new BaseAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(int position) {

            String item = mAdapter.getItem(position);
            String[] parts = item.split("\" ");

            if (item.contains("type=\"link\"")) {

                String url = null;

                for (String part : parts) {

                    if (part.contains("URL=\"")) {
                        url = part.replace("URL=\"", "").replace("\"", "");
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
        mCoverPopupMenu.setOnMenuItemClickListener(item0 -> {

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
                    }

                    if (part.contains("image=\"")) {
                        url_image = part.replace("image=\"", "");
                    }
                }

                new saveItem().execute(url, name, url_image, mContext);
            }

            return true;
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

                URL getUrl = new URL(httpRequest);
                HttpURLConnection connUrl = (HttpURLConnection) getUrl.openConnection();
                connUrl.setRequestProperty("User-Agent", USER_AGENT);
                connUrl.connect();
                InputStream streamUrl = connUrl.getInputStream();
                url_radio = convertStreamToString(streamUrl);
                streamUrl.close();

                URL getImg = new URL(url_image);
                HttpURLConnection connImg = (HttpURLConnection) getImg.openConnection();
                connImg.setRequestProperty("User-Agent", USER_AGENT);
                connImg.connect();
                InputStream streamImg = connImg.getInputStream();
                bmImg = BitmapFactory.decodeStream(streamImg);
                streamImg.close();

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
                error.putExtra("error", "TimeoutException " + e);
                context.sendBroadcast(error);

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

                URL getUrl = new URL(httpRequest);
                HttpURLConnection connUrl = (HttpURLConnection) getUrl.openConnection();
                connUrl.setRequestProperty("User-Agent", USER_AGENT);
                connUrl.connect();
                InputStream streamUrl = connUrl.getInputStream();
                data = convertStreamToString(streamUrl);
                streamUrl.close();

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
                error.putExtra("error", "TimeoutException " + e);
                context.sendBroadcast(error);

            }
            catch (Exception e) {
                e.printStackTrace();
            }

            return data;
        }
    }

    private static String convertStreamToString(InputStream is) {
        Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    @Override
    public void onResume() {
        super.onResume();

        receiver = new Receiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_SEARCH);
        filter.addAction(INTENT_FOCUS);
        mContext.registerReceiver(receiver, filter);

        // Active la touche back
        //noinspection ConstantConditions
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener((v, keyCode, event) -> {

            AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                //noinspection ConstantConditions
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);
            }

            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                //noinspection ConstantConditions
                audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND);
            }

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
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        mContext.unregisterReceiver(receiver);

    }

    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String receiveIntent = intent.getAction();

            if (INTENT_SEARCH.equals(receiveIntent)) {

                String text = intent.getStringExtra("search");
                search(text);
            }

            if (INTENT_FOCUS.equals(receiveIntent)) {
                //noinspection ConstantConditions
                getView().setFocusableInTouchMode(true);
                getView().requestFocus();
            }

        }
    }

    private void search(String search) {

        String query = search.replace(" ", "%20");

        Bundle args = new Bundle();
        args.putString("url", "http://opml.radiotime.com/Search.ashx?query=" + query);

        //noinspection ConstantConditions
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();

        load(args);
    }

}
