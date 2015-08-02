package org.kfjc.android.player.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import org.kfjc.android.player.activity.HomeScreenActivity;
import org.kfjc.android.player.model.TrackInfo;
import org.kfjc.droid.R;

public class NotificationUtil {

    public static final int KFJC_NOTIFICATION_ID = 1;

    private Context context;
    private NotificationManager notificationManager;

    public NotificationUtil(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void updateNowPlayNotification(TrackInfo nowPlaying) {
        if (nowPlaying.getCouldNotFetch()) {
            postNotification(
                    context.getString(R.string.app_name),
                    context.getString(R.string.status_not_connected));
        } else {
            String artistTrackString = context.getString(R.string.artist_track_format,
                    nowPlaying.getArtist(), nowPlaying.getTrackTitle());
            postNotification(nowPlaying.getDjName(), artistTrackString);
        }
    }

    public static Notification kfjcNotification(Context context, String title, String text) {
        PendingIntent kfjcPlayerIntent = PendingIntent.getActivity(
                context, 0,
                new Intent(context, HomeScreenActivity.class),
                Notification.FLAG_ONGOING_EVENT);

        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_kfjc_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setWhen(0)
                .setContentIntent(kfjcPlayerIntent)
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
