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
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;

/**
 * RSS/Atom feed reader.
 * 
 * @author Iñaki San Vicente
 *
 */
public class FeedReader {

	private Properties params = new Properties();	
	//private URL feedUrl;
	private LangDetect LID = new LangDetect();
	private List<String> acceptedLangs;


	/*public URL getFeedUrl() {
		return feedUrl;
	}

	public void setFeedUrl(URL feedUrl) {
		this.feedUrl = feedUrl;
	}*/

	public FeedReader(String source) {
		
		//Language identification
		loadAcceptedLangs(params.getProperty("langs", "all"));
				
		String[] urls = source.split(",");
		for (int i=0; i<urls.length;i++)
		{
			try {
				URL feedUrl = new URL(urls[i]);
				getFeed(feedUrl);
			} catch (MalformedURLException ue ) {
				System.err.println("MSM::FeedReader - ERROR: malformed source url given"+ue.getMessage());
			}		
		}
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

		//Language identification
		loadAcceptedLangs(params.getProperty("langs", "all"));
						
		String source = params.getProperty("feedURL", "none");
		
		String[] urls = source.split(",");
		for (int i=0; i<urls.length;i++)
		{
			try {
				URL feedUrl = new URL(urls[i]);
				getFeed(feedUrl);
			} catch (MalformedURLException ue ) {
				System.err.println("MSM::FeedReader - ERROR: malformed source url given"+ue.getMessage());
			}			
		}
	}


	private void getFeed (URL url){

		boolean ok = false;
		String link = "";
		try {
			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = input.build(new XmlReader(url));	

			for (SyndEntry entry : feed.getEntries())
			{
				link = entry.getLink();

				Document doc = Jsoup.connect(link).get();
				Cleaner clean = new Cleaner(Whitelist.none().addTags("br","p"));
				String text = clean.clean(doc).text().replaceAll("<p>", "").replaceAll("</p>", "\n\n").replaceAll("<br\\/?>","\n");
				/*
				 * Code using standard url library from java. 
				URL linkSource = new URL(link);
				BufferedReader in = new BufferedReader(
				        new InputStreamReader(linkSource.openStream()));
				
				StringBuilder sb = new StringBuilder();	
				String inputLine;
		        while ((inputLine = in.readLine()) != null)
		        {
		            sb.append(inputLine);
		        }
		        in.close();
				
		        String text = Jsoup.clean(sb.toString(), Whitelist.none().addTags("br","p")).replaceAll("<p>", "").replaceAll("</p>", "\n\n").replaceAll("<br\\/?>","\n");
		        */
				/*
				 * Old code to read the feed actual contents (usually snippets) 
				 * 
				StringBuilder sb = new StringBuilder();	
				
				for (SyndContent content : entry.getContents())
				{				
					sb.append(Jsoup.clean(content.getValue(), Whitelist.none().addTags("br","p")));
				}
				String text = sb.toString().replaceAll("<p>", "").replaceAll("</p>", "\n\n").replaceAll("<br\\/?>","\n");
				*/
				System.out.println("-------------\n"
						+ "link : "+link);
				System.out.println("\n"
						+ "content:\n"+text+"\n-------------");                

                // Hemen testuan gako hitzak bilatzeko kodea falta da, eta topatuz gero
				// aipamen bat sortu eta datubasera sartzea.
				
			}
			ok = true;
		} catch (MalformedURLException mue) {
			mue.printStackTrace();
			System.out.println("FeadReader::getFeed ->  ERROR: Malformed url when parsing a link"+mue.getMessage());
		} catch (IOException ioe) {	        
			ioe.printStackTrace();
			System.out.println("FeadReader::getFeed ->  ERROR when reading html a link ("+link+") - "+ioe.getMessage());
		} catch (FeedException fe) {	        
			fe.printStackTrace();
			System.out.println("FeadReader::getFeed ->  Feed ERROR with"+url.toString()+" : "+fe.getMessage());
		}
		if (!ok) {
			System.out.println();
			System.out.println("FeedReader reads and prints any RSS/Atom feed type.");
			System.out.println("The first parameter must be the URL of the feed to read.");
			System.out.println();
		}	
	}


	private void loadAcceptedLangs(String property) {
		this.acceptedLangs=Arrays.asList(property.split(","));	
		System.err.println("elh-MSM::TwitterStreamClient - Accepted languages: "+acceptedLangs);
	}

}
