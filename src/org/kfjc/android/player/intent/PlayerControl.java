package org.kfjc.android.player.intent;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.kfjc.android.player.activity.HomeScreenDrawerActivity;
import org.kfjc.android.player.model.KfjcMediaSource;
import org.kfjc.android.player.service.StreamService;

public class PlayerControl {

    public static final String INTENT_SOURCE = "media_source";

    public static final String INTENT_STOP = "action_stop";
    public static final String INTENT_PAUSE = "action_pause";
    public static final String INTENT_PLAY = "action_play";
    public static final String INTENT_UNPAUSE = "action_unpause";

    public static Intent notificationIntent(Context context, KfjcMediaSource source) {
        Intent i = new Intent(context, HomeScreenDrawerActivity.class);
        i.putExtra(INTENT_SOURCE, source);
        return i;
    }

    public static PendingIntent playerIntent(Context context, Intent intent) {
        return PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static Intent controlIntent(Context context, String action) {
        return new Intent(action, null, context, StreamService.class);
    }

    static Intent controlIntent(Context context, String action, KfjcMediaSource source) {
        Intent i = new Intent(action, null, context, StreamService.class);
        i.putExtra(PlayerControl.INTENT_SOURCE, source);
        return i;
    }

    public static void sendAction(Activity activity, String action) {
        activity.startService(PlayerControl.controlIntent(activity, action));
    }

    public static void sendAction(Activity activity, String action, KfjcMediaSource source) {
        activity.startService(PlayerControl.controlIntent(activity, action, source));
    }

    public static PendingIntent controlPendingIntent(Context context, String action) {
        return PendingIntent.getService(context, 1, controlIntent(context, action),
                PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
