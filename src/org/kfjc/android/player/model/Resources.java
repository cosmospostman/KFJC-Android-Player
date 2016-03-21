package org.kfjc.android.player.model;


import android.graphics.drawable.Drawable;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Map;

public interface Resources {
    ListenableFuture<Map<String, String>> getStreams();
    ListenableFuture<Drawable> getBackgroundImage(int hourOfDay);
}
