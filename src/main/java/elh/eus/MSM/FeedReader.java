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
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.naming.NamingException;

import org.xml.sax.SAXException;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.CommonExtractors;
import de.l3s.boilerpipe.sax.HtmlArticleExtractor;


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
	private Connection DBconn;
	private List<Keyword> kwrds;
	private List<Keyword> independentkwrds;
	private List<Keyword> dependentkwrds;
	
	private static Pattern anchorPattern; //pattern for anchor kwrds. they are usually general terms.
	private HashMap<Integer,Pattern> kwrdPatterns; //patterns for keywords.

	


	/*public URL getFeedUrl() {
		return feedUrl;
	}

	public void setFeedUrl(URL feedUrl) {
		this.feedUrl = feedUrl;
	}*/

	public FeedReader(String source) {
		
		//Language identification
		loadAcceptedLangs(params.getProperty("langs", "all"));
		
		try {
			kwrds = Keyword.retrieveFromDB(DBconn, "press", params.getProperty("langs", "all"));
			constructKeywordsPatterns();
			
		}catch (Exception e)
		{
			System.err.println("elh-MSM::FeedReader - DB Error when trying to load keywords");
			System.exit(1);
		}
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

	/**
	 * This void creates Patterns for all the keywords and stores them in two structures depending if the keywords are anchors or not.
	 * @param kwrds2
	 */
	
	private void constructKeywordsPatterns() {
		if (this.kwrds == null || this.kwrds.isEmpty())
		{
			System.err.println ("elh-MSM::FeedReader - No keywords loaded");
			System.exit(1);
		}

		StringBuilder sb_anchors = new StringBuilder();
		sb_anchors.append("(?i)\\b(");
		for (Keyword k : kwrds)
		{
			//create and store pattern;
			Pattern p = Pattern.compile("(?i)\\b"+k.getText().replace("_", " ")); 
			kwrdPatterns.put(k.getId(), p);
			if (k.isAnchor())
			{
				sb_anchors.append(k.getText().replace("_", " ")).append("|"); 
			}
			else
			{
				if (k.needsAnchor())
				{
					independentkwrds.add(k);
				}
				else	
				{
					dependentkwrds.add(k);
				}
			}
		} 
		String anchPatt = sb_anchors.toString();
		anchPatt=anchPatt.substring(0, anchPatt.length()-1)+")";
		anchorPattern = Pattern.compile(anchPatt);
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

		//keyword loading keywords identify relevant mentions in articles.
		try {
			kwrds = Keyword.retrieveFromDB(DBconn, "press", params.getProperty("langs", "all"));
		}catch (Exception e)
		{
			System.err.println("elh-MSM::FeedReader - DB Error when trying to load keywords");
			System.exit(1);
		}

		
		//feed sources
		// * try to load them from the config file
		String source = params.getProperty("feedURL", "none");
		if (source.equalsIgnoreCase("none"))
		{
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
		else
		{
			// * if not in the config file 
			try
			{
				DBconn = Utils.DbConnection(params.getProperty("dbuser"),params.getProperty("dbpass"),params.getProperty("dbhost"),params.getProperty("dbname"));

				// our SQL SELECT query. 
				String query = "SELECT * FROM "
						+ "behagunea_app_source JOIN behagunea_app_feed "
						+ "ON behagunea_app_source.source_id=behagunea_app_feed.source_id "
						+ "WHERE behagunea_app_source.type='feed'";

				Statement st = DBconn.createStatement();			       
				// execute the query, and get a java resultset
				ResultSet rs = st.executeQuery(query);

				// iterate through the java resultset
				while (rs.next())
				{
					int id = rs.getInt("id");
					String url = rs.getString("url");
					String lastFetch = rs.getString("last_fetch");
					String langs = rs.getString("lang");
					try {
						URL feedUrl = new URL(url);
						getFeed(feedUrl, lastFetch, langs, id);
					} catch (MalformedURLException ue ) {
						System.err.println("MSM::FeedReader - ERROR: malformed source url given"+ue.getMessage());
					}		
				}
				st.close();			

			} catch (SQLException | NamingException sqle){
				System.err.println("elh-MSM::FeedReader - DB Error when retrieving feed sources");
				System.exit(1);
			}
		}		
	}//end constructor

		private void getFeed (URL url, String lastFetchDate, String langs, int sId){

			boolean ok = false;
			String link = "";

			//Language identification
			loadAcceptedLangs(langs);
			
			try {
				SyndFeedInput input = new SyndFeedInput();
				SyndFeed feed = input.build(new XmlReader(url));	

				for (SyndEntry entry : feed.getEntries())
				{
					link = entry.getLink();		
					URL linkSrc = new URL(link);
					String date = entry.getPublishedDate().toString();

					if (date.compareToIgnoreCase(lastFetchDate)>0)
					{
						final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;

						final HtmlArticleExtractor htmlExtr = HtmlArticleExtractor.INSTANCE;
						
						String text = htmlExtr.process(extractor, linkSrc);
						text = text.replaceAll("(?i)<p>", "").replaceAll("(?i)</p>", "\n\n").replaceAll("(?i)<br\\/?>","\n");

						// Hemen testuan gako hitzak bilatzeko kodea falta da, eta topatuz gero
						// aipamen bat sortu eta datubasera sartzea.
						String lang = LID.detectLanguage(text, langs);
						
						parseArticleForKeywords(text,lang, entry.getPublishedDate(), link, sId);
						
					}
					
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
			} catch (BoilerpipeProcessingException | SAXException
					| URISyntaxException be) {
				be.printStackTrace();
				System.out.println("FeadReader::getFeed ->  Boilerplate removal ERROR with"+url.toString()+" : "+be.getMessage());
			}
			if (!ok) {
				System.out.println();
				System.out.println("FeedReader reads and prints any RSS/Atom feed type.");
				System.out.println("The first parameter must be the URL of the feed to read.");
				System.out.println();
			}	
		}
		
		
	/**
	 * @param text
	 */
	private void parseArticleForKeywords(String text, String lang, Date date, String link, int sId) {
		
		List<Keyword> result = new ArrayList<Keyword>();
		boolean anchorFound = anchorPattern.matcher(text).matches();
		
		String[] paragraphs = text.split("\n+");
		for (String par : paragraphs)
		{
			//Mention m = new Mention();
			//keywords that do not need any anchor
			for (Keyword k : independentkwrds)
			{
				if (k.getLang().equalsIgnoreCase(lang) && kwrdPatterns.get(k.getId()).matcher(par).matches())
				{
					result.add(k);
				}
			}			
			//keywords that need and anchor, only if anchors where found
			if (anchorFound)
			{
				for (Keyword k : dependentkwrds)
				{
					if (k.getLang().equalsIgnoreCase(lang) && kwrdPatterns.get(k.getId()).matcher(par).matches())
					{
						result.add(k);
					}
				}
			}
			
			if (result != null && !result.isEmpty())
			{
				Mention m = new Mention(lang,text,date,link,String.valueOf(sId));
				m.setKeywords(result);
			}
		}				
	}

	/**
	 *  Function retrieves a feed and processes it (either stores in the database or prints to the stdout
	 * 
	 * @param url
	 * @Deprecated	
	 */
	private void getFeed (URL url){

		boolean ok = false;
		String link = "";
		try {
			SyndFeedInput input = new SyndFeedInput();
			SyndFeed feed = input.build(new XmlReader(url));	

			for (SyndEntry entry : feed.getEntries())
			{
				link = entry.getLink();

				final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;

				final HtmlArticleExtractor htmlExtr = HtmlArticleExtractor.INSTANCE;
				
				String text = htmlExtr.process(extractor, new URL(link));
				text = text.replaceAll("(?i)<p>", "").replaceAll("(?i)</p>", "\n\n").replaceAll("(?i)<br\\/?>","\n");
				
				//Document doc = Jsoup.connect(link).get();
				//Cleaner clean = new Cleaner(Whitelist.none().addTags("br","p"));
				//String text = clean.clean(doc).text().replaceAll("<p>", "").replaceAll("</p>", "\n\n").replaceAll("<br\\/?>","\n");
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
		} catch (BoilerpipeProcessingException | SAXException
				| URISyntaxException be) {
			be.printStackTrace();
			System.out.println("FeadReader::getFeed ->  Boilerplate removal ERROR with"+url.toString()+" : "+be.getMessage());
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
