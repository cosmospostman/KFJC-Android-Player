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

import org.kfjc.android.player.activity.HomeScreenDrawerActivity;
import org.kfjc.android.player.control.PreferenceControl;
import org.kfjc.android.player.model.Playlist;
import org.kfjc.android.player.R;
import org.kfjc.android.player.service.StreamService;

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
        if (playlist.hasError()) {
            cancelKfjcNotification();
            postNotification(
                    context.getString(R.string.app_name),
                    context.getString(R.string.empty_string),
                    StreamService.INTENT_STOP);
        } else {
            postNotification(
                    playlist.getDjName(),
                    artistTrackStringNotification(playlist.getLastTrackEntry()),
                    StreamService.INTENT_STOP);
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

    public Notification bufferingNotification(Context context) {
        return kfjcNotification(context,
                context.getString(R.string.app_name),
                context.getString(R.string.format_buffering,
                        PreferenceControl.getStreamPreference().description),
                StreamService.INTENT_STOP);
    }

    public Notification kfjcNotification(Context context, String title, String text, String action) {
        Intent i = new Intent(context, HomeScreenDrawerActivity.class);
        i.putExtra(HomeScreenDrawerActivity.INTENT_FROM_NOTIFICATION, true);
        PendingIntent kfjcPlayerIntent = PendingIntent.getActivity(
                context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

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
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            if (action.equals(StreamService.INTENT_STOP)) {
                // Should instead build action with Icon.fromResource (but only for Api 23+)
                builder.addAction(R.drawable.ic_stop_white_48dp,
                        context.getString(R.string.action_stop), buildControlIntent(context, StreamService.INTENT_STOP));
                builder.setStyle(new Notification.MediaStyle()
                        .setShowActionsInCompactView(0));
            } else if (action.equals(StreamService.INTENT_PAUSE)) {
                builder.addAction(R.drawable.ic_stop_white_48dp,
                        context.getString(R.string.action_stop), buildControlIntent(context, StreamService.INTENT_STOP));
                builder.addAction(R.drawable.ic_pause_white_48dp,
                        context.getString(R.string.action_pause), buildControlIntent(context, StreamService.INTENT_PAUSE));
                builder.setStyle(new Notification.MediaStyle()
                        .setShowActionsInCompactView(0, 1));
            }else if (action.equals(StreamService.INTENT_UNPAUSE)) {
                builder.addAction(R.drawable.ic_play_arrow_white_48dp,
                        context.getString(R.string.action_play), buildControlIntent(context, StreamService.INTENT_UNPAUSE));
                builder.setStyle(new Notification.MediaStyle()
                        .setShowActionsInCompactView(0));
            }
        }
        return builder.build();
    }

    private static PendingIntent buildControlIntent(Context context, String action) {
        Intent intent = new Intent(action);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void postNotification(String title, String text, String action) {
        notificationManager.notify(
                KFJC_NOTIFICATION_ID, kfjcNotification(context, title, text, action));
    }

    public void cancelKfjcNotification() {
        notificationManager.cancel(KFJC_NOTIFICATION_ID);
    }
}
