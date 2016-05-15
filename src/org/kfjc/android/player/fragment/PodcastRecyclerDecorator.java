package org.kfjc.android.player.fragment;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;

public class PodcastRecyclerDecorator extends RecyclerView.ItemDecoration {

    private static int spacePx;
    private static int endPx;

    public PodcastRecyclerDecorator(Context c) {
        spacePx = convertDpToPixel(2, c);
        endPx = convertDpToPixel(16, c);
    }

    public static int convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return (int) px;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        // First
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.left = endPx;
            outRect.right = spacePx;
        }
        // Last
        else if (parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() - 1) {
            outRect.left = spacePx;
            outRect.right = endPx;
        }
        // Middle
        else {
            outRect.left = spacePx;
            outRect.right = spacePx;
        }
    }
}
