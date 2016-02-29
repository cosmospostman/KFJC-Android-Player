package org.kfjc.android.player.model;


import android.graphics.Bitmap;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Map;

public interface Resources {
    ListenableFuture<Map<String, String>> getStreams();
    ListenableFuture<Bitmap> getBackgroundImage(int hourOfDay);
}
