package org.kfjc.android.player.receiver;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.kfjc.android.player.activity.HomeScreenDrawerActivity;
import org.kfjc.android.player.util.Intents;

public class DownloadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startIntent = new Intent(context, HomeScreenDrawerActivity.class);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        startIntent.putExtra(Intents.INTENT_DOWNLOAD_IDS,
                intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS));
        context.startActivity(startIntent);
    }
}
