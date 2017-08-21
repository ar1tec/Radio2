package org.oucho.radio2.tunein.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;


import org.oucho.radio2.R;

import java.util.ArrayList;
import java.util.List;


public class TuneInAdapter extends BaseAdapter<TuneInAdapter.TuneInViewHolder>  {

    private static final String TAG = "TuneInAdapter";
    private List<String> categorieList = new ArrayList<>();


    public void setData(List<String> data) {
        categorieList = data;
        notifyDataSetChanged();
    }


    @Override
    public TuneInViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.categorie_item, parent, false);

        return new TuneInViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(TuneInViewHolder viewHolder, int position) {
        String item = categorieList.get(position);

        String[] parts = item.split("\" ");


        if (item.contains("type=\"link\"")) {

            String text = parts[1];

            String name = text.replace("text=\"" , "");

            viewHolder.text.setVisibility(View.VISIBLE);
            viewHolder.relativeLayout.setVisibility(View.GONE);

            viewHolder.text.setText(name);
        }

        if  (item.contains("type=\"audio\"")) {

            String text = parts[1];

            String name = text.replace("text=\"" , "");

            viewHolder.text.setVisibility(View.GONE);
            viewHolder.relativeLayout.setVisibility(View.VISIBLE);

            viewHolder.details_title.setText(name);

            String url;



            for (int i = 0; i < parts.length; i++) {

                if (parts[i].contains("URL=\"")) {

                    url = parts[i].replace("URL=\"", "");

                    Log.d(TAG, "url: " + url);



                }

                if (parts[i].contains("subtext=\"")) {

                    viewHolder.detail_subtitle.setText(parts[i].replace("subtext=\"", ""));

                    Log.d(TAG, "subtitle: " + parts[i].replace("subtext=\"", ""));


                }

                if (parts[i].contains("image=\"")) {

                    String url_image = parts[i].replace("image=\"", "");

                    Log.d(TAG, "image url: " + url_image);

                    Picasso.with(viewHolder.itemView.getContext()).load(parts[i].replace("image=\"", "")).fit().centerCrop().into(viewHolder.image);
                }

            }
        }
    }

    @Override
    public int getItemCount() {

        return categorieList.size();
    }

    public String getItem(int position) {

        return categorieList.get(position);
    }

    public class TuneInViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private final TextView text;

        private final TextView details_title;
        private final TextView detail_subtitle;

        private final ImageView image;

        private final RelativeLayout relativeLayout;


        TuneInViewHolder(View itemView) {
            super(itemView);

            text = (TextView) itemView.findViewById(R.id.item_title);

            details_title = (TextView) itemView.findViewById(R.id.detail_title);
            detail_subtitle = (TextView) itemView.findViewById(R.id.detail_subtitle);

            image = (ImageView) itemView.findViewById(R.id.detail_image);

            relativeLayout = (RelativeLayout) itemView.findViewById(R.id.detail_layout);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();

            triggerOnItemClickListener(position, v);

        }

        @Override
        public boolean onLongClick(View v) {
            int position = getAdapterPosition();

            return true;
        }
    }

}