package org.kfjc.android.player.util;

import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.google.common.io.Files;

import org.kfjc.android.player.model.BroadcastShow;
import org.kfjc.android.player.model.Playlist;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

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
    public static boolean createShowDir(BroadcastShow show, Playlist playlist) {
        if (!isExternalStorageWritable()) {
            return false;
        }
        try {
            File podcastDir = makePodcastDir(show.getPlaylistId());
            Log.i("EXT", podcastDir.getCanonicalPath());

            // .nomedia prevents indexing and display in other apps
            (new File(podcastDir, NOMEDIA_FILENAME)).createNewFile();

            // Write the show metadata
            PrintWriter showWriter = new PrintWriter(new File(podcastDir, KFJC_INDEX_FILENAME));
            showWriter.println(show.toJsonString());
            showWriter.close();

            // Write the playlist
            PrintWriter playlistWriter = new PrintWriter(getPlaylistFile(show.getPlaylistId()));
            playlistWriter.println(playlist.toJsonString());
            playlistWriter.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static File getPodcastDir() {
        return new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PODCASTS), KFJC_DIRECTORY_NAME);
    }

    public static File getPodcastDir(String playlistId) {
        return new File(getPodcastDir(), playlistId);
    }

    public static File getPlaylistFile(String playlistId) {
        File podcastDir = getPodcastDir(playlistId);
        return new File(podcastDir, KFJC_PLAYLIST_FILENAME);
    }

    private static File makePodcastDir(String playlistId) {
        File podcastDir = getPodcastDir(playlistId);
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

    public static List<BroadcastShow> getSavedShows() {
        List<BroadcastShow> shows = new LinkedList<>();
        File podcastDir = getPodcastDir();
        if (podcastDir.listFiles() == null) {
            return Collections.emptyList();
        }
        for (File f : podcastDir.listFiles()) {
            File index = new File(f, KFJC_INDEX_FILENAME);
            BroadcastShow show = new BroadcastShow(readFile(index));
            if (! show.hasError()) {
                shows.add(show);
            }
        }
        return shows;
    }

    public static String readFile(File f) {
        try {
            return Files.toString(f, Charset.defaultCharset());
        } catch (IOException e) {
            return "";
        }
    }

    public static boolean hasAllContent(BroadcastShow show) {
        File podcastDir = getPodcastDir(show.getPlaylistId());
        if (! (new File(podcastDir, KFJC_INDEX_FILENAME).exists())
                && new File(podcastDir, KFJC_PLAYLIST_FILENAME).exists()) {
            return false;
        }
        for (String url : show.getUrls()) {
            String expectedFilename = Uri.parse(url).getLastPathSegment();
            File expectedFile = new File(podcastDir, expectedFilename);
            if (!expectedFile.exists()) {
                return false;
            }
        }
        return true;
    }

    public static List<File> getSavedArchivesForShow(BroadcastShow show) {
        List<File> files = new ArrayList<>();
        File podcastDir = getPodcastDir(show.getPlaylistId());
        if (hasAllContent(show)) {
            for (String url : show.getUrls()) {
                String expectedFilename = Uri.parse(url).getLastPathSegment();
                File expectedFile = new File(podcastDir, expectedFilename);
                if (expectedFile.exists()) {
                    files.add(expectedFile);
                }
            }
        }
        return files;
    }
}
