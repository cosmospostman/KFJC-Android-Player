package org.kfjc.android.player.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ShowListBuilder {

    private Multimap<String, BroadcastHour> hoursByPlaylistId = HashMultimap.create();

    private ShowListBuilder() {}

    public static ShowListBuilder newInstance() {
        return new ShowListBuilder();
    }

    public void addHour(BroadcastHour hour) {
        String playlistId = hour.getPlaylistId();
        if (!playlistId.equals("0")) {
            hoursByPlaylistId.put(hour.getPlaylistId(), hour);
        }
    }

    public List<ShowDetails> build() {
        SortedMap<String, ShowDetails> shows = new TreeMap<>();
        for (String playlistId : hoursByPlaylistId.keys()) {
            ShowDetails details = new ShowDetails(hoursByPlaylistId.get(playlistId));
            shows.put(details.getPlaylistId(), details);
        }
        return Lists.reverse(Lists.newArrayList(shows.values()));
    }
}
