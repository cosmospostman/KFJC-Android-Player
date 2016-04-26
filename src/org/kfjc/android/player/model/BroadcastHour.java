package org.kfjc.android.player.model;

/** Example input:
 *  { "air_name":"Abacus Finch",
 *    "show_date":"2016-04-25",
 *    "start_hour":16,
 *    "url":"http:\/\/archive.kfjc.org\/archives\/1604251553h_abacus_finch.mp3",
 *    "playlist_num":51251 }
 */
public interface BroadcastHour {
    String getAirName();
    int getStartHour();
    String getUrl();
    String getPlaylistId();
}
