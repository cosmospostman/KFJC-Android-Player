package org.kfjc.android.player.util;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import android.text.TextUtils;

import org.kfjc.android.player.R;
import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.intent.PlayerControl;
import org.kfjc.android.player.model.KfjcMediaSource;
import org.kfjc.android.player.model.Playlist;

public class NotificationUtil {

    public static final int KFJC_NOTIFICATION_ID = 1;
    private static final String KFJC_NOTIFICATION_CHANNEL_ID = "org.kfjc.android.player";

    private Context context;
    private static NotificationManager notificationManager;
    private static Bitmap icon;

    public NotificationUtil(Context context) {
        this.context = context;
        notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.radiodevil);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
    }

    @TargetApi(26)
    private void createNotificationChannel() {
        NotificationChannel notificationChannel =
                new NotificationChannel(KFJC_NOTIFICATION_CHANNEL_ID, "default", NotificationManager.IMPORTANCE_LOW);
        notificationChannel.setShowBadge(false);
        notificationManager.createNotificationChannel(notificationChannel);
    }

    public void updateNowPlayNotification(Playlist playlist, KfjcMediaSource source) {
        if (playlist == null) {
            return;
        }
        NotificationCompat.Builder builder = kfjcBaseNotification(
                context, PlayerControl.notificationIntent(context, source), PlayerControl.INTENT_STOP);
        if (playlist.hasError()) {
            cancelKfjcNotification();
            builder.setContentTitle(context.getString(R.string.app_name));
            builder.setContentText(context.getString(R.string.empty_string));
            notificationManager.notify(KFJC_NOTIFICATION_ID, builder.build());
        } else {
            builder.setContentTitle(playlist.getDjName());
            builder.setContentText(artistTrackStringNotification(playlist.getLastTrackEntry()));
            notificationManager.notify(KFJC_NOTIFICATION_ID, builder.build());
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

    private static NotificationCompat.Builder kfjcBaseNotification(Context context, Intent i, String action) {
        PendingIntent kfjcPlayerIntent = PlayerControl.playerIntent(context, i);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, KFJC_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_kfjc_notification)
                .setLargeIcon(icon)
                .setOngoing(true)
                .setWhen(0)
                .setContentIntent(kfjcPlayerIntent)
                .setPriority(Notification.PRIORITY_HIGH);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            // Should instead build action with Icon.fromResource (but only for Api 23+)
            builder.addAction(R.drawable.ic_stop_white_48dp,
                    context.getString(R.string.action_stop),
                    PlayerControl.controlPendingIntent(context, PlayerControl.INTENT_STOP));
            if (action.equals(PlayerControl.INTENT_STOP)) {
                builder.setStyle(new MediaStyle()
                        .setShowActionsInCompactView(0));
            } else if (action.equals(PlayerControl.INTENT_PAUSE)) {
                builder.addAction(R.drawable.ic_pause_white_48dp,
                        context.getString(R.string.action_pause),
                        PlayerControl.controlPendingIntent(context, PlayerControl.INTENT_PAUSE));
                builder.setStyle(new MediaStyle()
                        .setShowActionsInCompactView(0, 1));
            } else if (action.equals(PlayerControl.INTENT_UNPAUSE)) {
                builder.addAction(R.drawable.ic_play_arrow_white_48dp,
                        context.getString(R.string.action_play),
                        PlayerControl.controlPendingIntent(context, PlayerControl.INTENT_UNPAUSE));
                builder.setStyle(new MediaStyle()
                        .setShowActionsInCompactView(0, 1));
            }
        }
        return builder;
    }

    public static Notification kfjcStreamNotification(Context context, KfjcMediaSource source, String action, boolean isBuffering) {
        Intent i = PlayerControl.notificationIntent(context, source);
        NotificationCompat.Builder builder = kfjcBaseNotification(context, i, action);
        if (isBuffering) {
            builder.setContentTitle(context.getString(R.string.app_name));
            builder.setContentText(context.getString(R.string.format_buffering,
                    PreferenceControl.getStreamPreference().description));
        }
        else if (source.type == KfjcMediaSource.Type.LIVESTREAM) {
            builder.setContentTitle(context.getString(R.string.fragment_title_stream));
            builder.setContentText("");
        } else if (source.type == KfjcMediaSource.Type.ARCHIVE) {
            builder.setContentTitle(source.show.getAirName());
            builder.setContentText(source.show.getTimestampString());
        }
        return builder.build();
    }

    public static void cancelKfjcNotification() {
        if (notificationManager != null) {
            notificationManager.cancel(KFJC_NOTIFICATION_ID);
        }
    }
}
