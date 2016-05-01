package org.kfjc.android.player.receiver;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.kfjc.android.player.activity.HomeScreenDrawerActivity;

public class DownloadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startIntent = new Intent(context, HomeScreenDrawerActivity.class);
        startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startIntent.putExtra(HomeScreenDrawerActivity.INTENT_DOWNLOAD_IDS,
                intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS));
        context.startActivity(startIntent);
    }
}
