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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Scanner;
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
import org.xml.sax.XMLReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.Header;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.auth.BasicScheme; 
import org.apache.http.ssl.SSLContextBuilder;
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
					new SimpleDateFormat("yyyy-MM-dd"),
					new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z")));


	private  HashMap<String, HashMap<String, String>> credentials = new HashMap<String, HashMap<String,String>>();
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
			DBconn = MSMUtils.DbConnection(params.getProperty("dbuser"),params.getProperty("dbpass"),params.getProperty("dbhost"),params.getProperty("dbname"));
			kwrds = Keyword.retrieveFromDB(DBconn, "press", params.getProperty("langs", "all"));
			System.err.println("MSM::FeedReader(config,store) - retrieved "+kwrds.size()+" keywords");

			closeDBConnection();
		}catch (Exception e)
		{
			System.err.println("MSM::FeedReader(config,store) - DB Error when trying to load keywords");
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
		
		boolean anchors = false;

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
				//System.err.println("MSM::FeedReader::constructKeywordPatterns - currentPattern:"+p.toString());

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
				anchors=true;
			}
		} 

		// if anchor keywords found construct the anchor pattern
		if (anchors)
		{
			String anchPatt = sb_anchors.toString();		
			anchPatt=anchPatt.substring(0, anchPatt.length()-1)+")";
			anchorPattern = Pattern.compile(anchPatt);
		}
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
			System.err.println("MSM::FeedReader - Config file not found "+config);
			System.exit(1);
		} catch (IOException ioe){
			System.err.println("MSM::FeedReader - Config file could not read "+config);
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
		if (MSMUtils.checkFile(censusf))
		{
			try {
				census = MSMUtils.loadOneColumnResource(new FileInputStream(censusf));
			} catch (FileNotFoundException fe){
				System.err.println("MSM::FeedReader - census file NOT FOUND, crawler will continue without census"+censusf);
			} catch (IOException ioe){
				System.err.println("MSM::FeedReader - census file COULD NOT BE READ, crawler will continue without census "+censusf);
			} 
		}
		
		if (store.equalsIgnoreCase("db"))
		{
			try {
				DBconn = MSMUtils.DbConnection(params.getProperty("dbuser"),params.getProperty("dbpass"),params.getProperty("dbhost"),params.getProperty("dbname"));
			} catch (NamingException | SQLException e) {
				System.err.println("MSM::FeedReader - Database connection could not be stablished.");
				System.exit(1);
			}
		}

		//authentication credentials for media if provided in the config file
		try {
			loadCredentials(params.getProperty("feedAuth", ""));
		} catch (NullPointerException ne) {
			System.err.println("MSM::FeedReader - Error when reading credentials for feed, no credentials loaded. "
					+ "If a source requires authentication related articles may not be downloaded correctly.");
		}
		
	}//end constructor


	public void processFeeds(String store, String type, String ffmpeg)
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
				float splitWindow = Float.valueOf(params.getProperty("multimediaFeed.splitWindowlangs", "5"));
				getMultimediaFeed(f, store, ffmpeg, splitWindow);
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

		System.err.println("FeadReader::getRssFeed -> parse feed "+f.getFeedURL()+" lastFetched: "+f.getLastFetchDate());
		String link = "";

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");				
		Date currentDate = new Date();

		Date lastFetchDate_date = MSMUtils.parseDate(f.getLastFetchDate());
		
		// reload language identification with the feed possible languages
		// loadAcceptedLangs(f.getLangs());
		SyndFeed feed = new SyndFeedImpl();
		try{
			InputStream stream;
			String url = f.getFeedURL().trim();
			
			if (url.startsWith("file://")){
				stream = new FileInputStream(url.replaceFirst("file://", ""));
			}
			else
			{
				CloseableHttpClient client = MSMUtils.httpClient();
				RequestConfig localConfig = RequestConfig.custom()
						.setCookieSpec(CookieSpecs.STANDARD)
						.build();				
				HttpGet get = new HttpGet(f.getFeedURL().trim());
				get.setConfig(localConfig);
				org.apache.http.HttpResponse response = client.execute(get);
				//try (CloseableHttpResponse response = client.execute(method);
				stream = response.getEntity().getContent();
			}
			SyndFeedInput input = new SyndFeedInput();
			input.setPreserveWireFeed(true);
			input.setAllowDoctypes(true);
			// try to read a feed.
			try {
				feed = input.build(new XmlReader(stream));
				// String ftype =feed.getFeedType();
			} catch (NoSuchElementException nsee ){
				System.err.println(
						"FeadReader::getRssFeed ->  Feed ERROR with " + f.getFeedURL() + " seems the feed returned empty :\n ");
				nsee.printStackTrace();
			} catch (FeedException | IOException fe) {
				System.err.println(
						"FeadReader::getRssFeed ->  Feed ERROR with " + f.getFeedURL() + " :\n ");
				fe.printStackTrace();
			} catch (Exception e) {
				System.err.println(
						"FeadReader::getRssFeed ->  Feed ERROR with when reading stream. " + f.getFeedURL() + " :\n ");
				e.printStackTrace();
			}
			
		} catch (IOException cpe) {			
			System.err.println(
					"FeadReader::getRssFeed ->  HTTP ERROR with " + f.getFeedURL() + " :\n " + cpe.getMessage());
			cpe.printStackTrace();
		} catch (KeyManagementException | NoSuchAlgorithmException| KeyStoreException ssle) {
			System.err.println("FeadReader::getRssFeed ->  HTTP client error (ssl related) " + f.getFeedURL() + " :\n " + ssle.getMessage());
			ssle.printStackTrace();
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

			int domCount=0;
			Matcher match = MSMUtils.duplicatedDomainInUrl.matcher(link);
			int matchStart = 0;
			while (match.find())
			{
				domCount=domCount+1;
				matchStart= match.start();
			}
			// check and correct malformed urls  of type https://x.y.comhttps:/x.y.com/page
			if (domCount > 1) {
				link = link.substring(matchStart);
				System.err.println("FeadReader::getRssFeed ->  WARN url with incorrect domain. Clean url: "+link);
			}
			
			if (store.equalsIgnoreCase("db"))
			{	
				if (MSMUtils.mentionsInDB(DBconn, link) > 0){
						System.err.println("FeadReader::getRssFeed -> entry already parsed "+link);
						continue;
				}
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
				
				// the following lines set the last fetch date 24 hours before the stored date. 
				// This is done in order to deal with feeds that publish news with past dates.
				// The 24 hour period is arbitrary.
				Calendar c = Calendar.getInstance();
				c.setTime(lastFetchDate_date);
				c.add(Calendar.DATE, -1);
				lastFetchDate_date=c.getTime();
				
				if ((!nullDate && pubDate.after(lastFetchDate_date)) || (nullDate && lastFetchDate_date.before(pubDate)) )
				{
					newEnts++;
					System.err.println("FeadReader::getRssFeed -> new entry "+date+" vs."+f.getLastFetchDate());
					System.err.println(link);
					//com.robbypond version.
					//final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;
					//final HtmlArticleExtractor htmlExtr = HtmlArticleExtractor.INSTANCE;
					//String text = htmlExtr.process(extractor, linkSrc);

					//normal kohlschuetter extractor call
					// parse the document into boilerpipe's internal data structure
					//final InputSource is = HTMLFetcher.fetch(linkSrc).toInputSource();
					
					final InputSource is = fetchHTML(linkSrc, f.getSrcDomain());
					TextDocument doc = new BoilerpipeSAXInput(is).getTextDocument();
					// perform the extraction/classification process on "doc"
					ArticleExtractor.INSTANCE.process(doc);
					//text = text.replaceAll("(?i)<p>", "").replaceAll("(?i)</p>", "\n\n").replaceAll("(?i)<br\\/?>","\n");

					//detect language
					String lang = LID.detectFeedLanguage(doc.getContent(), f.getLangs())[0];

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
							processFullArticle(doc,lang, pubDate, link, f.getSrcId(), store);
							//parseArticleForKeywords(doc,lang, pubDate, link, f.getSrcId(), store);
						}
					}
				}
			//	else
			//	{
			//		System.err.println("FeadReader::getFeed -> no new entries ");
			//	}

			} catch (IOException ioe) {	        
				ioe.printStackTrace();
				System.err.println("FeadReader::getRssFeed ->  ERROR when reading html a link ("+link+") - "+ioe.getMessage());
			} catch (BoilerpipeProcessingException | SAXException be){ //| URISyntaxException be) {			
				be.printStackTrace();
				System.err.println("FeadReader::getRssFeed ->  Boilerplate removal ERROR with"+f.getFeedURL()+" : "+be.getMessage());
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KeyManagementException | NoSuchAlgorithmException| KeyStoreException ssle) {
				System.err.println("FeadReader::getRssFeed ->  HTTP client error (ssl related) when reading a feed entry (" +link+") :\n "+ssle.getMessage());
				ssle.printStackTrace();
			} catch (AuthenticationException ae) {
				System.err.println("FeadReader::getRssFeed ->  HTTP client authentication error whe reading a feed entry (" +link+") :\n "+ae.getMessage());
				ae.printStackTrace();
			}
		}
		System.err.println("FeadReader::getRssFeed -> found "+newEnts+" new entries ");

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
			System.out.println("FeadReader::getRssFeed ->  ERROR when updating fetch time "+dateFormat.format(currentDate)+" : "+sqle.getMessage());
			//e.printStackTrace();
		}		
	}

	
	/**
	 * Core function of the class. Code to process a single feed
	 * @param splitWindow 
	 * 
	 * @param Feed f
	 * 
	 */
	private void getMultimediaFeed (Feed f, String store, String ffmpeg, float splitWindow){

		System.err.println("FeadReader::getFeed -> parse feed "+f.getFeedURL()+" lastFetched: "+f.getLastFetchDate());
		System.err.println("FeadReader::getFeed -> window length: "+splitWindow);

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
	
		// object to store the feed elements to process.
		List<multimediaElement> entries = new ArrayList<multimediaElement>();
		// xml parser for parsing the feed
		SAXBuilder sax = new SAXBuilder();
		XPathFactory xFactory = XPathFactory.instance();
		Document feed;		
		try{
			InputStream stream;
			//URL normalization
			UrlValidator defaultValidator = new UrlValidator(UrlValidator.ALLOW_2_SLASHES);
					
			if (defaultValidator.isValid(f.getFeedURL())) 
			{
				//this is legacy code, normaly the feed is stored somewhere in our HD.
				HttpClient client = MSMUtils.httpClient();
				HttpUriRequest method = new HttpGet(f.getFeedURL());
				org.apache.http.HttpResponse response = client.execute(method);
				//try (CloseableHttpResponse response = client.execute(method);
				stream = response.getEntity().getContent();					

			}
			else{
			    stream = new URL(f.getFeedURL()).openStream();
			}
			// try to read a feed.
			try {
				feed = sax.build(stream);
				XPathExpression<Element> expr = xFactory.compile("//saioa",
						Filters.element());
				List<Element> shows = expr.evaluate(feed);
				for (Element show : shows) {
					multimediaElement mme = new multimediaElement(show, ffmpeg);
					entries.add(mme);					
				}
			} catch (JDOMException | IOException e) {
				System.err.println( "FeadReader::getMultimediaFeed -> Feed ERROR with " + f.getFeedURL() + " : " + e.getMessage());
				//e.printStackTrace();								
			}
			stream.close();
		} catch (IOException cpe) {			
			System.err.println(
					"FeadReader::getFeed ->  HTTP ERROR with " + f.getFeedURL() + " : " + cpe.getMessage());
			cpe.printStackTrace();
		} catch (KeyManagementException | NoSuchAlgorithmException| KeyStoreException ssle) {
			System.err.println("FeadReader::getRssFeed ->  HTTP client error (ssl related) when reading html a link (" +link+") :\n "+ssle.getMessage());
			ssle.printStackTrace();
		}

		int newEnts =0;
		//System.err.println("FeadReader::getFeed -> feed type: "+feed type);
		for (multimediaElement entry : entries)
		{
			link = entry.getOriginURL();		
			//language comes from the transcription module (which in turn reads it from the DB info on the feed)
			String lang= entry.getLang();

			System.err.println("FeadReader::getFeed -> analysing entries - "+link+" ("+lang+")");

			if (store.equalsIgnoreCase("db"))
			{	
				if (MSMUtils.mentionsInDB(DBconn, link) > 0){
						System.err.println("FeadReader::getFeed -> entry already parsed "+link);
						continue;
				}
			}
			
			newEnts++;

				
			//if language accepted parse article for mentions. If found store them to DB or print them
			if (acceptedLangs.contains("all") || acceptedLangs.contains(lang))
			{
				if (kwrds.isEmpty())
				{
					System.err.println("MSM::FeadReader::getFeed ->no keywords provided. MSM will stop now, "
							+ "it doesn't make any sense to continue without keywords.");
					break;
				}
				else
				{
					try {
					    System.err.println("FeadReader::getMultimediaFeed - parsing multimedia feed for keywords");
					    entry.parseForKeywords(kwrds, kwrdPatterns, independentkwrds, dependentkwrds, anchorPattern, 60,store,DBconn,splitWindow);
					} catch (IOException e) {
						System.err.println("FeadReader::getMultimediaFeed -> XML parsing error when parsing "
											+entry.getTranscriptionURL()+" transcription file for entry "+entry.getShowURL());
						e.printStackTrace();
					} catch (JDOMException e) {
						System.err.println("FeadReader::getMultimediaFeed -> I/O error where reading "
								+entry.getTranscriptionURL()+" transcription file for entry "+entry.getShowURL());
						e.printStackTrace();
					}
				}
			}			 
		} //for 
		System.err.println("FeadReader::getMultimediaFeed -> found "+newEnts+" new entries ");
		
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
		boolean anchorFound = false;
		if (anchorPattern == null)
		{
			anchorFound=false;
		}
		else
		{
			anchorFound = anchorPattern.matcher(wholeText).find();
		}

		//System.err.println("MSM::FeedReader::parseArticleForKeywords - anchorPattern: "+anchorPattern.toString()
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
					System.err.println("MSM::FeedReader::parseArticleForKeywords - indpndnt keyword found,"
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
				
				//System.err.println("MSM::FeedReader::parseArticleForKeywords - independent key:"
				//	+k.getText()+" l="+k.getLang()+" pattern:"+kwrdPatterns.get(k.getId()).toString()+" document lang: "+lang);
				if(k.getLang().equalsIgnoreCase(lang) && kwrdFound)
				{	
					//System.err.println("MSM::FeedReader::parseArticleForKeywords - independent key found!!!: "+k.getText()+" id: "+k.getId());
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
						System.err.println("MSM::FeedReader::parseArticleForKeywords - dpndnt keyword found,"
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
						//System.err.println("MSM::FeedReader::parseArticleForKeywords - dependent key found!!!: "+k.getText()+" id: "+k.getId());						
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
					System.err.println("MSM::FeedReader::parseArticleForKeywords - mention2db: "+par);
				}
				else
				{
					System.out.println("MSM::FeedReader::parseArticleForKeywords - mention found!: "+par);
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
				String lang = LID.detectFeedLanguage(doc.getContent(), langs)[0];

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
			System.err.println("MSM::FeedReader::processFullArticle - 2db not implemented yet. "+link);
		}
		else
		{
			System.err.println("MSM::FeedReader::processFullArticle - article to stout: "+link);
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
		System.err.println("MSM::FeedReader - Accepted languages: "+acceptedLangs);
	}

	
	/**
	 * Load credentials from config string
	 * @param property
	 */
	private void loadCredentials(String property) {
		List<String> allCredentials=Arrays.asList(property.split(","));
		
		System.err.println("MSM::FeedReader - Found Credentials: "+allCredentials.size());
		
		for (String cred : allCredentials) {
			String[] split = cred.split("::");
			HashMap<String,String> usrpass = new HashMap<String,String>();
			usrpass.put(split[1],split[2]);
			credentials.put(split[0],usrpass); 
		}
		System.err.println("MSM::FeedReader - Credentials added for the following domains: "+credentials.keySet().toString());
	}
	
	/**
	 * Close connetion to DB.
	 */
	public void closeDBConnection()
	{
		try {
			DBconn.close();
		} catch (SQLException e) {
			System.err.println("MSM::FeedReader::closeDBConnection - ERROR when closing DB connection. "
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
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * @throws AuthenticationException 
	 */
	private InputSource fetchHTML(URL linkSrc, String domain) throws IOException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, AuthenticationException {
		
		final Pattern PAT_CHARSET = Pattern
				.compile("charset=([^; ]+)$");

		//HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();
		CloseableHttpClient client = MSMUtils.httpClient();
		RequestConfig localConfig = RequestConfig.custom()
				.setCookieSpec(CookieSpecs.STANDARD)
				.build();				
		
        URI linkUri = new URI(linkSrc.getProtocol(), linkSrc.getUserInfo(), linkSrc.getHost(), linkSrc.getPort(), linkSrc.getPath(), linkSrc.getQuery(), linkSrc.getRef()); 		
        HttpUriRequest get = new HttpGet(linkUri);    
        if (credentials.containsKey(domain)) {
        	Map.Entry<String,String> entry = credentials.get(domain).entrySet().iterator().next();
        	get.addHeader(new BasicScheme(StandardCharsets.UTF_8).authenticate(new UsernamePasswordCredentials(entry.getKey(),entry.getValue()), get, null));        	
        }
		CloseableHttpResponse response = client.execute(get);
		InputStream stream = response.getEntity().getContent();	
		
		//final URLConnection conn = linkSrc.openConnection();
		
		
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
