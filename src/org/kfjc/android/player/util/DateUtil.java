package org.kfjc.android.player.util;

public class DateUtil {

    private static final int SECONDS_IN_HOUR = 3600;

    public static long roundUpHour(long timestampSec) {
        long remainder = timestampSec % SECONDS_IN_HOUR;
        long timeToAdd = SECONDS_IN_HOUR - remainder;
        return timestampSec + timeToAdd;
    }

}
