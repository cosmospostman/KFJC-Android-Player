package org.kfjc.android.player.fragment;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.kfjc.android.player.R;
import org.kfjc.android.player.model.BroadcastShow;

import java.util.List;

public class PodcastRecyclerAdapter extends RecyclerView.Adapter<PodcastViewHolder> {

    enum Type {
        HORIZONTAL,
        VERTICAL
    }

    private List<BroadcastShow> shows;
    private PodcastViewHolder.PodcastClickDelegate clickDelegate;
    private Type layoutType;

    public PodcastRecyclerAdapter(List<BroadcastShow> shows, Type type,
                                  PodcastViewHolder.PodcastClickDelegate clickDelegate) {
        this.shows = shows;
        this.clickDelegate = clickDelegate;
        this.layoutType = type;
    }

    @Override
    public PodcastViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType, null, false);
        PodcastViewHolder viewHolder = new PodcastViewHolder(view, clickDelegate, layoutType);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(PodcastViewHolder holder, int position) {
        holder.setShow(shows.get(position));
    }

    @Override
    public int getItemCount() {
        return shows.size();
    }

    @Override
    public int getItemViewType(int position) {
        switch (layoutType) {
            case HORIZONTAL:
                return R.layout.recycler_podcast;
            case VERTICAL:
                return R.layout.recycler_saved_podcast;
            default:
                return -1;
        }
    };
}
