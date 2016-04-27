package org.kfjc.android.player.fragment;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.kfjc.android.player.R;
import org.kfjc.android.player.model.BroadcastShow;

import java.util.List;

public class PodcastRecyclerAdapter extends RecyclerView.Adapter<PodcastViewHolder> {

    private List<BroadcastShow> shows;
    private PodcastViewHolder.PodcastClickDelegate clickDelegate;

    public PodcastRecyclerAdapter(List<BroadcastShow> shows,
                                  PodcastViewHolder.PodcastClickDelegate clickDelegate) {
        this.shows = shows;
        this.clickDelegate = clickDelegate;
    }

    @Override
    public PodcastViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_podcast, null, false);
        PodcastViewHolder viewHolder = new PodcastViewHolder(view, clickDelegate);
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
}
