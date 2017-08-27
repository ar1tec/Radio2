package org.oucho.radio2.tunein.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.oucho.radio2.R;
import org.oucho.radio2.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;


public class TuneInAdapter extends BaseAdapter<TuneInAdapter.TuneInViewHolder>  {

    private static final String TAG = "TuneInAdapter";
    private List<String> categorieList = new ArrayList<>();

    private boolean typeAudio = true;

    public void setData(List<String> data) {

        List<String> list = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            if  (data.get(i).contains("type=\"audio\"")) {
                list.add(data.get(i));
            }
        }

        for (int i = 0; i < data.size(); i++) {
            if  (data.get(i).contains("type=\"link\"")) {
                list.add(data.get(i));
            }
        }

        categorieList = list;
        typeAudio = data.contains("type=\"audio\"");
        notifyDataSetChanged();
    }


    @Override
    public TuneInViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_tunein_list_item, parent, false);
        return new TuneInViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(TuneInViewHolder viewHolder, int position) {

        String item = categorieList.get(position);
        String[] parts = item.split("\" ");

        viewHolder.text.setVisibility(View.GONE);
        viewHolder.relativeLayout.setVisibility(View.GONE);

        if  (item.contains("type=\"audio\"")) {

            String text = parts[1];
            String name = text.replace("text=\"" , "");

            viewHolder.text.setVisibility(View.GONE);
            viewHolder.relativeLayout.setVisibility(View.VISIBLE);
            viewHolder.details_title.setText(name);

            for (String part : parts) {

                if (part.contains("subtext=\"")) {
                    viewHolder.detail_subtitle.setText(part.replace("subtext=\"", ""));
                }

                if (part.contains("image=\"")) {
                    String url_image = part.replace("image=\"", "");
                    Picasso.with(viewHolder.itemView.getContext()).load(url_image).fit().centerCrop().into(viewHolder.image);
                }
            }
        }

        if  (!typeAudio) {

            if (item.contains("type=\"link\"")) {

                String text = parts[1];
                String name = text.replace("text=\"", "");

                viewHolder.text.setVisibility(View.VISIBLE);
                viewHolder.relativeLayout.setVisibility(View.GONE);
                viewHolder.text.setText(name);
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

        private final RelativeLayout relativeLayout;
        private final ImageView image;
        private final TextView detail_subtitle;
        private final TextView details_title;
        private final TextView text;

        TuneInViewHolder(View itemView) {
            super(itemView);

            text = itemView.findViewById(R.id.item_title);

            details_title = itemView.findViewById(R.id.detail_title);
            detail_subtitle = itemView.findViewById(R.id.detail_subtitle);

            image = itemView.findViewById(R.id.detail_image);

            relativeLayout = itemView.findViewById(R.id.detail_layout);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            triggerOnItemClickListener(position, v);
        }

        @Override
        public boolean onLongClick(View v) {
            int position = getAdapterPosition();
            if (categorieList.get(position).contains("type=\"audio\"") ) {
                triggerOnItemLongClickListener(position, v);
                return true;
            } else {
                return false;
            }
        }
    }

}