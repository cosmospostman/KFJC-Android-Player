package org.kfjc.android.player.fragment;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.kfjc.android.player.R;
import org.kfjc.android.player.model.BroadcastShow;

public class PodcastViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    public interface PodcastClickDelegate {
        public void onClick(BroadcastShow show);
    }

    private TextView airName;
    private TextView timestamp;
    private PodcastClickDelegate clickDelegate;
    private BroadcastShow show;

    public PodcastViewHolder(View itemView, PodcastClickDelegate clickDelegate) {
        super(itemView);
        this.clickDelegate = clickDelegate;
        itemView.setOnClickListener(this);
        airName = (TextView) itemView.findViewById(R.id.airName);
        timestamp = (TextView) itemView.findViewById(R.id.timestamp);
    }

    public void setShow(BroadcastShow show) {
        this.show = show;
        airName.setText(show.getAirName());
        timestamp.setText(show.getTimestampString());
    }

    @Override
    public void onClick(View v) {
        clickDelegate.onClick(show);
    }
}
