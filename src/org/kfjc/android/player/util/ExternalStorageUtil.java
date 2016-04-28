package org.kfjc.android.player.util;

import android.os.Environment;
import android.util.Log;

import org.kfjc.android.player.model.BroadcastShow;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class ExternalStorageUtil {

    private static final String KFJC_DIRECTORY_NAME = "KfjcPodcasts";
    private static final String KFJC_INDEX_FILENAME = "index.json";
    private static final String KFJC_PLAYLIST_FILENAME = "playlist.json";
    private static final String NOMEDIA_FILENAME = ".nomedia";

    private static final String LOG_TAG = ExternalStorageUtil.class.getSimpleName();

    /**
     * Directory structure:
     * KfjcPodcasts
     *  - PlaylistID
     *    - .nomedia (prevent indexing and display in other apps)
     *    - index.json
     *    - playlist.json
     *    - mp3 files
     */

    /** Create a directory for an archive show. Return true if successful */
    public static boolean createShowDir(BroadcastShow show) {
        if (!isExternalStorageWritable()) {
            return false;
        }
        try {
            File podcastDir = getPodcastDir(show.getPlaylistId());
            Log.i("EXT", podcastDir.getCanonicalPath());

            // .nomedia prevents indexing and display in other apps
            (new File(podcastDir, NOMEDIA_FILENAME)).createNewFile();
            // Write the show metadata
            PrintWriter showWriter = new PrintWriter(new File(podcastDir, KFJC_INDEX_FILENAME));
            showWriter.println(show.toJsonString());

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static File getPodcastDir(String playlistId) {
        File kfjcDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PODCASTS), KFJC_DIRECTORY_NAME);
        File podcastDir = new File(kfjcDir, playlistId);
        if (!podcastDir.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created");
        }
        return podcastDir;
    }

    /* Checks if external storage is available for read and write */
    static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
