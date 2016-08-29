package org.kfjc.android.player.util;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.R;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.model.PlaylistJsonImpl;
import org.kfjc.android.player.model.ShowDetails;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DownloadUtil {

    private static Map<Long, ShowDetails> activeDownloads = new HashMap<>();

    public static void ensureDownloaded(Activity homeScreen, ShowDetails show) throws IOException {
        File podcastDir = ExternalStorageUtil.getPodcastDir(show.getPlaylistId());
        File podcastPlaylist = ExternalStorageUtil.getPlaylistFile(show.getPlaylistId());
        boolean podcastDirExists = podcastDir.exists();
        boolean podcastPlaylistExistsAndNotEmpty =
                podcastPlaylist.exists() && podcastPlaylist.length() > 0;
        if (! (podcastDirExists && podcastPlaylistExistsAndNotEmpty)) {
            String playlistUrl = Constants.PLAYLIST_URL + show.getPlaylistId();
            Playlist playlist = new PlaylistJsonImpl(HttpUtil.getUrl(playlistUrl));
            ExternalStorageUtil.createShowDir(show, playlist);
        }
        for (int i = 0; i < show.getUrls().size(); i++) {
            Uri uri = Uri.parse(show.getUrls().get(i));
            String filename = uri.getLastPathSegment();
            File downloadFile = new File(podcastDir, filename);
            if (! (downloadFile.exists() && downloadFile.length() > 0)) {
                DownloadManager dm = (DownloadManager) homeScreen.getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Request req = new DownloadManager.Request(uri)
                        .setTitle(homeScreen.getString(R.string.format_archive_file, show.getAirName(), i+1, show.getUrls().size()))
                        .setVisibleInDownloadsUi(false)
                        .setDestinationUri(Uri.fromFile(downloadFile));
                long referenceId = dm.enqueue(req);
                registerDownload(referenceId, show);
            }
        }
    }

    private static void registerDownload(long downloadId, ShowDetails show) {
        activeDownloads.put(downloadId, show);
    }

    public static boolean hasDownloadId(long id) {
        return activeDownloads.containsKey(id);
    }

    public static ShowDetails getDownload(long id) {
        return activeDownloads.get(id);
    }
}
