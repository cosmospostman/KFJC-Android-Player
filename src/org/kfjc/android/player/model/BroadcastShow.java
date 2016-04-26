package org.kfjc.android.player.model;

import java.util.ArrayList;
import java.util.List;

public class BroadcastShow {
    private final String playlistId;

    private final String airName;
    private final String startDateTime = "ddmmyy hh";
    List<String> urls;

    BroadcastShow(BroadcastHour hour) {
        urls = new ArrayList<>();
        this.playlistId = hour.getPlaylistId();
        this.airName = hour.getAirName();
        urls.add(hour.getUrl());
    }

    void addHour(BroadcastHour hour) {
        if (!hour.getPlaylistId().equals(playlistId)) {
            return;
        }
        urls.add(hour.getUrl());
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public String getAirName() {
        return airName;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    List<String> getUrls() {
        return urls;
    }
}
