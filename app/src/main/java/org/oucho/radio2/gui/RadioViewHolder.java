package org.oucho.radio2.gui;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.oucho.radio2.MainActivity;
import org.oucho.radio2.R;
import org.oucho.radio2.db.Radio;
import org.oucho.radio2.utils.ImageFactory;
import org.oucho.radio2.interfaces.ListsClickListener;

class RadioViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private final TextView text;
    private final ImageButton menu;

    private final ImageView imageDefault;
    private final ImageView logoRadio;

    private final RelativeLayout fond;

    private Radio radio;
    private final MainActivity activity;
    private final ListsClickListener clickListener;


    RadioViewHolder(View view, MainActivity activity, ListsClickListener clickListener) {
        super(view);

        this.activity = activity;
        this.clickListener = clickListener;

        text = (TextView) view.findViewById(R.id.textViewRadio);
        menu = (ImageButton) view.findViewById(R.id.buttonMenu);
        imageDefault = (ImageView) view.findViewById(R.id.imageRadioDefault);
        logoRadio = (ImageView) view.findViewById(R.id.logoViewRadio);
        fond  = (RelativeLayout) view.findViewById(R.id.fond);

        view.setOnClickListener(this);
        menu.setOnClickListener(this);
        menu.setFocusable(false);
    }

    public void update(Context context, Radio radio, String nomRadio) {

        this.radio = radio;

        text.setText(radio.getTitle());

        if (radio.getLogo() != null) {
            imageDefault.setVisibility(View.INVISIBLE);
            logoRadio.setImageBitmap(ImageFactory.getImage(radio.getLogo()));
            logoRadio.setVisibility(View.VISIBLE);
        }

        if (radio.getTitle().equals(nomRadio)  ) {

            fond.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
            text.setTextColor(ContextCompat.getColor(context, R.color.white));
            menu.setImageDrawable(activity.getDrawable(R.drawable.ic_more_vert_white_24dp));

        } else {

            fond.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
            text.setTextColor(ContextCompat.getColor(context, R.color.grey_800));
            menu.setImageDrawable(activity.getDrawable(R.drawable.ic_more_vert_grey_400_24dp));
        }
    }


    @Override
    public void onClick(View view) {
        if(view.equals(menu)) {
            final PopupMenu popup = new PopupMenu(activity, menu);

            if (MainActivity.getMpdAppIsInstalled()) {
                popup.getMenuInflater().inflate(R.menu.contextmenu_editdelete_mpd, popup.getMenu());
            } else {
                popup.getMenuInflater().inflate(R.menu.contextmenu_editdelete, popup.getMenu());
            }

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    clickListener.onPlayableItemMenuClick(radio, item.getItemId());
                    return true;
                }
            });

            popup.show();
        } else {
            clickListener.onPlayableItemClick(radio);
        }
    }
}
