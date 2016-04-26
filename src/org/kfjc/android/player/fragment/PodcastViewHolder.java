package org.kfjc.android.player.fragment;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.kfjc.android.player.R;
import org.kfjc.android.player.model.BroadcastShow;

public class PodcastViewHolder extends RecyclerView.ViewHolder {

    private TextView airName;
    private TextView id;

    public PodcastViewHolder(View itemView) {
        super(itemView);
        airName = (TextView) itemView.findViewById(R.id.airName);
        id = (TextView) itemView.findViewById(R.id.id);
    }

    public void setShow(BroadcastShow show) {
        airName.setText(show.getAirName());
        id.setText(show.getPlaylistId());
    }
}
