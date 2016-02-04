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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.rometools.utils.Dates;

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
	private Set<Feed> feeds;
	private Set<Keyword> kwrds;
	private Set<Keyword> independentkwrds = new HashSet<Keyword>();
	private Set<Keyword> dependentkwrds = new HashSet<Keyword>();

	private static Pattern anchorPattern; //pattern for anchor kwrds. they are usually general terms.
	private HashMap<Integer,Pattern> kwrdPatterns = new HashMap<Integer,Pattern>(); //patterns for keywords.

	private static List<DateFormat> dateFormats= new ArrayList<DateFormat>(
			Arrays.asList(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z"),
					new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
					new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z")));


	public Set<Feed> getFeeds(){
		return this.feeds;
	}

	public void setFeeds(Set<Feed> flist){
		this.feeds=flist;
	}
	
	public FeedReader(String source) {

		//Language identification
		loadAcceptedLangs(params.getProperty("langs", "all"));

		//keyword loading: keywords to identify relevant mentions in articles.
		try {
			DBconn = Utils.DbConnection(params.getProperty("dbuser"),params.getProperty("dbpass"),params.getProperty("dbhost"),params.getProperty("dbname"));
			kwrds = Keyword.retrieveFromDB(DBconn, "press", params.getProperty("langs", "all"));
			System.err.println("elh-MSM::FeedReader(config,store) - retrieved "+kwrds.size()+" keywords");

			closeDBConnection();
		}catch (Exception e)
		{
			System.err.println("elh-MSM::FeedReader(config,store) - DB Error when trying to load keywords");
			e.printStackTrace();


			//System.exit(1);
		}

		// prepare patterns to match keywords
		constructKeywordsPatterns();

		String[] urls = source.split(",");
		for (int i=0; i<urls.length;i++)
		{
			try {
				URL feedUrl = new URL(urls[i]);
				getFeed(feedUrl, "stout");
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
			//System.err.println("elh-MSM::FeedReader::constructKeywordPatterns - currentPattern:"+p.toString());

			kwrdPatterns.put(k.getId(), p);
			if (k.isAnchor())
			{
				sb_anchors.append(k.getText().replace('_',' ').toLowerCase()).append("|"); 
			}

			if (k.needsAnchor())
			{
				dependentkwrds.add(k);
			}
			else	
			{
				independentkwrds.add(k);
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
			kwrds = Keyword.retrieveFromDB(DBconn, "Press", params.getProperty("langs", "all"));
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
						getFeed(feedUrl, store);
					} catch (MalformedURLException ue ) {
						System.err.println("MSM::FeedReader - ERROR: malformed source url given - "+ue.getMessage()+" url: "+urls[i]);
					}			
				}
				closeDBConnection();
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
					int id = rs.getInt("behagunea_app_feed.source_id");
					int fid = rs.getInt("id");
					String url = rs.getString("url");
					String lastFetch = rs.getString("behagunea_app_feed.last_fetch");
					String langs = rs.getString("lang");
					try {
						URL feedUrl = new URL(url);
						getFeed(feedUrl, lastFetch, langs, id, fid, store);
					} catch (MalformedURLException ue ) {
						System.err.println("MSM::FeedReader - ERROR: malformed source url given - "+ue.getMessage()+" url: "+url);
					}		
				}
				st.close();			
				closeDBConnection();
			} catch (SQLException | NamingException sqle){
				System.err.println("elh-MSM::FeedReader - DB Error when retrieving feed sources");
				sqle.printStackTrace();
				System.exit(1);
			}
		}		
	}//end constructor


	public FeedReader(String config, Set<Feed> feedList, Set<Keyword> kwrdList, String store) {
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
		kwrds = kwrdList;
		setFeeds(feedList);

		// prepare patterns to match keywords
		constructKeywordsPatterns();
		
		if (store.equalsIgnoreCase("db"))
		{
			try {
				DBconn = Utils.DbConnection(params.getProperty("dbuser"),params.getProperty("dbpass"),params.getProperty("dbhost"),params.getProperty("dbname"));
			} catch (NamingException | SQLException e) {
				System.err.println("elh-MSM::FeedReader - Database connection could not be stablished.");
				System.exit(1);
			}
		}

	}//end constructor


	public void processFeeds(String store)
	{
		for (Feed f : getFeeds())
		{
			getFeed(f, store);
		}
		
		closeDBConnection();
	}


	/**
	 * Core function of the class. code to process a single feed
	 * 
	 * @param Feed f
	 * 
	 */
	private void getFeed (Feed f, String store){

		System.err.println("FeadReader::getFeed -> parse feed "+f.getFeedURL()+" lastFetched: "+f.getLastFetchDate());
		String link = "";

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");				
		Date currentDate = new Date();

		Date lastFetchDate_date = new Date();
		for (DateFormat df : dateFormats)
		{
			try {
				lastFetchDate_date = df.parse(f.getLastFetchDate());
				break;						
			}catch(ParseException de){
				//continue loop
			}
		}	
		
		//reload language identification with the feed possible languages
		//loadAcceptedLangs(f.getLangs());

		SyndFeedInput input = new SyndFeedInput();
		input.setPreserveWireFeed(true);
		SyndFeed feed = new SyndFeedImpl();
		//try to read a feed.
		try {
			feed = input.build(new XmlReader(new URL(f.getFeedURL())));
			//String ftype =feed.getFeedType();
		}catch (FeedException | IOException fe) {							     
			fe.printStackTrace();
			System.err.println("FeadReader::getFeed ->  Feed ERROR with"+f.getFeedURL()+" : "+fe.getMessage());
		} 
		
		//System.err.println("FeadReader::getFeed -> feed type: "+feed type);
		for (SyndEntry entry : feed.getEntries())
		{
			//System.err.println("FeadReader::getFeed -> analysing entries");
			link = entry.getLink();		

			try 
			{	
				if (store.equalsIgnoreCase("db"))
				{	
					String query = "SELECT count(*) FROM "
						+ "behagunea_app_mention "
						+ "WHERE url='"+link+"'";

					Statement st = DBconn.createStatement();			       
					// execute the query, and get a java resultset
					ResultSet rs = st.executeQuery(query);
					int urlKop = 0 ;
					while (rs.next()){
						urlKop = rs.getInt(1); 
					}
					//if the url already exist do not parse the entry
					if (urlKop > 0){
						System.err.println("FeadReader::getFeed -> entry already parsed "+link);
						continue;
					}
					st.close();
				}
			} catch(SQLException sqle) {
				System.err.println("FeadReader::getFeed ->  MYSQL ERROR when looking for parsed urls in DB "+f.getFeedURL());
			}
			
			try
			{
				URL linkSrc = new URL(link);
				Date pubDate = entry.getPublishedDate();
				boolean nullDate=false;
				if (pubDate==null)
				{					
					//entry.getWireEntry();
					pubDate = feed.getPublishedDate();						
					if (pubDate==null)
					{
						Calendar c = Calendar.getInstance(); 
						c.setTime(currentDate); 
						c.add(Calendar.DATE, -1);
						pubDate = c.getTime();
						nullDate=true;
					}
				}
				String date = dateFormat.format(pubDate);

				if ((!nullDate && pubDate.after(lastFetchDate_date)) || (nullDate && lastFetchDate_date.before(pubDate)) )
				{
					System.err.println("FeadReader::getFeed -> new entry "+date+" vs."+f.getLastFetchDate());
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

					//detect language
					String lang = LID.detectLanguage(doc.getContent(), f.getLangs());

					//if language accepted parse article for mentions. If found store them to DB or print them
					if (acceptedLangs.contains("all") || acceptedLangs.contains(lang))
					{
						parseArticleForKeywords(doc,lang, pubDate, link, f.getSrcId(), store);
					}
				}
				else
				{
					System.err.println("FeadReader::getFeed -> no new entries ");
				}

			} catch (IOException ioe) {	        
				ioe.printStackTrace();
				System.err.println("FeadReader::getFeed ->  ERROR when reading html a link ("+link+") - "+ioe.getMessage());
			} catch (BoilerpipeProcessingException | SAXException be){ //| URISyntaxException be) {			
				be.printStackTrace();
				System.err.println("FeadReader::getFeed ->  Boilerplate removal ERROR with"+f.getFeedURL()+" : "+be.getMessage());
			}

		}
		
		try {
			//update last fetch date in the DB.
			if (store.equalsIgnoreCase("db"))
			{
				String updateComm = "UPDATE behagunea_app_feed "
						+ "SET last_fetch='"+dateFormat.format(currentDate)+"' WHERE id='"+f.getId()+"'";
				Statement st = DBconn.createStatement();			       
				// execute the query, and get a java resultset
				st.executeUpdate(updateComm);
			}
		}catch (SQLException sqle) {
			System.out.println("FeadReader::getFeed ->  ERROR when updating fetch time "+dateFormat.format(currentDate)+" : "+sqle.getMessage());
			//e.printStackTrace();
		}		
	}




	/**
	 * @param url
	 * @param lastFetchDate
	 * @param langs
	 * @param sId
	 * @param fId
	 * 
	 * @deprecated
	 */
	private void getFeed (URL url, String lastFetchDate, String langs, int sId, int fId, String store){

		System.err.println("FeadReader::getFeed -> parse feed "+url.toString()+" lastFetched: "+lastFetchDate);
		boolean ok = false;
		String link = "";


		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");				
		Date currentDate = new Date();

		//Language identification
		loadAcceptedLangs(langs);

		try {
			SyndFeedInput input = new SyndFeedInput();
			input.setPreserveWireFeed(true);
			SyndFeed feed = input.build(new XmlReader(url));
			String ftype =feed.getFeedType();

			//if (ftype.matches("rss"))
			//{

			//}
			//else if(ftype.matches("atom"))
			//{

			//}
			//System.err.println("FeadReader::getFeed -> feed type: "+feed type);
			for (SyndEntry entry : feed.getEntries())
			{
				//System.err.println("FeadReader::getFeed -> analysing entries");
				link = entry.getLink();		

				String query = "SELECT count(*) FROM "
						+ "behagunea_app_mention "
						+ "WHERE url='"+link+"'";

				Statement st = DBconn.createStatement();			       
				// execute the query, and get a java resultset
				ResultSet rs = st.executeQuery(query);
				int urlKop = 0 ;
				while (rs.next()){
					urlKop = rs.getInt(1); 
				}
				//if the url already exist do not parse the entry
				if (urlKop > 0){
					System.err.println("FeadReader::getFeed -> entry already parsed "+link);
					continue;
				}
				st.close();

				URL linkSrc = new URL(link);
				Date pubDate = entry.getPublishedDate();
				boolean nullDate=false;
				if (pubDate==null)
				{					
					entry.getWireEntry();
					pubDate = feed.getPublishedDate();						
				}
				Date lastFetchDate_date = new Date();
				for (DateFormat df : dateFormats)
				{
					try {
						lastFetchDate_date = dateFormat.parse(lastFetchDate);
						break;						
					}catch(ParseException de){
						//continue loop
					}
				}	

				if (pubDate==null)
				{
					Calendar c = Calendar.getInstance(); 
					c.setTime(currentDate); 
					c.add(Calendar.DATE, -1);
					pubDate = c.getTime();
					nullDate=true;
				}
				String date = dateFormat.format(pubDate);

				if ((!nullDate && pubDate.after(lastFetchDate_date)) || (nullDate && lastFetchDate_date.before(pubDate)) )
				{
					System.err.println("FeadReader::getFeed -> new entry "+date+" vs."+lastFetchDate);
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
						parseArticleForKeywords(doc,lang, pubDate, link, sId, store);
					}
				}
				else
				{
					System.err.println("FeadReader::getFeed -> no new entries ");
				}

			}

			String updateComm = "UPDATE behagunea_app_feed "
					+ "SET last_fetch='"+dateFormat.format(currentDate)+"' WHERE id='"+fId+"'";
			Statement st = DBconn.createStatement();			       
			// execute the query, and get a java resultset
			st.executeUpdate(updateComm);


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
		} catch (SQLException sqle) {
			System.out.println("FeadReader::getFeed ->  ERROR when updating fetch time "+dateFormat.format(currentDate)+" : "+sqle.getMessage());
			//e.printStackTrace();
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
	private void parseArticleForKeywords(TextDocument doc, String lang, Date date, String link, int sId, String store) {

		Set<Keyword> result = new HashSet<Keyword>();
		String wholeText = StringUtils.stripAccents(doc.getContent()).toLowerCase(); 
		boolean anchorFound = anchorPattern.matcher(wholeText).find();
		//System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - anchorPattern: "+anchorPattern.toString()
		//		+"\n -- found? "+anchorFound+" lang: "+lang+" indep/dep:"+independentkwrds.size()+"/"+dependentkwrds.size());


		String[] paragraphs = doc.getContent().split("\n+");
		for (String par : paragraphs )
			//for (TextBlock b : doc.getTextBlocks())
		{
			result = new HashSet<Keyword>();
			String searchText = StringUtils.stripAccents(par).toLowerCase();			
			//System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - search paragraph: "+searchText);						
			//if (b.isContent())
			//{
			//String origText = b.getText();
			//String par = StringUtils.stripAccents(origText);

			//keywords that do not need any anchor
			for (Keyword k : independentkwrds)
			{				
				//System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - independent key:"
				//	+k.getText()+" l="+k.getLang()+" pattern:"+kwrdPatterns.get(k.getId()).toString());
				if(k.getLang().equalsIgnoreCase(lang) && kwrdPatterns.get(k.getId()).matcher(searchText).find())
				{	
					//System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - independent key found!!!: "+k.getText()+" id: "+k.getId());
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
						//System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - dependent key found!!!: "+k.getText()+" id: "+k.getId());						
						result.add(k);
					}					
				}
			}

			if (result != null && !result.isEmpty())
			{
				Mention m = new Mention(lang,par,date,link,String.valueOf(sId));
				m.setKeywords(result);
				if (store.equalsIgnoreCase("db"))
				{
					m.mention2db(DBconn);
					System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - mention2db: "+par);
				}
				else
				{
					System.out.println("elh-MSM::FeedReader::parseArticleForKeywords - mention found!: "+par);
					m.print();
					System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - mention 2 stout: "+par);
				}
			}			
		}				
	}

	/**
	 *  Function retrieves a feed and processes it (either stores in the database or prints to the stdout
	 * 
	 * @param url
	 * @Deprecated	
	 */
	private void getFeed (URL url,String store){

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
					System.err.println("FeadReader::getFeed ->  entry pubdate is null: ");
					pubDate = feed.getPublishedDate();
					if (pubDate==null)
					{
						System.err.println("FeadReader::getFeed ->  feed pubdate is null: ");
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
					parseArticleForKeywords(doc,lang, pubDate, link, -11111111,store);
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
