package org.oucho.radio2.gui;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.oucho.radio2.MainActivity;
import org.oucho.radio2.R;
import org.oucho.radio2.db.Radio;
import org.oucho.radio2.images.ImageFactory;
import org.oucho.radio2.interfaces.ListsClickListener;

class RadioViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private final TextView text;
    private final ImageButton menu;

    private final ImageView image;
    private final ImageView imageLogo;

    private final CardView fond;

    private Radio radio;
    private final MainActivity activity;
    private final ListsClickListener clickListener;


    RadioViewHolder(View view, MainActivity activity, ListsClickListener clickListener) {
        super(view);
        text = (TextView) view.findViewById(R.id.textViewRadio);
        menu = (ImageButton) view.findViewById(R.id.buttonMenu);

        image = (ImageView) view.findViewById(R.id.imageViewRadio);

        imageLogo = (ImageView) view.findViewById(R.id.logoViewRadio);

        fond  = (CardView) view.findViewById(R.id.fond);


        this.activity = activity;
        this.clickListener = clickListener;
        view.setOnClickListener(this);
        menu.setOnClickListener(this);
        menu.setFocusable(false);
    }

    public void update(Context context, Radio radio, String nomRadio) {

        this.radio = radio;

        text.setText(radio.getTitle());

        if (radio.getImg() != null) {
            image.setVisibility(View.INVISIBLE);
            imageLogo.setImageBitmap(ImageFactory.getImage(radio.getLogo()));
            imageLogo.setVisibility(View.VISIBLE);

        }

        if (radio.getName().equals(nomRadio)  ) {

            fond.setBackgroundColor(ContextCompat.getColor(context, R.color.amber_50));

            if (radio.getImg() == null)
            image.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent));

        }
    }


    @Override
    public void onClick(View view) {
        if(view.equals(menu)) {
            final PopupMenu popup = new PopupMenu(activity, menu);

            if (MainActivity.mpdIsInstalled) {
                popup.getMenuInflater().inflate(R.menu.contextmenu_editdelete_mpd, popup.getMenu());
            } else {
                popup.getMenuInflater().inflate(R.menu.contextmenu_editdelete, popup.getMenu());
            }

          //  popup.getMenuInflater().inflate(R.menu.contextmenu_editdelete, popup.getMenu());
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
