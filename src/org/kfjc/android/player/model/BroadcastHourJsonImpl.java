package org.kfjc.android.player.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Example input:
 *  { "air_name":"Abacus Finch",
 *    "show_date":"2016-04-25",
 *    "start_hour":16,
 *    "url":"http:\/\/archive.kfjc.org\/archives\/1604251553h_abacus_finch.mp3",
 *    "playlist_num":51251 }
 */
public class BroadcastHourJsonImpl implements BroadcastHour  {

    private String airName;
    private long  timestamp;
    private String url;
    private String playlistId;

    public BroadcastHourJsonImpl(JSONObject jsonHour) {
        try {
            airName = jsonHour.getString("air_name");
            timestamp = jsonHour.getLong("start_time");
            url = jsonHour.getString("url");
            playlistId = jsonHour.getString("playlist_num");
        } catch (JSONException e) {}
    }

    @Override
    public String getAirName() {
        return airName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getPlaylistId() {
        return playlistId;
    }
}
