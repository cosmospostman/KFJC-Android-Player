package org.kfjc.android.player.model;

import android.text.TextUtils;

import com.google.common.base.Strings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kfjc.android.player.util.DateUtil;

import java.util.ArrayList;
import java.util.List;

/**
 *  Source: http://kfjc.org/music/json-playlist.php?i=0
 *  If there is no ?i= it returns the current playlist as does ?i=0 (same i #s as the main website)
 *
 *  It returns a json array where the first item is a two item array containing the DJ name followed
 *  by the date/time of the show, and the second item is an array of [Artist,Title,Album,TimePlayed]
 *  arrays.
 */
public class PlaylistJsonImpl implements Playlist {

    boolean hasError;
    String djName;
    long timestampMillis;
    List<PlaylistEntry> entries;
    String jsonString;

    public PlaylistJsonImpl(String jsonPlaylistString) {
        try {
            if (TextUtils.isEmpty(jsonPlaylistString)) {
                hasError = true;
                djName = "";
                timestampMillis = 0L;
                return;
            }
            JSONObject jsonPlaylist = new JSONObject(jsonPlaylistString);
            JSONObject metadata = jsonPlaylist.getJSONObject("show_info");
            JSONArray playlist = jsonPlaylist.getJSONArray("playlist");

            djName = metadata.getString("air_name");
            timestampMillis = metadata.getLong("start_time");

            entries = new ArrayList<>();
            for (int i = 0; i < playlist.length(); i++) {
                JSONObject jsonEntry = playlist.getJSONObject(i);
                entries.add(new PlaylistEntryJsonImpl(jsonEntry));
            }
            hasError = false;
            this.jsonString = jsonPlaylistString;
        } catch (JSONException e) {
            // Fuck
            hasError = true;
        }
    }

    public String toJsonString() {
        return jsonString;
    }

    @Override
    public boolean hasError() {
        return hasError;
    }

    @Override
    public String getDjName() {
        return djName;
    }

    @Override
    public String getTime() {
        return DateUtil.roundHourFormat(timestampMillis, DateUtil.FORMAT_DELUXE_DATE);
    }

    @Override
    public List<PlaylistEntry> getTrackEntries() {
        return entries;
    }

    @Override
    public PlaylistEntry getLastTrackEntry() {
        for (int i = entries.size() - 1; i >= 0; i--) {
            PlaylistEntry entry = entries.get(i);
            if (!entry.isEmpty()) {
                return entry;
            }
        }
        return new PlaylistEntryJsonImpl(null);
    }

    public class PlaylistEntryJsonImpl implements PlaylistEntry {
        String artist;
        String track;
        String album;
        String time;
        String label;

        public PlaylistEntryJsonImpl(JSONObject entry) {
            artist = tryGetString(entry, "artist");
            track = tryGetString(entry, "track_title");
            album = tryGetString(entry, "album_title");
            label = tryGetString(entry, "album_label");
            long timestamp = tryGetLong(entry, "time_played");
            if (timestamp > 0L) {
                time = DateUtil.format(timestamp, DateUtil.FORMAT_HH_MM)
                        .replaceAll("\\sPM", "p").replaceAll("\\sAM", "a");
            }
        }

        @Override
        public String getTime() {
            return time;
        }

        @Override
        public String getTrack() {
            return track;
        }

        @Override
        public String getArtist() {
            return artist;
        }

        @Override
        public String getAlbum() {
            return album;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public boolean isEmpty() {
            return Strings.isNullOrEmpty(time)
                    && Strings.isNullOrEmpty(artist)
                    && Strings.isNullOrEmpty(album)
                    && Strings.isNullOrEmpty(track)
                    && Strings.isNullOrEmpty(label);
        }
    }

    private static String tryGetString(JSONObject object, String key) {
        if (object == null) {
            return "";
        }
        try {
            return object.getString(key);
        } catch (JSONException e) {
            return "";
        }
    }

    private static long tryGetLong(JSONObject object, String key) {
        if (object == null) {
            return 0L;
        }
        try {
            return object.getLong(key);
        } catch (JSONException e) {
            return 0L;
        }
    }
}
