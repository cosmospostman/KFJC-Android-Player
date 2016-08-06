package org.kfjc.android.player.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.text.TextUtils;

import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.model.MediaSource;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.R;

public class NotificationUtil {

    public static final int KFJC_NOTIFICATION_ID = 1;

    private Context context;
    private static NotificationManager notificationManager;
    private static Bitmap icon;

    public NotificationUtil(Context context) {
        this.context = context;
        notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.radiodevil);
    }

    public void updateNowPlayNotification(Playlist playlist) {
        if (playlist == null) {
            return;
        }
        Notification.Builder builder = kfjcBaseNotification(
                context, Intents.notificationIntent(context), Intents.INTENT_STOP);
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

    private static Notification.Builder kfjcBaseNotification(Context context, Intent i, String action) {
        PendingIntent kfjcPlayerIntent = Intents.playerIntent(context, i);

        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_kfjc_notification)
                .setLargeIcon(icon)
                .setOngoing(true)
                .setWhen(0)
                .setContentIntent(kfjcPlayerIntent)
                .setPriority(Notification.PRIORITY_HIGH);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            if (action.equals(Intents.INTENT_STOP)) {
                // Should instead build action with Icon.fromResource (but only for Api 23+)
                builder.addAction(R.drawable.ic_stop_white_48dp,
                        context.getString(R.string.action_stop),
                        Intents.controlIntent(context, Intents.INTENT_STOP));
                builder.setStyle(new Notification.MediaStyle()
                        .setShowActionsInCompactView(0));
            } else if (action.equals(Intents.INTENT_PAUSE)) {
                builder.addAction(R.drawable.ic_stop_white_48dp,
                        context.getString(R.string.action_stop),
                        Intents.controlIntent(context, Intents.INTENT_STOP));
                builder.addAction(R.drawable.ic_pause_white_48dp,
                        context.getString(R.string.action_pause),
                        Intents.controlIntent(context, Intents.INTENT_PAUSE));
                builder.setStyle(new Notification.MediaStyle()
                        .setShowActionsInCompactView(0, 1));
            }else if (action.equals(Intents.INTENT_UNPAUSE)) {
                builder.addAction(R.drawable.ic_stop_white_48dp,
                        context.getString(R.string.action_stop),
                        Intents.controlIntent(context, Intents.INTENT_STOP));
                builder.addAction(R.drawable.ic_play_arrow_white_48dp,
                        context.getString(R.string.action_play),
                        Intents.controlIntent(context, Intents.INTENT_UNPAUSE));
                builder.setStyle(new Notification.MediaStyle()
                        .setShowActionsInCompactView(0, 1));
            }
        }
        return builder;
    }

    public Notification bufferingNotification(Context context) {
        Notification.Builder builder = kfjcBaseNotification(
                context, Intents.notificationIntent(context), Intents.INTENT_STOP);
        builder.setContentTitle(context.getString(R.string.app_name));
        builder.setContentText(context.getString(R.string.format_buffering,
                PreferenceControl.getStreamPreference().description));
        return builder.build();
    }

    public static Notification kfjcStreamNotification(Context context, MediaSource source, String action) {
        Intent i = Intents.notificationIntent(context, source);
        Notification.Builder builder = kfjcBaseNotification(context, i, action);

        if (source.type == MediaSource.Type.LIVESTREAM) {
            builder.setContentTitle(context.getString(R.string.fragment_title_stream));
            builder.setContentText("");
        } else if (source.type == MediaSource.Type.ARCHIVE) {
            builder.setContentTitle(source.show.getAirName());
            builder.setContentText(source.show.getTimestampString());
        }

        return builder.build();
    }

    public static void cancelKfjcNotification() {
        notificationManager.cancel(KFJC_NOTIFICATION_ID);
    }
}
