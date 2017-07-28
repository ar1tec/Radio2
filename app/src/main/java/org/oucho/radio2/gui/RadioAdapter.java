package org.oucho.radio2.gui;


import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.oucho.radio2.MainActivity;
import org.oucho.radio2.R;
import org.oucho.radio2.db.Radio;
import org.oucho.radio2.interfaces.ListsClickListener;

import java.util.ArrayList;

public class RadioAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final MainActivity activity;
    private final ArrayList<Object> radioItems;
    private final LayoutInflater inflater;
    private final ListsClickListener clickListener;

    private final String radioName;


    public RadioAdapter(MainActivity activity, ArrayList<Object> radioItems, String radioName, ListsClickListener clickListener) {
        this.activity = activity;
        this.radioItems = radioItems;
        this.clickListener = clickListener;
        inflater = activity.getLayoutInflater();

        this.radioName = radioName;
    }

    @SuppressLint("InflateParams")
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new RadioViewHolder(inflater.inflate(R.layout.radio_item, null), activity, clickListener);

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Object item = radioItems.get(position);

        ((RadioViewHolder) holder).update(activity.getApplicationContext(), (Radio)item, radioName);

    }

    @Override
    public int getItemCount() {
        return radioItems.size();
    }

}
