package org.kfjc.android.player;

import org.kfjc.android.player.model.Stream;

public class Constants {
    // TODO: decommission this endpoint once old clients upgrade.
    public static final String CURRENT_TRACK_URL = "http://kfjc.org/api/playlists/current.php";
    public static final String AVAILABLE_STREAMS_URL = "http://www.kfjc.org/netcast/streams.json";

    public static final String PLAYLIST_URL = "http://kfjc.org/music/json-playlist.php"; //?i=50723
    public static final int CURRENT_TRACK_POLL_DELAY_MS = 30000;
    public static final String USER_AGENT = "kfjc4droid-v4";

    public static final String RESOURCES_URL = "http://www.kfjc.org/api/resources.json";
    public static final String ARCHIVES_URL = "http://kfjc.org/api/archives.php";
    public static final Stream FALLBACK_STREAM =
            new Stream("http://netcast6.kfjc.org:80/", "Default", "128k mp3", Stream.Format.MP3);
}
