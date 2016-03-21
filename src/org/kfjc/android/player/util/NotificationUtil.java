package org.kfjc.android.player.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import org.kfjc.android.player.activity.HomeScreenDrawerActivity;
import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.R;

public class NotificationUtil {

    public static final int KFJC_NOTIFICATION_ID = 1;

    private Context context;
    private NotificationManager notificationManager;

    public NotificationUtil(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void updateNowPlayNotification(Playlist playlist) {
        if (playlist == null) {
            return;
        }
        if (playlist.hasError()) {
            cancelNowPlayNotification();
            postNotification(
                    context.getString(R.string.app_name),
                    context.getString(R.string.empty_string));
        } else {
            postNotification(
                    playlist.getDjName(),
                    artistTrackStringNotification(playlist.getLastTrackEntry()));
        }
    }

    private String artistTrackStringNotification(Playlist.PlaylistEntry e) {
        String artistTrackString;
        if (!TextUtils.isEmpty(e.getArtist()) && !TextUtils.isEmpty(e.getTrack()) ) {
            // Both artist and track supplied
            artistTrackString =
                    context.getString(R.string.artist_track_format, e.getArtist(), e.getTrack());
        } else if (TextUtils.isEmpty(e.getArtist())) {
            // Only track title
            artistTrackString = e.getTrack();
        } else if (TextUtils.isEmpty(e.getTrack())){
            // Only artist
            artistTrackString = e.getArtist();
        } else {
            // Neither artist nor track
            artistTrackString = context.getString(R.string.empty_string);
        }
        return artistTrackString;
    }

    public static Notification bufferingNotification(Context context) {
        return kfjcNotification(context,
                context.getString(R.string.app_name),
                context.getString(R.string.buffering_format,
                        PreferenceControl.getStreamNamePreference()));
    }

    public static Notification kfjcNotification(Context context, String title, String text) {
        PendingIntent kfjcPlayerIntent = PendingIntent.getActivity(
                context, 0,
                new Intent(context, HomeScreenDrawerActivity.class),
                Notification.FLAG_ONGOING_EVENT);

        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_kfjc_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setWhen(0)
                .setContentIntent(kfjcPlayerIntent)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();
    }

    public void postNotification(String title, String text) {
        notificationManager.notify(
                KFJC_NOTIFICATION_ID, kfjcNotification(context, title, text));
    }

    public void cancelNowPlayNotification() {
        notificationManager.cancel(KFJC_NOTIFICATION_ID);
    }
}
