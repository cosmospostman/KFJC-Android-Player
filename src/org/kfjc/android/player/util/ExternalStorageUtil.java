package org.kfjc.android.player.util;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.ShowDetails;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class ExternalStorageUtil {

    static final String KFJC_DIRECTORY_NAME = "kfjc";
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
    public static boolean createShowDir(Context context, ShowDetails show, Playlist playlist) {
        if (!isExternalStorageWritable()) {
            return false;
        }
        try {
            File podcastDir = makePodcastDir(context, show.getPlaylistId());
            Log.i("EXT", podcastDir.getCanonicalPath());

            // .nomedia prevents indexing and display in other apps
            (new File(podcastDir, NOMEDIA_FILENAME)).createNewFile();

            // Write the show metadata
            PrintWriter showWriter = new PrintWriter(new File(podcastDir, KFJC_INDEX_FILENAME));
            showWriter.println(show.toJsonString());
            showWriter.close();

            // Write the playlist
            PrintWriter playlistWriter = new PrintWriter(getPlaylistFile(context, show.getPlaylistId()));
            playlistWriter.println(playlist.toJsonString());
            playlistWriter.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static File getPodcastDir(Context context) {
        return new File(context.getExternalFilesDir(null), "");
    }

    public static File getPodcastDir(Context context, String playlistId) {
        return new File(context.getExternalFilesDir(null), playlistId);
    }

    public static Uri getDestinationURIForDownload(Context context, String playlistId, String filename) {
        File dest = new File(
                context.getExternalFilesDir(null),
                playlistId + "/" + filename);
        return Uri.fromFile(dest);
    }

    public static File getPlaylistFile(Context context, String playlistId) {
        File podcastDir = getPodcastDir(context, playlistId);
        return new File(podcastDir, KFJC_PLAYLIST_FILENAME);
    }

    private static File makePodcastDir(Context context, String playlistId) {
        File podcastDir = getPodcastDir(context, playlistId);
        if (!podcastDir.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created");
        }
        return podcastDir;
    }

    public static void deletePodcastDir(Context context, String playlistId) {
        File podcastDir = getPodcastDir(context, playlistId);
        deleteRecursively(podcastDir);
    }

    public static long folderSize(Context context, String playlistId) {
        return folderSize(getPodcastDir(context, playlistId));
    }

    private static long folderSize(File directory) {
        long length = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile())
                length += file.length();
            else
                length += folderSize(file);
        }
        return length;
    }

    private static void deleteRecursively(File path) {
        File[] contents = path.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteRecursively(f);
            }
        }
        path.delete();
    }

    /* Checks if external storage is available for read and write */
    static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static List<ShowDetails> getSavedShows(Context context) {
        List<ShowDetails> shows = new LinkedList<>();
        File podcastDir = getPodcastDir(context);
        if (podcastDir.listFiles() == null) {
            return Collections.emptyList();
        }
        for (File f : podcastDir.listFiles()) {
            File index = new File(f, KFJC_INDEX_FILENAME);
            ShowDetails show = new ShowDetails(readFile(index));
            if (show.hasError()) {
                Log.i(LOG_TAG, "Show has error");
            } else if (!hasAllContent(context, show)) {
                Log.i(LOG_TAG, "Missing some content");
            } else {
                shows.add(show);
            }
        }
        return shows;
    }

    public static String readFile(File f) {
        try {
            return new Scanner(f).useDelimiter("\\Z").next();
        } catch (IOException e) {
            return "";
        }
    }

    public static boolean hasAllContent(Context context, ShowDetails show) {
        File podcastDir = getPodcastDir(context, show.getPlaylistId());
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

    public static File getSavedArchive(Context context, String playlistId, String hourUrl) {
        File podcastDir = getPodcastDir(context, playlistId);
        String expectedFilename = Uri.parse(hourUrl).getLastPathSegment();
        return new File(podcastDir, expectedFilename);
    }

    public static boolean bytesAvailable(Context context, long forFileSize) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // Sorry, can't be bothered implementing. Your download might fail.
            return true;
        }
        try {
            StatFs stat = new StatFs(getPodcastDir(context).getPath());
            return stat.getAvailableBytes() > forFileSize;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
