package org.kfjc.android.player.fragment;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.R;
import org.kfjc.android.player.model.ShowDetails;
import org.kfjc.android.player.util.DateUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PodcastViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    public interface PodcastClickDelegate {
        void onClick(ShowDetails show);
    }

    private static final float[] OPACITIES = {
            1f, 1f, 1f, 1f, 1f, 1f, //12a-5a
            0.44f, 0.44f, 0.44f, 0.16f, 0.16f, 0.16f, //6-11a
            0.16f, 0.16f, 0.16f, 0.16f, 0.16f, 0.16f, //12-5p
            0.58f, 0.72f, 0.86f, 0.86f, 1f, 1f //6-11p
        };


    private PodcastRecyclerAdapter.Type layoutType;

    private View iconBackground;
    private TextView iconLetter;
    private TextView airName;
    private TextView timestamp;
    private PodcastClickDelegate clickDelegate;
    private ShowDetails show;

    public PodcastViewHolder(View itemView, PodcastClickDelegate clickDelegate,
                             PodcastRecyclerAdapter.Type type) {
        super(itemView);
        this.layoutType = type;
        this.clickDelegate = clickDelegate;
        itemView.setOnClickListener(this);
        iconBackground = itemView.findViewById(R.id.iconBackground);
        iconLetter = (TextView) itemView.findViewById(R.id.iconLetter);
        airName = (TextView) itemView.findViewById(R.id.airName);
        timestamp = (TextView) itemView.findViewById(R.id.timestamp);
    }

    public void setShow(ShowDetails show) {
        this.show = show;
        int color = getColor(show.getTimestamp());
        if (show.getAirName().toUpperCase().contains("SPLIFF")) {
            color = Color.parseColor("#C8E6C9");
        }
        if (iconLetter != null) {
            iconLetter.setText(getIconLetter());
            iconLetter.setTextColor(color);
        }
        if (iconBackground != null) {
            iconBackground.setBackgroundColor(color);
        }
        airName.setText(show.getAirName());
        timestamp.setText(DateUtil.roundUpHourFormat(show.getTimestamp(), getSimpleDateFormat()));
    }

    private String getIconLetter() {
        if (show.getAirName().toUpperCase().contains("NUMBER 6")) {
            return "6";
        }
        if (show.getAirName().toUpperCase().contains("CINDERAURA")) {
            return "\uD83D\uDC3E";
        }
        if (show.getAirName().toUpperCase().contains("PAX")) {
            return "\u262E";
        }
        return "" + show.getAirName().charAt(0);
    }

    private SimpleDateFormat getSimpleDateFormat() {
        return (layoutType == PodcastRecyclerAdapter.Type.HORIZONTAL)
            ? DateUtil.FORMAT_SHORT_DATE
            : DateUtil.FORMAT_FULL_DATE;
    }

    private int getClockHour(long timestamp) {
        Date date = new Date(DateUtil.roundUpHour(timestamp) * 1000);
        SimpleDateFormat df = new SimpleDateFormat("H");
        df.setTimeZone(Constants.BROADCAST_TIMEZONE);
        return Integer.parseInt(df.format(date));
    }

    private int getColor(long timestamp) {
        int hour = getClockHour(timestamp);
        return Color.argb((int)(OPACITIES[hour]*255), 46, 52, 54);
    }

    @Override
    public void onClick(View v) {
        clickDelegate.onClick(show);
    }

}
