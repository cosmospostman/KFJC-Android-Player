package org.kfjc.droid;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import net.moraleboost.streamscraper.ScrapeException;
import net.moraleboost.streamscraper.Scraper;
import net.moraleboost.streamscraper.Stream;
import net.moraleboost.streamscraper.scraper.ShoutCastScraper;
import android.os.AsyncTask;

public class TrackInfo extends AsyncTask<String, Void, String> {
	
	TrackInfoHandler handler;
	
	TrackInfo(TrackInfoHandler handler) {
		this.handler = handler;		
	}
	
	public interface TrackInfoHandler {
		public void onTrackInfoFetched(String trackInfo);
	}

    protected String doInBackground(String... urls) {
        Scraper scraper = new ShoutCastScraper();
    	List<Stream> streams;
		try {
			streams = scraper.scrape(new URI("http://netcast6.kfjc.org:80/"));
			for (Stream stream: streams) {
	            System.out.println("Song Title: " + stream.getCurrentSong());
	            System.out.println("URI: " + stream.getUri());
	        }
	    	return streams.get(0).getCurrentSong();
		} catch (ScrapeException e) {
		} catch (URISyntaxException e) {}
		return "Fallthrough";
    }

    protected void onPostExecute(String feed) {
        handler.onTrackInfoFetched(feed);
    }
}
