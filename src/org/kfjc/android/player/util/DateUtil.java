package org.kfjc.android.player.util;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.fragment.PodcastRecyclerAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {

    private static final int SECONDS_IN_HOUR = 3600;
    private static final int SECONDS_IN_HALF_HOUR = 1800;

    public static final SimpleDateFormat FORMAT_H_MM = new SimpleDateFormat("h:mm a");
    public static final SimpleDateFormat FORMAT_DELUXE_DATE = new SimpleDateFormat("ha, EEEE d MMMM yyyy");
    public static final SimpleDateFormat FORMAT_FULL_DATE = new SimpleDateFormat("ha EEE d MMM yyyy");
    public static final SimpleDateFormat FORMAT_SHORT_DATE = new SimpleDateFormat("ha EEE d MMM");

    public static long roundToNearestHour(long timestampSec) {
        return roundDownHour(timestampSec + SECONDS_IN_HALF_HOUR);
    }

    public static long roundDownHour(long timestampSec) {
        return timestampSec + SECONDS_IN_HOUR - remainderToHour(timestampSec);
    }

    private static long remainderToHour(long timestampSec) {
        long remainder = timestampSec % SECONDS_IN_HOUR;
        return SECONDS_IN_HOUR - remainder;
    }

    public static String format(long timestamp, SimpleDateFormat df) {
        df.setTimeZone(Constants.BROADCAST_TIMEZONE);
        return df.format(new Date(timestamp * 1000));
    }

    public static String roundDownHourFormat(long timestamp, SimpleDateFormat df) {
        df.setTimeZone(Constants.BROADCAST_TIMEZONE);
        return df.format(new Date(DateUtil.roundDownHour(timestamp) * 1000));
    }

    /**
     * Returns elapsed time in milliseconds as a string H:mm:ss.
     */
    public static String formatTime(long timeMillis) {
        long timeMillisRoundedToSecond = (long)(timeMillis/1000.0 +.5) * 1000;
        boolean isNegativeTime = false;
        if (timeMillisRoundedToSecond < 0) {
            isNegativeTime = true;
            timeMillisRoundedToSecond = Math.abs(timeMillisRoundedToSecond);
        }

        Date timestamp = new Date(timeMillisRoundedToSecond);

        SimpleDateFormat hours = new SimpleDateFormat("H");
        SimpleDateFormat minutes = new SimpleDateFormat("mm");
        SimpleDateFormat seconds = new SimpleDateFormat("ss");

        TimeZone tz = TimeZone.getTimeZone("UTC");
        hours.setTimeZone(tz);
        minutes.setTimeZone(tz);
        seconds.setTimeZone(tz);

        String hoursStr = hours.format(timestamp);
        String minutesStr = minutes.format(timestamp);
        String secondsStr = seconds.format(timestamp);

        StringBuilder sb = new StringBuilder();
        if (isNegativeTime) {
            sb.append("-");
        }
        if (Integer.parseInt(hoursStr) > 0) {
            sb.append(hoursStr + ":");
        }
        sb.append(minutesStr + ":");
        sb.append(secondsStr);
        return sb.toString();
    }

}
