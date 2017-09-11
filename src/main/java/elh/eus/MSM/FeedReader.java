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
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
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
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.naming.NamingException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import com.google.api.client.http.HttpResponse;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import de.l3s.boilerpipe.sax.HTMLDocument;
import de.l3s.boilerpipe.sax.HTMLFetcher;
import eus.ixa.ixa.pipe.seg.RuleBasedSegmenter;

import fabricator.*;


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
	private Set<Long> census = new HashSet<Long>();
	
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
	
	
	/**
	 * Minimum constructor. Only a string of feeds is given and result is printed to stout
	 * @param source : ',' separated list of feed URLs. 
	 */
	@Deprecated
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
			return;
			//System.exit(1);
		}

		StringBuilder sb_anchors = new StringBuilder();
		sb_anchors.append("\\b(");
		for (Keyword k : kwrds)
		{
			if (k.isKword())
			{				
				//create and store pattern;
				Pattern p=null;
				// as of 2016/08/03 press mentions have to respect keyword capitalization.
				if (k.getCaseSensitiveSearch())
				{
					p = Pattern.compile("\\b"+k.getText().replace('_',' '));//.toLowerCase());
				}
				else
				{
					p = Pattern.compile("\\b"+k.getText().replace('_',' ').toLowerCase());
				}
				//System.err.println("elh-MSM::FeedReader::constructKeywordPatterns - currentPattern:"+p.toString());

				kwrdPatterns.put(k.getId(), p);

				if (k.needsAnchor())
				{
					dependentkwrds.add(k);
				}
				else	
				{
					independentkwrds.add(k);
				}
			}
			if (k.isAnchor())
			{
				sb_anchors.append(k.getText().replace('_',' ').toLowerCase()).append("|"); 
			}
		} 
		String anchPatt = sb_anchors.toString();
		anchPatt=anchPatt.substring(0, anchPatt.length()-1)+")";
		anchorPattern = Pattern.compile(anchPatt);
	}

	
	/**
	 *  Main constructor. 
	 * 
	 * @param config
	 * @param feedList
	 * @param kwrdList
	 * @param store
	 */
	public FeedReader(String config, Set<Feed> feedList, Set<Keyword> kwrdList, String store, String censusf) {
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
		
		//if a census field is provided fill the census map
		if (Utils.checkFile(censusf))
		{
			try {
				census = Utils.loadOneColumnResource(new FileInputStream(censusf));
			} catch (FileNotFoundException fe){
				System.err.println("elh-MSM::FeedReader - census file NOT FOUND, crawler will continue without census"+censusf);
			} catch (IOException ioe){
				System.err.println("elh-MSM::FeedReader - census file COULD NOT BE READ, crawler will continue without census "+censusf);
			} 
		}
		
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


	public void processFeeds(String store, String type)
	{
		switch (type)
		{
		case "press":
			for (Feed f : getFeeds())
			{
				getRssFeed(f, store);
			}
			break;
		case "multimedia":
			for (Feed f : getFeeds())
			{
				getMultimediaFeed(f, store);
			}
			break;
		default:
			System.out.println("MSM::FeedReader::processFeeds -> wrong feed type provided [press|multimedia] -> "+type);
		}
		
		try{
			closeDBConnection();			
		}catch(NullPointerException ne){
			//no connection to close.
		}

	}

	/**
	 * Core function for standard rss feeds. Code to process a single feed
	 * 
	 * @param Feed f
	 * 
	 */
	private void getRssFeed (Feed f, String store){

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
			}catch(ParseException pe){
				//continue loop
			}
		}	
		
		// reload language identification with the feed possible languages
		// loadAcceptedLangs(f.getLangs());
		SyndFeed feed = new SyndFeedImpl();
		try{
			HttpClient client = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();
			// (CloseableHttpClient client = HttpClients.createDefault()..createMinimal()) 
			//client.setRedirectStrategy(new LaxRedirectStrategy());
			HttpUriRequest method = new HttpGet(f.getFeedURL());
			org.apache.http.HttpResponse response = client.execute(method);
			//try (CloseableHttpResponse response = client.execute(method);
			InputStream stream = response.getEntity().getContent();	
			SyndFeedInput input = new SyndFeedInput();
			input.setPreserveWireFeed(true);
			// try to read a feed.
			try {
				feed = input.build(new XmlReader(stream));
				// String ftype =feed.getFeedType();
			} catch (FeedException | IOException fe) {				
				System.err.println(
						"FeadReader::getFeed ->  Feed ERROR with" + f.getFeedURL() + " : " + fe.getMessage());
				fe.printStackTrace();
			}
		} catch (IOException cpe) {			
			System.err.println(
					"FeadReader::getFeed ->  HTTP ERROR with" + f.getFeedURL() + " : " + cpe.getMessage());
			cpe.printStackTrace();
		}

		//SyndFeedInput input = new SyndFeedInput();
		//input.setPreserveWireFeed(true);
		//SyndFeed feed = new SyndFeedImpl();
		
		
		int newEnts =0;
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
					newEnts++;
					System.err.println("FeadReader::getFeed -> new entry "+date+" vs."+f.getLastFetchDate());
					//com.robbypond version.
					//final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;
					//final HtmlArticleExtractor htmlExtr = HtmlArticleExtractor.INSTANCE;
					//String text = htmlExtr.process(extractor, linkSrc);

					//normal kohlschuetter extractor call
					// parse the document into boilerpipe's internal data structure
					//final InputSource is = HTMLFetcher.fetch(linkSrc).toInputSource();
					final InputSource is = fetchHTML(linkSrc);
					TextDocument doc = new BoilerpipeSAXInput(is).getTextDocument();
					// perform the extraction/classification process on "doc"
					ArticleExtractor.INSTANCE.process(doc);
					//text = text.replaceAll("(?i)<p>", "").replaceAll("(?i)</p>", "\n\n").replaceAll("(?i)<br\\/?>","\n");

					//detect language
					String lang = LID.detectFeedLanguage(doc.getContent(), f.getLangs());

					//if language accepted parse article for mentions. If found store them to DB or print them
					if (acceptedLangs.contains("all") || acceptedLangs.contains(lang))
					{
						if (kwrds.isEmpty())
						{
							System.err.println("MSM::FeadReader::getFeed ->no keywords provided full articles will be returned");
							processFullArticle(doc,lang, pubDate, link, f.getSrcId(), store);
						}
						else
						{
							parseArticleForKeywords(doc,lang, pubDate, link, f.getSrcId(), store);
						}
					}
				}
			//	else
			//	{
			//		System.err.println("FeadReader::getFeed -> no new entries ");
			//	}

			} catch (IOException ioe) {	        
				ioe.printStackTrace();
				System.err.println("FeadReader::getFeed ->  ERROR when reading html a link ("+link+") - "+ioe.getMessage());
			} catch (BoilerpipeProcessingException | SAXException be){ //| URISyntaxException be) {			
				be.printStackTrace();
				System.err.println("FeadReader::getFeed ->  Boilerplate removal ERROR with"+f.getFeedURL()+" : "+be.getMessage());
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		System.err.println("FeadReader::getFeed -> found "+newEnts+" new entries ");

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
	 * Core function of the class. Code to process a single feed
	 * 
	 * @param Feed f
	 * 
	 */
	private void getMultimediaFeed (Feed f, String store){

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
			}catch(ParseException pe){
				//continue loop
			}
		}	
		
		// reload language identification with the feed possible languages
		// loadAcceptedLangs(f.getLangs());
		SAXBuilder sax = new SAXBuilder();
		XPathFactory xFactory = XPathFactory.instance();
		Document feed;		
		List<multimediaElement> entries = new ArrayList<multimediaElement>();
		try{
			HttpClient client = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();
			// (CloseableHttpClient client = HttpClients.createDefault()..createMinimal()) 
			//client.setRedirectStrategy(new LaxRedirectStrategy());
			HttpUriRequest method = new HttpGet(f.getFeedURL());
			org.apache.http.HttpResponse response = client.execute(method);
			//try (CloseableHttpResponse response = client.execute(method);
			InputStream stream = response.getEntity().getContent();	
			// try to read a feed.
			try {
				feed = sax.build(stream);
				XPathExpression<Element> expr = xFactory.compile("//saioa",
						Filters.element());
				List<Element> shows = expr.evaluate(feed);
				for (Element show : shows) {
					multimediaElement mme = new multimediaElement(show);
					entries.add(mme);					
				}
			} catch (JDOMException | IOException e) {
				System.err.println( "FeadReader::getMultimediaFeed -> Feed ERROR with" + f.getFeedURL() + " : " + e.getMessage());
				//e.printStackTrace();
				//System.err.println("EliXa::CorpusReader - Could not extract opinions from corpus (semeval2015 format)."
				//		+ "\n\t Make sure the corpus is correctly formatted, and if so pass the correct format in the -f argument."
				//		+ "\n\t If you are already doing that the file can't be read for some other reason");				
			}
		} catch (IOException cpe) {			
			System.err.println(
					"FeadReader::getFeed ->  HTTP ERROR with" + f.getFeedURL() + " : " + cpe.getMessage());
			cpe.printStackTrace();
		}

		int newEnts =0;
		//System.err.println("FeadReader::getFeed -> feed type: "+feed type);
		for (multimediaElement entry : entries)
		{
			//System.err.println("FeadReader::getFeed -> analysing entries");
			link = entry.getOriginURL();		

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
				
				/**
				 * 
				 * 
				 * 
				 *   HEMEN SARTU BEHAR DA TRANSKRIPZIOA EGITEKO KORRITU ETA KEYWORD-AK TOPATZEKO KODEA
				 * 
				 * 
				 * 
				 * */
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
					newEnts++;
					System.err.println("FeadReader::getFeed -> new entry "+date+" vs."+f.getLastFetchDate());
					//com.robbypond version.
					//final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;
					//final HtmlArticleExtractor htmlExtr = HtmlArticleExtractor.INSTANCE;
					//String text = htmlExtr.process(extractor, linkSrc);

					//normal kohlschuetter extractor call
					// parse the document into boilerpipe's internal data structure
					//final InputSource is = HTMLFetcher.fetch(linkSrc).toInputSource();
					final InputSource is = fetchHTML(linkSrc);
					TextDocument doc = new BoilerpipeSAXInput(is).getTextDocument();
					// perform the extraction/classification process on "doc"
					ArticleExtractor.INSTANCE.process(doc);
					//text = text.replaceAll("(?i)<p>", "").replaceAll("(?i)</p>", "\n\n").replaceAll("(?i)<br\\/?>","\n");

					//detect language
					String lang = LID.detectFeedLanguage(doc.getContent(), f.getLangs());

					//if language accepted parse article for mentions. If found store them to DB or print them
					if (acceptedLangs.contains("all") || acceptedLangs.contains(lang))
					{
						if (kwrds.isEmpty())
						{
							System.err.println("MSM::FeadReader::getFeed ->no keywords provided full articles will be returned");
							processFullArticle(doc,lang, pubDate, link, f.getSrcId(), store);
						}
						else
						{
							parseArticleForKeywords(doc,lang, pubDate, link, f.getSrcId(), store);
						}
					}
				}
			//	else
			//	{
			//		System.err.println("FeadReader::getFeed -> no new entries ");
			//	}

			} catch (IOException ioe) {	        
				ioe.printStackTrace();
				System.err.println("FeadReader::getFeed ->  ERROR when reading html a link ("+link+") - "+ioe.getMessage());
			} catch (BoilerpipeProcessingException | SAXException be){ //| URISyntaxException be) {			
				be.printStackTrace();
				System.err.println("FeadReader::getFeed ->  Boilerplate removal ERROR with"+f.getFeedURL()+" : "+be.getMessage());
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		System.err.println("FeadReader::getFeed -> found "+newEnts+" new entries ");

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
	 * Function looks for keywords in a given text. The text is broken down into sentences and 
	 * keyword search is carried out for each sentence. If keywords are found, a sentence is stored as a mention.
	 * 
	 * @param doc
	 * @param lang
	 * @param date
	 * @param link
	 * @param srcId
	 */
	private void parseArticleForKeywords(TextDocument doc, String lang, Date date, String link, long srcId, String store) {

		Set<Keyword> result = new HashSet<Keyword>();
		
		String wholeText = StringUtils.stripAccents(doc.getContent()).toLowerCase(); 
		boolean anchorFound = anchorPattern.matcher(wholeText).find();
		//System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - anchorPattern: "+anchorPattern.toString()
		//		+"\n -- found? "+anchorFound+" lang: "+lang+" indep/dep:"+independentkwrds.size()+"/"+dependentkwrds.size());


		// objects needed to call the tokenizer
		Properties tokProp = new Properties();		
		tokProp.setProperty("language", lang);
		tokProp.setProperty("normalize", "default");
		tokProp.setProperty("untokenizable", "no");
		tokProp.setProperty("hardParagraph", "no");	
		// tokenizer call
		RuleBasedSegmenter seg = new RuleBasedSegmenter(doc.getContent(),tokProp);
		String[] paragraphs = seg.segmentSentence();
		//String[] paragraphs = doc.getContent().split("\n+");
		
		for (String par : paragraphs )
			//for (TextBlock b : doc.getTextBlocks())
		{
			result = new HashSet<Keyword>();
			// this set controls that two keywords belonging to the same screen tag won't be assigned to a mention 
			Set<String> screenTagList = new HashSet<String>();
			
			// capitalization must be respected in order to accept keywords found in paragraphs.
			// as of 2016/08/03 press mentions have to respect keyword capitalization. 
			String searchText = StringUtils.stripAccents(par); //.toLowerCase(); 
			String searchTextLC = StringUtils.stripAccents(par).toLowerCase();
			//keywords that do not need any anchor
			for (Keyword k : independentkwrds)
			{	
				//check if a keyword with the same tag has been already matched, if so do not check the key. 
				if (screenTagList.contains(k.getScreenTag()))
				{
					System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - indpndnt keyword found,"
							+ " but not stored because another key was matched with the same screen tag: "+k.getText());
					continue;
				}
				boolean kwrdFound = false;
				//check if keywords are found in the sentence
				// case sensitive search
				if (k.getCaseSensitiveSearch())
				{
					kwrdFound = kwrdPatterns.get(k.getId()).matcher(searchText).find();
				}
				// case insensitive search
				else
				{
					kwrdFound = kwrdPatterns.get(k.getId()).matcher(searchTextLC).find();
				}
				
				//System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - independent key:"
				//	+k.getText()+" l="+k.getLang()+" pattern:"+kwrdPatterns.get(k.getId()).toString());
				if(k.getLang().equalsIgnoreCase(lang) && kwrdFound)
				{	
					//System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - independent key found!!!: "+k.getText()+" id: "+k.getId());
					result.add(k);
					screenTagList.add(k.getScreenTag());
				}								
			}			
			//keywords that need and anchor, only if anchors where found
			if (anchorFound)
			{
				for (Keyword k : dependentkwrds)
				{
					//check if a keyword with the same tag has been already matched, if so do not check the key. 
					if (screenTagList.contains(k.getScreenTag()))
					{
						System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - dpndnt keyword found,"
								+ " but not stored because another key was matched with the same screen tag: "+k.getText());
						continue;
					}

					boolean kwrdFound = false;
					//check if keywords are found in the sentence
					// case sensitive search
					if (k.getCaseSensitiveSearch())
					{
						kwrdFound = kwrdPatterns.get(k.getId()).matcher(searchText).find();
					}
					// case insensitive search
					else
					{
						kwrdFound = kwrdPatterns.get(k.getId()).matcher(searchTextLC).find();
					}
					
					if (k.getLang().equalsIgnoreCase(lang) && kwrdFound)
					{
						//System.err.println("elh-MSM::FeedReader::parseArticleForKeywords - dependent key found!!!: "+k.getText()+" id: "+k.getId());						
						result.add(k);
						screenTagList.add(k.getScreenTag());
					}					
				}
			}

			if (result != null && !result.isEmpty())
			{
				Mention m = new Mention(lang,par,date,link,srcId,true);
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
				}
			}			
		} //for each paragraph				
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
				//DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");				
				//String date = dateFormat.format(pubDate);


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
				String lang = LID.detectFeedLanguage(doc.getContent(), langs);

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

	/**
	 * @param doc
	 * @param lang
	 * @param date
	 * @param link
	 * @param sId
	 */
	private void processFullArticle(TextDocument doc, String lang, Date date, String link, long sId, String store) 
	{
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");				
		
		if (store.equalsIgnoreCase("db"))
		{
			System.err.println("elh-MSM::FeedReader::processFullArticle - 2db not implemented yet. "+link);
		}
		else
		{
			System.err.println("elh-MSM::FeedReader::processFullArticle - article to stout: "+link);
			StringBuilder tp = new StringBuilder();
			tp.append("<doc>\n").append("<url>").append(link).append("</url>\n");
			tp.append("<lang>").append(lang).append("</lang>\n");
			tp.append("<date>").append(dateFormat.format(date)).append("</date>\n");
			tp.append("<title>").append(doc.getTitle()).append("</title>\n");
			tp.append("<text>").append(doc.getText(true, true)).append("</text>\n");
			tp.append("</doc>\n");			
			System.out.println(tp.toString());
		}
	}
	
	/**
	 * Load accepted langs from string
	 * @param property
	 */
	private void loadAcceptedLangs(String property) {
		this.acceptedLangs=Arrays.asList(property.split(","));	
		System.err.println("elh-MSM::FeedReader - Accepted languages: "+acceptedLangs);
	}

	/**
	 * Close connetion to DB.
	 */
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


	/**
	 *  Custom html source fetcher. It differs from kohlschuetter's provided version in that a random user
	 *  agent is added to the URLConnection object, otherwise some servers return error codes (403,404 or 504
	 *  have been spotted).
	 * 
	 * @param linkSrc
	 * @return Input source
	 * @throws IOException
	 * @throws URISyntaxException 
	 */
	private InputSource fetchHTML(URL linkSrc) throws IOException, URISyntaxException {
		
		final Pattern PAT_CHARSET = Pattern
				.compile("charset=([^; ]+)$");

		HttpClient client = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();
		// (CloseableHttpClient client = HttpClients.createDefault()..createMinimal()) 
		//client.setRedirectStrategy(new LaxRedirectStrategy());
		HttpUriRequest method = new HttpGet(linkSrc.toURI());
		org.apache.http.HttpResponse response = client.execute(method);
		//try (CloseableHttpResponse response = client.execute(method);
		InputStream stream = response.getEntity().getContent();	
		
		//final URLConnection conn = linkSrc.openConnection();
		
		// kodean gehituta lerroak
		//String userAg = Fabricator.userAgent().browser();
		//System.err.println("User-Agent-a: "+userAg);
		//conn.addRequestProperty("User-Agent", userAg);
		//conn.addRequestProperty("Accept","*/*");
		
		
		final String ct = response.getEntity().getContentType().getValue();//conn.getContentType();

		if (ct == null
				|| !(ct.equals("text/html") || ct.startsWith("text/html;"))) {
			throw new IOException("Unsupported content type: "+ct);
		}

		Charset cs = Charset.forName("Cp1252");
		if (ct != null) {
			Matcher m = PAT_CHARSET.matcher(ct);
			if (m.find()) {
				final String charset = m.group(1);
				try {
					cs = Charset.forName(charset);
				} catch (UnsupportedCharsetException e) {
					System.err.println("WARN: unsupported charset: "+ charset);
				}
			}
		}

		InputStream in = stream;//conn.getInputStream();

		final Header encoding = response.getEntity().getContentEncoding();
		if (encoding != null) {
			if ("gzip".equalsIgnoreCase(encoding.getValue())) {
				in = new GZIPInputStream(in);
			} else {
				System.err.println("WARN: unsupported Content-Encoding: "+ encoding.getValue());
			}
		}

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		int r;
		while ((r = in.read(buf)) != -1) {
			bos.write(buf, 0, r);
		}
		in.close();

		final byte[] data = bos.toByteArray();
		
		// kodifikazio aldatu UTF-8 ra, batzuk conn.getContentType() : ISO-8859-1 itzultzen dute 
		byte[] utf8 = new String(data, cs.displayName()).getBytes("UTF-8");
		cs = Charset.forName("UTF-8");
		return new HTMLDocument(utf8, cs).toInputSource();
			
	}
	
	
}
