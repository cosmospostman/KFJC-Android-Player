package org.kfjc.android.player.model;

import android.graphics.drawable.Drawable;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

public interface Resources {
    void loadResources();
    ListenableFuture<List<MediaSource>> getStreamsList();
    ListenableFuture<Drawable> getBackgroundImage(int hourOfDay);
    ListenableFuture<String> getLavaUrl();
}
