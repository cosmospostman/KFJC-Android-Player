package org.kfjc.android.player.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.kfjc.android.player.activity.HomeScreenDrawerActivity;
import org.kfjc.android.player.model.MediaSource;

public class Intents {

    public static final String INTENT_FROM_NOTIFICATION = "fromNotification";
    public static final String INTENT_SOURCE = "fromNotification";
    public static final String INTENT_DOWNLOAD_CLICKED = "downloadClicked";
    public static final String INTENT_DOWNLOAD_IDS = "downloadIds";

    public static final String INTENT_ACTION = "intent_action";
    public static final String INTENT_STOP = "action_stop";
    public static final String INTENT_PAUSE = "action_pause";
    public static final String INTENT_UNPAUSE = "action_unpause";

    static Intent notificationIntent(Context context) {
        Intent i = new Intent(context, HomeScreenDrawerActivity.class);
        i.putExtra(INTENT_FROM_NOTIFICATION, true);
        return i;
    }

    static Intent notificationIntent(Context context, MediaSource source) {
        Intent i = notificationIntent(context);
        i.putExtra(INTENT_SOURCE, source);
        return i;
    }

    static PendingIntent playerIntent(Context context, Intent intent) {
        return PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static PendingIntent controlIntent(Context context, String action) {
        Intent intent = new Intent(action);
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_UPDATE_CURRENT);
//        Intent intent = new Intent(context, HomeScreenDrawerActivity.class);
//        intent.putExtra(INTENT_ACTION, action);
//        return PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);
    }
}
