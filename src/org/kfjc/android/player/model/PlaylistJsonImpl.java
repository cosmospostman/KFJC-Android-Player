package org.kfjc.android.player.model;

import android.text.TextUtils;

import com.google.common.base.Strings;

import org.json.JSONArray;
import org.json.JSONException;
import org.kfjc.android.player.R;

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
    String timeString;
    List<PlaylistEntry> entries;
    String jsonString;

    public PlaylistJsonImpl(String jsonPlaylistString) {
        try {
            if (TextUtils.isEmpty(jsonPlaylistString)) {
                hasError = true;
                djName = "";
                timeString = "";
                return;
            }
            JSONArray jsonPlaylist = new JSONArray(jsonPlaylistString);

            JSONArray title = jsonPlaylist.getJSONArray(0);
            djName = tryGetString(title, 0);
            timeString = tryGetString(title, 1);

            JSONArray jsonEntries = jsonPlaylist.getJSONArray(1);
            entries = new ArrayList<>();
            for (int i = 0; i < jsonEntries.length(); i++) {
                JSONArray jsonEntry = jsonEntries.getJSONArray(i);
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
        return timeString;
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

        public PlaylistEntryJsonImpl(JSONArray entry) {
            artist = tryGetString(entry, 0);
            track = tryGetString(entry, 1);
            album = tryGetString(entry, 2);
            time = tryGetString(entry, 3);
            time = time.replaceAll("\\sPM", "p").replaceAll("\\sAM", "a");
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
            return "";
        }

        @Override
        public boolean isEmpty() {
            return Strings.isNullOrEmpty(time)
                    && Strings.isNullOrEmpty(artist)
                    && Strings.isNullOrEmpty(album)
                    && Strings.isNullOrEmpty(track);
        }
    }

    private static String tryGetString(JSONArray array, int index) {
        if (array == null) {
            return "";
        }
        try {
            return array.getString(index);
        } catch (JSONException e) {
            return "";
        }
    }
}
