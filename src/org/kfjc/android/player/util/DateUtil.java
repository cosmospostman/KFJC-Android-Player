package org.kfjc.android.player.util;

import org.kfjc.android.player.Constants;
import org.kfjc.android.player.fragment.PodcastRecyclerAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {

    private static final int SECONDS_IN_HOUR = 3600;

    public static final SimpleDateFormat FORMAT_HH_MM = new SimpleDateFormat("hh:mm a");
    public static final SimpleDateFormat FORMAT_DELUXE_DATE = new SimpleDateFormat("ha, EEEE d MMMM yyyy");
    public static final SimpleDateFormat FORMAT_FULL_DATE = new SimpleDateFormat("ha EEE d MMM yyyy");
    public static final SimpleDateFormat FORMAT_SHORT_DATE = new SimpleDateFormat("ha EEE d MMM");

    public static long roundUpHour(long timestampSec) {
        long remainder = timestampSec % SECONDS_IN_HOUR;
        long timeToAdd = SECONDS_IN_HOUR - remainder;
        return timestampSec + timeToAdd;
    }

    public static String format(long timestamp, SimpleDateFormat df) {
        df.setTimeZone(Constants.BROADCAST_TIMEZONE);
        return df.format(new Date(timestamp * 1000));
    }

    public static String roundHourFormat(long timestamp, SimpleDateFormat df) {
        df.setTimeZone(Constants.BROADCAST_TIMEZONE);
        return df.format(new Date(DateUtil.roundUpHour(timestamp) * 1000));
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
