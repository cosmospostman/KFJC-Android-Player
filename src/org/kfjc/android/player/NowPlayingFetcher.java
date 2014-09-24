package org.kfjc.android.player;

import android.os.AsyncTask;

public class NowPlayingFetcher extends AsyncTask<Void, Void, NowPlayingInfo> {

	private static final String METADATA = "http://kfjc.org/api/playlists/current.php";
	private NowPlayingHandler handler;
	
	NowPlayingFetcher(NowPlayingHandler handler) {
		this.handler = handler;		
	}
	
	public interface NowPlayingHandler {
		public void onTrackInfoFetched(NowPlayingInfo trackInfo);
	}

    protected NowPlayingInfo doInBackground(Void... unusedParams) {
    	return new NowPlayingInfo(HttpUtil.getUrl(METADATA));
    }

    protected void onPostExecute(NowPlayingInfo nowPlaying) {
        handler.onTrackInfoFetched(nowPlaying);
    }
}
