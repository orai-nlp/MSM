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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.naming.NamingException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.apache.commons.lang3.StringUtils;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextBlock;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.CommonExtractors;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import de.l3s.boilerpipe.sax.HTMLFetcher;
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
	private Set<Keyword> kwrds;
	private Set<Keyword> independentkwrds = new HashSet<Keyword>();
	private Set<Keyword> dependentkwrds = new HashSet<Keyword>();
	
	private static Pattern anchorPattern; //pattern for anchor kwrds. they are usually general terms.
	private HashMap<Integer,Pattern> kwrdPatterns = new HashMap<Integer,Pattern>(); //patterns for keywords.

	


	/*public URL getFeedUrl() {
		return feedUrl;
	}

	public void setFeedUrl(URL feedUrl) {
		this.feedUrl = feedUrl;
	}*/

	public FeedReader(String source) {
		
		//Language identification
		loadAcceptedLangs(params.getProperty("langs", "all"));
		
		//keyword loading: keywords to identify relevant mentions in articles.
		try {
			DBconn = Utils.DbConnection(params.getProperty("dbuser"),params.getProperty("dbpass"),params.getProperty("dbhost"),params.getProperty("dbname"));
			kwrds = Keyword.retrieveFromDB(DBconn, "press", params.getProperty("langs", "all"));
			System.err.println("elh-MSM::FeedReader(config,store) - retrieved "+kwrds.size()+" keywords");

			// prepare patterns to match keywords
			constructKeywordsPatterns();

			DBconn.close();
		}catch (Exception e)
		{
			System.err.println("elh-MSM::FeedReader(config,store) - DB Error when trying to load keywords");
			e.printStackTrace();
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
		sb_anchors.append("\\b(");
		for (Keyword k : kwrds)
		{
			//create and store pattern;
			Pattern p = Pattern.compile("\\b"+k.getText().replace('_',' ').toLowerCase());
			System.err.println("elh-MSM::FeedReader::constructKeywordPatterns - currentPattern:"+p.toString());
			
			kwrdPatterns.put(k.getId(), p);
			if (k.isAnchor())
			{
				sb_anchors.append(k.getText().replace('_',' ').toLowerCase()).append("|"); 
			}
			else
			{
				if (k.needsAnchor())
				{
					dependentkwrds.add(k);
				}
				else	
				{
					independentkwrds.add(k);
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
		
		//keyword loading: keywords to identify relevant mentions in articles.
		try {
			DBconn = Utils.DbConnection(params.getProperty("dbuser"),params.getProperty("dbpass"),params.getProperty("dbhost"),params.getProperty("dbname"));
			kwrds = Keyword.retrieveFromDB(DBconn, "press", params.getProperty("langs", "all"));
			System.err.println("elh-MSM::FeedReader(config,store) - retrieved "+kwrds.size()+" keywords");
			
			// prepare patterns to match keywords
			constructKeywordsPatterns();
			DBconn.close();			
		}catch (Exception e)
		{
			System.err.println("elh-MSM::FeedReader(config,store) - DB Error when trying to load keywords");
			e.printStackTrace();
			System.exit(1);
		}

		
		//feed sources
		// * try to load them from the config file
		String source = params.getProperty("feedURL", "none");
		if (!source.equalsIgnoreCase("none"))
		{	
			try{
			DBconn = Utils.DbConnection(params.getProperty("dbuser"),params.getProperty("dbpass"),params.getProperty("dbhost"),params.getProperty("dbname"));
			
			String[] urls = source.split(",");
			for (int i=0; i<urls.length;i++)
			{
				try {
					URL feedUrl = new URL(urls[i]);
					getFeed(feedUrl);
				} catch (MalformedURLException ue ) {
					System.err.println("MSM::FeedReader - ERROR: malformed source url given - "+ue.getMessage()+" url: "+urls[i]);
				}			
			}
			DBconn.close();
			}catch (Exception e){
				e.printStackTrace();
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
						+ "WHERE behagunea_app_source.type='press'";

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
						System.err.println("MSM::FeedReader - ERROR: malformed source url given - "+ue.getMessage()+" url: "+url);
					}		
				}
				st.close();			
				DBconn.close();
			} catch (SQLException | NamingException sqle){
				System.err.println("elh-MSM::FeedReader - DB Error when retrieving feed sources");
				sqle.printStackTrace();
				System.exit(1);
			}
		}		
	}//end constructor

		private void getFeed (URL url, String lastFetchDate, String langs, int sId){

			System.err.println("FeadReader::getFeed -> parse feed "+url.toString());
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
					Date pubDate = entry.getPublishedDate();
					if (pubDate==null)
					{
						pubDate = feed.getPublishedDate();
						if (pubDate==null)
						{
							pubDate = new Date();
						}
					}
					DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");				
					String date = dateFormat.format(pubDate);
					
					if (date.compareToIgnoreCase(lastFetchDate)>0)
					{
						//com.robbypond version.
						//final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;
						//final HtmlArticleExtractor htmlExtr = HtmlArticleExtractor.INSTANCE;
						//String text = htmlExtr.process(extractor, linkSrc);
						
						//normal kohlschuetter extractor call
						// parse the document into boilerpipe's internal data structure
						final InputSource is = HTMLFetcher.fetch(linkSrc).toInputSource();						 
						TextDocument doc = new BoilerpipeSAXInput(is).getTextDocument();
						// perform the extraction/classification process on "doc"
						ArticleExtractor.INSTANCE.process(doc);
						//text = text.replaceAll("(?i)<p>", "").replaceAll("(?i)</p>", "\n\n").replaceAll("(?i)<br\\/?>","\n");

						// Hemen testuan gako hitzak bilatzeko kodea falta da, eta topatuz gero
						// aipamen bat sortu eta datubasera sartzea.
						String lang = LID.detectLanguage(doc.getContent(), langs);
						
						if (acceptedLangs.contains("all") || acceptedLangs.contains(lang))
						{
							parseArticleForKeywords(doc,lang, pubDate, link, sId);
						}
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
			} catch (BoilerpipeProcessingException | SAXException be){ //| URISyntaxException be) {			
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
	 * @param doc
	 * @param lang
	 * @param date
	 * @param link
	 * @param sId
	 */
	private void parseArticleForKeywords(TextDocument doc, String lang, Date date, String link, int sId) {
		
		Set<Keyword> result = new HashSet<Keyword>();
		String wholeText = StringUtils.stripAccents(doc.getContent()).toLowerCase(); 
		boolean anchorFound = anchorPattern.matcher(wholeText).find();
		System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - anchorPattern: "+anchorPattern.toString()
				+"\n -- found? "+anchorFound+" lang: "+lang+" indep/dep:"+independentkwrds.size()+"/"+dependentkwrds.size());
		
		
		String[] paragraphs = doc.getContent().split("\n+");
		for (String par : paragraphs )
		//for (TextBlock b : doc.getTextBlocks())
		{
			result = new HashSet<Keyword>();
			String searchText = StringUtils.stripAccents(par).toLowerCase();			
			System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - search paragraph: "+searchText);						
			//if (b.isContent())
			//{
				//String origText = b.getText();
				//String par = StringUtils.stripAccents(origText);
			
			//keywords that do not need any anchor
			for (Keyword k : independentkwrds)
			{				
				//System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - independent key:"+k.getText()+" l="+k.getLang());
				if (k.getLang().equalsIgnoreCase(lang) && kwrdPatterns.get(k.getId()).matcher(searchText).find())
				{
					System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - independent key found!!!: "+k.getText()+" id: "+k.getId());
					result.add(k);
				}
			}			
			//keywords that need and anchor, only if anchors where found
			if (anchorFound)
			{
				for (Keyword k : dependentkwrds)
				{
					if (k.getLang().equalsIgnoreCase(lang) && kwrdPatterns.get(k.getId()).matcher(searchText).find())
					{
						System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - dependent key found!!!: "+k.getText()+" id: "+k.getId());						
						result.add(k);
					}
				}
			}
			
			if (result != null && !result.isEmpty())
			{
				Mention m = new Mention(lang,par,date,link,String.valueOf(sId));
				m.setKeywords(result);
				m.mention2db(DBconn);
				System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - mention2db: "+par);
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

			String langs = acceptedLangs.toString();
			for (SyndEntry entry : feed.getEntries())
			{
				System.err.println("FeadReader::getFeed ->  entry: "+entry.getLink());
				link = entry.getLink();		
				URL linkSrc = new URL(link);
				Date pubDate = entry.getPublishedDate();
				if (pubDate==null)
				{
					pubDate = feed.getPublishedDate();
					if (pubDate==null)
					{
						pubDate= new Date();
					}
				}	
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");				
				String date = dateFormat.format(pubDate);

				//com.robbypond version.
				//final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;
				//final HtmlArticleExtractor htmlExtr = HtmlArticleExtractor.INSTANCE;
				//String text = htmlExtr.process(extractor, linkSrc);

				//normal kohlschuetter extractor call
				// parse the document into boilerpipe's internal data structure
				final InputSource is = HTMLFetcher.fetch(linkSrc).toInputSource();						 
				TextDocument doc = new BoilerpipeSAXInput(is).getTextDocument();
				// perform the extraction/classification process on "doc"
				ArticleExtractor.INSTANCE.process(doc);
				
				//text = text.replaceAll("(?i)<p>", "").replaceAll("(?i)</p>", "\n\n").replaceAll("(?i)<br\\/?>","\n");

				// Hemen testuan gako hitzak bilatzeko kodea falta da, eta topatuz gero
				// aipamen bat sortu eta datubasera sartzea.
				//System.err.println("--------------------\n"+doc.getContent()+"\n----------------\n");
				String lang = LID.detectLanguage(doc.getContent(), langs);
				
				if (acceptedLangs.contains("all") || acceptedLangs.contains(lang))
				{
					parseArticleForKeywords(doc,lang, pubDate, link, 9999999);
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
		} catch (BoilerpipeProcessingException | SAXException be){ //| URISyntaxException be) {			
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
		System.err.println("elh-MSM::FeedReader - Accepted languages: "+acceptedLangs);
	}

	public void closeDBConnection()
	{
		try {
			DBconn.close();
		} catch (SQLException e) {
			System.err.println("elh-MSM::FeedReader::closeDBConnection - ERROR when closing DB connection. "
					+ "Either it is already closed or it was never openned");
			e.printStackTrace();
		}
	}
	
}
