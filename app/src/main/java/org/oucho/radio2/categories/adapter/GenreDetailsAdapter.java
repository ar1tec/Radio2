package org.oucho.radio2.categories.adapter;


import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.IntDef;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.oucho.radio2.NavigationHelper;
import org.oucho.radio2.PlayerService;
import org.oucho.radio2.R;
import org.oucho.radio2.categories.model.BrowsableItem;
import org.oucho.radio2.categories.model.TuneInItem;
import org.oucho.radio2.images.ImageFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;

import static org.oucho.radio2.interfaces.RadioKeys.ACTION_PLAY;


public class GenreDetailsAdapter extends RecyclerView.Adapter<GenreDetailsAdapter.ViewHolder> implements View.OnClickListener, View.OnLongClickListener {

    private final static String TAG = "GenreDetailsAdapter";
    private final static int TYPE_GENRE = 0;
    private final static int TYPE_ITEM = 1;
    private RecyclerView recyclerView;

    private Context mContext;

    @IntDef({TYPE_GENRE, TYPE_ITEM})
    @interface ItemType {
    }

    private List<BrowsableItem> browsableItems;
    private final NavigationHelper navigationHelper;

    public GenreDetailsAdapter(Context context, List<BrowsableItem> browsableItems, NavigationHelper navigationHelper) {
        this.mContext = context;
        this.browsableItems = browsableItems;
        this.navigationHelper = navigationHelper;
    }

    @ItemType
    @Override
    public int getItemViewType(int position) {
        return browsableItems.get(position) instanceof TuneInItem ? TYPE_ITEM : TYPE_GENRE;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, @ItemType int viewType) {
        int layout = viewType == TYPE_ITEM ? R.layout.list_item_genre_details : R.layout.list_item_category;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);

        view.setOnClickListener(this);

        if (viewType == TYPE_ITEM) {
            view.setOnLongClickListener(this);
        }

        return new GenreDetailsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        BrowsableItem item = browsableItems.get(position);
        holder.title.setText(item.getText());

        if (holder.getItemViewType() == TYPE_ITEM) {
            TuneInItem tuneInItem = (TuneInItem) item;
            Picasso.with(holder.itemView.getContext()).load(tuneInItem.getImage()).fit().centerCrop().into(holder.image);
            holder.subTitle.setText(tuneInItem.getSubtext());
        }

        holder.item  = item;

    }


    @Override
    public int getItemCount() {
        return browsableItems == null ? 0 : browsableItems.size();
    }

    @Override
    public void onClick(View v) {

        Log.d(TAG, "onClick(View v)");

        GenreDetailsAdapter.ViewHolder holder = (GenreDetailsAdapter.ViewHolder) recyclerView.getChildViewHolder(v);

        if (holder.getItemViewType() == TYPE_GENRE && null != navigationHelper && recyclerView != null) {

            Log.d(TAG, "onClick(View v), if (null != navigationHelper && recyclerView != null)");

            navigationHelper.showCategory(holder.item);
        }

        if (holder.getItemViewType() == TYPE_ITEM && null != navigationHelper && recyclerView != null) {

            BrowsableItem item = holder.item;

            TuneInItem tuneInItem = (TuneInItem) item;

            new playItem().execute(tuneInItem.getUrl(), tuneInItem.getText());
        }
    }

    @Override
    public boolean onLongClick(View view) {

        showPopup(view);

        return true;
    }

    private void showPopup(final View view) {

        android.widget.PopupMenu mCoverPopupMenu = new android.widget.PopupMenu(mContext, view);

        mCoverPopupMenu.getMenu().add(1, 1, 0, "Ajouter à la liste");
        mCoverPopupMenu.setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(final MenuItem item0) {

                GenreDetailsAdapter.ViewHolder holder = (GenreDetailsAdapter.ViewHolder) recyclerView.getChildViewHolder(view);

                if (holder.getItemViewType() == TYPE_ITEM && null != navigationHelper && recyclerView != null) {

                    BrowsableItem item = holder.item;

                    final TuneInItem tuneInItem = (TuneInItem) item;

                    String plop = tuneInItem.getImage();

                    Log.d(TAG, "plop " + plop);


                    new saveItem().execute(tuneInItem.getUrl(), tuneInItem.getText(), tuneInItem.getImage());

                }

/*                if (groupId == 1) {

                    Album album = (Album) itemView;

                    final AlbumInfo albumInfo = new AlbumInfo(album);

                    CoverManager.getInstance().clear(albumInfo);

                    mAdapter.notifyDataSetChanged();

                } else {
                    result = false;
                }*/

                return true;
            }
        });

        mCoverPopupMenu.show();
    }


    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }

    public void setItems(List<BrowsableItem> browsableItems) {
        this.browsableItems = browsableItems;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        TextView subTitle;
        ImageView image;

        BrowsableItem item;

        ViewHolder(View view) {
            super(view);

            title = (TextView) view.findViewById(R.id.item_title);

            subTitle = (TextView) view.findViewById(R.id.item_subtitle);

            image = (ImageView) view.findViewById(R.id.item_image);
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

    private class saveItem extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... urlstring) {

            String data = null;

            String httpRequest = urlstring[0].replace(" ", "%20");

            Bitmap bitmap = null;

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



            return data;


        }


    }
}
