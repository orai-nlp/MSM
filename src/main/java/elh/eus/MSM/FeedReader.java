/*
 * Copyright 2015 Elhuyar Fundazioa

This file is part of MSM.

    MSM is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    MSM is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MSM.  If not, see <http://www.gnu.org/licenses/>.
 */

package elh.eus.MSM;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;

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
			
			//System.out.println(feed);

			for (SyndEntry entry : feed.getEntries())
			{
				String link = entry.getLink();						
				StringBuilder sb = new StringBuilder();	
				for (SyndContent content : entry.getContents())
				{				
					sb.append(Jsoup.clean(content.getValue(), Whitelist.none().addTags("br","p")));
				}
				String text = sb.toString().replaceAll("<p>", "").replaceAll("</p>", "\n\n").replaceAll("<br\\/?>","\n");
				System.out.println("-------------\n"
						+ "link : "+link);
				System.out.println("\n"
						+ "content:\n"+text+"\n-------------");

				// Hemen testuan gako hitzak bilatzeko kodea falta da, eta topatuz gero
				// aipamen bat sortu eta datubasera sartzea.
				
			}
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
