package elh.eus.MSM;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

/**
 * RSS/Atom feed reader.
 * 
 * @author Iñaki San Vicente
 *
 */
public class FeedReader {

	private Properties params = new Properties();	
	private URL feedUrl;

	public URL getFeedUrl() {
		return feedUrl;
	}

	public void setFeedUrl(URL feedUrl) {
		this.feedUrl = feedUrl;
	}

	public FeedReader(String source) {
		try {
			setFeedUrl(new URL(source));
		} catch (MalformedURLException ue ) {
			System.err.println("MSM::FeedReader - ERROR: malformed source url given"+ue.getMessage());
		} 

		getFeed();
	}

	public FeedReader(String config, String store) {
		try {
			params.load(new FileInputStream(new File(config)));
		} catch (FileNotFoundException fe){
			System.err.println("elh-MSM::FeedReader - Config file not found "+config);
			System.exit(1);
		} catch (IOException ioe){
			System.err.println("elh-MSM::FeedReader - Config file could not read "+config);
			System.exit(1);
		} 

		String source = params.getProperty("feedURL", "none");
		
		try {
			setFeedUrl(new URL(source));
		} catch (MalformedURLException ue ) {
			System.err.println("MSM::FeedReader - ERROR: malformed source url given"+ue.getMessage());
		} 

		getFeed();
	}


	private void getFeed (){

		boolean ok = false;
		try {
			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = input.build(new XmlReader(getFeedUrl()));

			System.out.println(feed);

			ok = true;
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("ERROR: "+ex.getMessage());
		} 	        

		if (!ok) {
			System.out.println();
			System.out.println("FeedReader reads and prints any RSS/Atom feed type.");
			System.out.println("The first parameter must be the URL of the feed to read.");
			System.out.println();
		}	
	}




}
