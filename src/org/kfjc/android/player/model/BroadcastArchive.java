package org.kfjc.android.player.model;

import com.google.common.collect.ForwardingSortedMap;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class BroadcastArchive {

    SortedMap<String, BroadcastShow> shows;

    public BroadcastArchive() {
        shows = new TreeMap<>();
    }

    public void addHour(BroadcastHour hour) {
        String id = hour.getPlaylistId();
        if (id.equals("0")) {
            return;
        }
        BroadcastShow show = shows.get(id);
        if (show == null) {
            show = new BroadcastShow(hour);
        } else {
            show.addHour(hour);
        }
        shows.put(id, show);
    }

    public List<BroadcastShow> getShows() {
        return Lists.reverse(Lists.newArrayList(shows.values()));
    }
}
