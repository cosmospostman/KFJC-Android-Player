package org.kfjc.android.player.util;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.text.TextUtils;

import org.kfjc.android.player.activity.HomeScreenDrawerActivity;
import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.R;
import org.kfjc.android.player.service.StreamService;

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
                    context.getString(R.string.format_artist_track, e.getArtist(), e.getTrack());
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
                context.getString(R.string.format_buffering,
                        PreferenceControl.getStreamPreference().description));
    }

    public static Notification kfjcNotification(Context context, String title, String text) {
        Intent i = new Intent(context, HomeScreenDrawerActivity.class);
        i.putExtra(HomeScreenDrawerActivity.INTENT_FROM_NOTIFICATION, true);
        PendingIntent kfjcPlayerIntent = PendingIntent.getActivity(
                context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.radiodevil);
        Notification.Builder builder = new Notification.Builder(context)
            .setSmallIcon(R.drawable.ic_kfjc_notification)
            .setLargeIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setWhen(0)
            .setContentIntent(kfjcPlayerIntent)
            .setPriority(Notification.PRIORITY_HIGH);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Should instead build action with Icon.fromResource (but only for Api 23+)
            builder
                .addAction(R.drawable.ic_stop_white_48dp,
                        context.getString(R.string.action_stop), buildStopIntent(context))
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(new Notification.MediaStyle()
                    .setShowActionsInCompactView(0));
        }
        return builder.build();
    }

    private static PendingIntent buildStopIntent(Context context) {
        Intent stopIntent = new Intent(StreamService.INTENT_STOP);
        return PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_ONE_SHOT);
    }

    public void postNotification(String title, String text) {
        notificationManager.notify(
                KFJC_NOTIFICATION_ID, kfjcNotification(context, title, text));
    }

    public void cancelNowPlayNotification() {
        notificationManager.cancel(KFJC_NOTIFICATION_ID);
    }
}
