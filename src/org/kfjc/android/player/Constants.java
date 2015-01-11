package org.kfjc.android.player;

public class Constants {
    public static final String CURRENT_TRACK_URL = "http://kfjc.org/api/playlists/current.php";
    public static final int CURRENT_TRACK_POLL_DELAY_MS = 30000;

    public static final String AVAILABLE_STREAMS_URL = "http://www.kfjc.org/netcast/streams.json";
    public static final String FALLBACK_STREAM_NAME = "High/128k mp3";
    public static final String FALLBACK_STREAM_URL = "http://netcast6.kfjc.org:80/";
    public static final String FALLBACK_STREAM_JSON =
            String.format("[ { name:'%s', url:'%s' } ]", FALLBACK_STREAM_NAME, FALLBACK_STREAM_URL);

    public static final String LOG_TAG = "kfjc";
}