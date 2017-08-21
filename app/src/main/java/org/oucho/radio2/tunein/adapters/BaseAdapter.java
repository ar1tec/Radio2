package org.oucho.radio2.tunein.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;


public abstract class BaseAdapter<V extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<V> {

    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    void triggerOnItemClickListener(int position, View view) {
        if(mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(position, view);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position, View view);
    }


    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        mOnItemLongClickListener = listener;
    }

    void triggerOnItemLongClickListener(int position, View view) {
        if(mOnItemLongClickListener != null) {
            mOnItemLongClickListener.onItemLongClick(position, view);
        }
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position, View view);
    }

}
