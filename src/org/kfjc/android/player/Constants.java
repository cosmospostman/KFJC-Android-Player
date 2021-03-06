package org.kfjc.android.player;

import org.kfjc.android.player.model.KfjcMediaSource;

import java.util.TimeZone;

public class Constants {
    // TODO: decommission this endpoint once old clients upgrade.
    public static final String CURRENT_TRACK_URL = "http://kfjc.org/api/playlists/current.php";

    public static final String PLAYLIST_URL = "https://kfjc.org/api/playlists/?i="; // 50723
    public static final String CURRENT_PLAYLIST_ID = "0";
    public static final int CURRENT_TRACK_POLL_DELAY_MS = 30000;
    public static final String USER_AGENT = "kfjc4droid-v" + BuildConfig.VERSION_CODE;

    public static final String RESOURCES_URL = "https://kfjc.org/api/resources.json";
    public static final String ARCHIVES_URL = "https://kfjc.org/api/shows.php";
    public static final KfjcMediaSource FALLBACK_MEDIA_SOURCE =
            new KfjcMediaSource("http://netcast.kfjc.org:80/", KfjcMediaSource.Format.MP3, "Default", "128k mp3");

    public static final TimeZone BROADCAST_TIMEZONE = TimeZone.getTimeZone("America/Los_Angeles");
    public static final long SHOW_SEGMENT_LENGTH = 3600000; // Milliseconds
}
