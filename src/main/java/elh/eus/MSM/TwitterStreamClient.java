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

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.Location;
import com.twitter.hbc.core.endpoint.Location.Coordinate;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import com.twitter.hbc.twitter4j.Twitter4jStatusClient;
import com.twitter.hbc.twitter4j.handler.StatusStreamHandler;
import com.twitter.hbc.twitter4j.message.DisconnectMessage;
import com.twitter.hbc.twitter4j.message.StallWarningMessage;

import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;


public class TwitterStreamClient {

	private Properties params = new Properties();	
	private String store = "";
	private List<String> acceptedLangs;
	private Set<Keyword> keywords;
	private List<Location> locations = new ArrayList<Location>();
	private List<Long> users = new ArrayList<Long>();
	private LangDetect LID;
	private Set<Long> census = new HashSet<Long>();
	
	private Set<Keyword> independentkwrds = new HashSet<Keyword>();
	private Set<Keyword> dependentkwrds = new HashSet<Keyword>();

	private static Pattern anchorPattern; //pattern for anchor kwrds. they are usually general terms.
	private HashMap<Integer,Pattern> kwrdPatterns = new HashMap<Integer,Pattern>(); //patterns for keywords.

	
	//private static Pattern retweetPattern = Pattern.compile("^RT[^\\p{L}\\p{M}\\p{Nd}]+.*");
	//private static Pattern user = Pattern.compile("@([\\p{L}\\p{M}\\p{Nd}_]{1,15})");
	private static Pattern urlPattern = Pattern.compile("\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
	
	public String getStore() {
		return store;
	}

	public void setStore(String store) {
		this.store = store;
	}

	StatusListener listener1 = new StatusListener() {
        @Override
        public void onStatus(Status status) 
        {
        	//if log is active store status into log file.
        	String logFilePath = params.getProperty("twitter_log", "no");
        	if (! logFilePath.equalsIgnoreCase("no"))
        	{
        		try {
					FileUtils.writeStringToFile(new File(logFilePath), status.toString());
				} catch (IOException e) {
					System.err.println("MSM::TwitterStreamClient -IO error when writing to twitter client log");
					e.printStackTrace();
				}
        	}
        	switch (getStore()) // print the message to stdout
			{
			case "stout": System.out.println(status.toString()); break;
			case "db":
				System.out.println("db - @" + status.getUser().getScreenName() + " - " + status.getText());
				processMention(status);
				break;
			case "solr": System.out.println("solr - @" + status.getUser().getScreenName() + " - " + status.getText()); break;
			} 
        }      
        
		@Override
        public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
            System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
        }

        @Override
        public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
            System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
        }

        @Override
        public void onScrubGeo(long userId, long upToStatusId) {
            System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
        }

        @Override
        public void onStallWarning(StallWarning warning) {
            System.out.println("Got stall warning:" + warning);
        }

        @Override
        public void onException(Exception ex) {
            ex.printStackTrace();
        }        

        
    };
    
	// A bare bones StatusStreamHandler, which extends listener and gives some extra functionality
	private StatusListener listener2 = new StatusStreamHandler() {
		@Override
		public void onStatus(Status status) {}

		@Override
		public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {}

		@Override
		public void onTrackLimitationNotice(int limit) {}

		@Override
		public void onScrubGeo(long user, long upToStatus) {}

		@Override
		public void onStallWarning(StallWarning warning) {}

		@Override
		public void onException(Exception e) {}

		@Override
		public void onDisconnectMessage(DisconnectMessage message) {
			System.err.println("MSM::TwitterStreamClient - Got disconnected from the stream:" + message);
			System.exit(5);
		}

		@Override
		public void onStallWarningMessage(StallWarningMessage warning) {}

		@Override
		public void onUnknownMessageType(String s) {}
	};

	/**
	 * 
	 * Constructor and main function. Starts a connections and gives the messages to the corresponding handlers.
	 * 
	 * @param config
	 * @param store
	 */
	public TwitterStreamClient (String config, String store, String parameters, String censusf, boolean twtLang)
	{
		try {
			params.load(new FileInputStream(new File(config)));
		} catch (FileNotFoundException fe){
			System.err.println("MSM::TwitterStreamClient - Config file not found "+config);
			System.exit(1);
		} catch (IOException ioe){
			System.err.println("MSM::TwitterStreamClient - Config file could not read "+config);
			System.exit(1);
		} 
		
		//where should the retrieved messages be stored [db|solr|stout]
		setStore(store);
		//Language identification
		loadAcceptedLangs(params.getProperty("langs", "all"));		
		LID = new LangDetect();
		
		/** Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */
		BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);
		BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>(1000);

		/** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
		Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
		StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
		
		//whether we should trust twitter language filter or not
		if (twtLang) {
			hosebirdEndpoint.languages(acceptedLangs);
		}
		
		
		
		/** TWITTER STREAMING API PARAMETER HANDLING */		
		//tracking terms
		if (parameters.equals("all") || parameters.equals("terms"))
		{
			List<String> terms = new ArrayList<String>();
			
			//Config file settings have preference over DB, if config file parameter is null then try the DB
			if (!params.getProperty("searchTerms","none").equalsIgnoreCase("none"))
			{
				terms = Arrays.asList(params.getProperty("searchTerms").split(","));	
				keywords = Keyword.createFromList(terms,acceptedLangs);
				System.err.println("MSM::TwitterStreamClient - retrieved "+keywords.size()+" keywords from config file");											
			}
			
			// If no search terms were defined in config file try to get search terms from the DB
			if(terms.isEmpty())
			{
				try{
					Connection conn = MSMUtils.DbConnection(
							params.getProperty("dbuser"), 
							params.getProperty("dbpass"), 
							params.getProperty("dbhost"), 
							params.getProperty("dbname")); 
					keywords= Keyword.retrieveFromDB(conn,"Twitter",params.getProperty("langs", "all"));			
					Set<String> kwrdSet = new HashSet<String>();
					for (Keyword k : keywords)
					{
						kwrdSet.add(k.getText());				
					}

					// null object and empty string ("") may be here due to the fields in DB. Remove them from keyword set. 
					kwrdSet.remove(null);
					kwrdSet.remove("");
					terms = new ArrayList<String>(kwrdSet);
					//searchTerms = Arrays.asList(kwrdSet).toString().replaceAll("(^\\[|\\]$)", "").replace(", ", ",").toLowerCase();
					System.err.println("MSM::TwitterStreamClient - retrieved "+keywords.size()+" keywords from DB");				

				} catch (Exception e){
					System.err.println("MSM::TwitterStreamClient - connection with the DB could not be established,"
							+ "MSM will try to read search terms from config file.");
					//e.printStackTrace();			
				}
			}//terms from db

			System.err.println("MSM::TwitterStreamClient - Search terms: "+terms.toString());

			//tracking terms
			if (!terms.isEmpty())
			{
				constructKeywordsPatterns();
				hosebirdEndpoint.trackTerms(terms);
			}
			else if (parameters.equals("terms"))
			{
				System.err.println("MSM::TwitterStreamClient - WARNING: no search terms could be found."
						+ "this can result in an Flood of tweets...");
			}

		} //tracking terms handling end
		
		//location parameters 		
		if (parameters.equals("all") || parameters.equals("geo"))
		{
			if (!params.getProperty("location", "none").equalsIgnoreCase("none"))
			{			
				List<String> locs = Arrays.asList(params.getProperty("location").split("::"));
				for (String s : locs)
				{
					System.err.println("MSM::TwitterStreamClient - location: "+s);
					String[] coords = s.split(",");
					Location loc = new Location(
							new Coordinate(Double.parseDouble(coords[0]),Double.parseDouble(coords[1])),
							new Coordinate(Double.parseDouble(coords[2]),Double.parseDouble(coords[3]))
							);
					locations.add(loc);
				}
			}
			System.err.println("MSM::TwitterStreamClient - retrieved "+locations.size()+" locations from config file");
			if (!locations.isEmpty())
			{
				hosebirdEndpoint.locations(locations);								
			}
			else if (parameters.equals("geo"))
			{
				System.err.println("MSM::TwitterStreamClient - WARNING: no geolocations could be found."
						+ "this can result in an Flood of tweets...");
			}
			
		}

		//users to follow parameters 		
		if (parameters.equals("all") || parameters.equals("users"))
		{				
			if (!params.getProperty("followings", "none").equalsIgnoreCase("none"))
			{			
				String[] follows = params.getProperty("followings").split(",");
				for (String s : follows)
				{
					//System.err.println("MSM::TwitterStreamClient - followings: "+s);
					users.add(Long.parseLong(s));
				}				
			}
			System.err.println("MSM::TwitterStreamClient - retrieved "+users.size()+" users from config file");
			if (!users.isEmpty())
			{
				hosebirdEndpoint.followings(users);				
			}
			else if (parameters.equals("users"))
			{
				System.err.println("MSM::TwitterStreamClient - WARNING: no users to follow could be found."
						+ "this can result in an Flood of tweets...");
			}
		}

		//if a census field is provided fill the census map
		if (MSMUtils.checkFile(censusf))
		{
			try {
				census = MSMUtils.loadOneColumnResource(new FileInputStream(censusf));
			} catch (FileNotFoundException fe){
				System.err.println("MSM::TwitterStreamClient - census file NOT FOUND, crawler will continue without census"+censusf);
			} catch (IOException ioe){
				System.err.println("MSM::TwitterStreamClient - census file COULD NOT BE READ, crawler will continue without census "+censusf);
			} 
		}
		else if (censusf!=null && censusf.equalsIgnoreCase("db"))
		{
			try {
				Connection conn = MSMUtils.DbConnection(
						params.getProperty("dbuser"), 
						params.getProperty("dbpass"), 
						params.getProperty("dbhost"), 
						params.getProperty("dbname"));
				census = Source.retrieveLocalAreaFromDB(conn, "twitter");				
			}catch (Exception e) {
				System.err.println("MSM::TwitterStreamClient - could not retrieve census from DB, "
						+ "the crawler will continue without census");			
			}
		}
		
		
		//END OF FILTER STREAM API PARAMETER HANDLING
		
		try {
			// These secrets should be read from a config file		
			String ckey = params.getProperty("consumerKey");
			String csecret = params.getProperty("consumerSecret");
			String token = params.getProperty("accessToken");
			String tsecret = params.getProperty("accesTokenSecret");
			//System.err.println("twitter data:"+ckey+" "+csecret+" "+token+" "+tsecret);	
			
			Authentication hosebirdAuth = new OAuth1(ckey, csecret, token, tsecret);
		
			//create the client
			ClientBuilder builder = new ClientBuilder()
			.hosts(hosebirdHosts)
			.authentication(hosebirdAuth)
			.endpoint(hosebirdEndpoint)
			.processor(new StringDelimitedProcessor(msgQueue))
			.eventMessageQueue(eventQueue);

			Client client = builder.build();

			// Create an executor service which will spawn threads to do the actual work of parsing the incoming messages and
			// calling the listeners on each message
			int numProcessingThreads = 4;
			ExecutorService service = Executors.newFixedThreadPool(numProcessingThreads);

			// Wrap our BasicClient with the twitter4j client
			Twitter4jStatusClient t4jClient = new Twitter4jStatusClient(
					client, msgQueue, Lists.newArrayList(listener1, listener2), service);

			// Establish a connection
			t4jClient.connect();
			for (int threads = 0; threads < numProcessingThreads; threads++) {
				// This must be called once per processing thread
				t4jClient.process();
			}

		} catch (Exception e) {
			System.err.println("MSM::TwitterStreamClient - Authentication problem");			
			e.printStackTrace();
			System.exit(1);
		}

	}

	/**
	 * Only messages in certain languages may be retrieved. This void load the list of allowed languages.
	 * @param property is a list of iso-639 codes separated by ',' chars. It can also contain the value 'all'
	 *        meaning that messages in all languages will be accepted
	 */
	private void loadAcceptedLangs(String property) {
		this.acceptedLangs=Arrays.asList(property.split(","));	
		System.err.println("MSM::TwitterStreamClient - Accepted languages: "+acceptedLangs);
	}
	
	
	/**
	 * This void creates Patterns for all the keywords and stores them in two structures depending
	 * if the keywords need anchors or not.
	 * 
	 */	
	private void constructKeywordsPatterns() {
		
		boolean anchors = false;
		
		if (this.keywords == null || this.keywords.isEmpty())
		{
			System.err.println ("elh-MSM::TwitterStreamClient - No keywords loaded");
			return;
			//System.exit(1);
		}

		StringBuilder sb_anchors = new StringBuilder();
		sb_anchors.append("\\b(");
		for (Keyword k : keywords)
		{
		        // capitalization must be respected if keywords are so specified.
		        // as of 2016/08/03 press mentions have to respect keyword capitalization. 
		        // as of 2016/09/09 twitter mentions may have to respect keyword capitalization depending on the key.
		        String pstr="\\b"+k.getText().replace('_',' ');			
			//if keyword has '#@' prefixes maintain '_' chars as they are specifically used in twitter
			if (k.getText().startsWith("#") || k.getText().startsWith("@"))
			{
				pstr = k.getText().toLowerCase();
			}
			
			if (! k.getCaseSensitiveSearch())
			{
			    pstr=pstr.toLowerCase();
			}

			if (k.isKword())
			{
				//create and store pattern;								
				Pattern p = Pattern.compile(pstr);
				System.err.println("MSM::TwitterStreamClient::constructKeywordPatterns - current:"+p.toString()+"  --> "+k.getText()+" | "+k.getLang()+" | "+k.getScreenTag()+" | ");

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
				sb_anchors.append(pstr).append("|");
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
	 * Function looks for keywords in a given text
	 * 
	 * @param text
	 * @param lang
	 * @return
	 */
	private Set<Keyword> parseTweetForKeywords(String text, String lang) {
				
		Set<Keyword> result = new HashSet<Keyword>();
		// this set controls that two keywords belonging to the same screen tag won't be assigned to a mention 
		Set<String> screenTagList = new HashSet<String>();

		// capitalization must be respected if keywords as so specified.
		// as of 2016/08/03 press mentions have to respect keyword capitalization. 
		String searchText = StringUtils.stripAccents(text).replace('\n', ' '); //.toLowerCase(); 
		String searchTextLC = searchText.toLowerCase();

		// delete urls, no keyword will be accepted if inside an url
		searchText = urlPattern.matcher(searchText).replaceAll("");
		searchTextLC = urlPattern.matcher(searchTextLC).replaceAll("");
		boolean anchorFound = false;
		if (anchorPattern == null)
		{
			anchorFound=false;
		}
		else
		{
			anchorFound = anchorPattern.matcher(searchText).find();
		}
	
		System.err.println("MSM::TwitterStreamClientReader::parseTweetForKeywords - independent:"+independentkwrds.size()
				+" - dependent: "+dependentkwrds.size()+"\n - searchText:"+searchText);
				
		
		//keywords that do not need any anchor
		for (Keyword k : independentkwrds)
		{				
			//System.err.println("MSM::TwitterStreamClient::parseTweetForKeywords - independent key:"
			//	+k.getText()+" l="+k.getLang()+" pattern:"+kwrdPatterns.get(k.getId()).toString());
			
			//check if a keyword with the same tag has been already matched, if so do not check the key.
			String currentScreenTag = k.getScreenTag();
			if (screenTagList.contains(currentScreenTag))
			{
				System.err.println("MSM::TwitterStreamClientReader::parseTweetForKeywords - indpndnt keyword found,"
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
			
			String kLang = k.getLang();
			
			
			if (kwrdFound && (kLang.equalsIgnoreCase(lang)|| kLang.equalsIgnoreCase("all") || lang.equalsIgnoreCase("unk")))
			//if((k.getLang().equalsIgnoreCase(lang) && kwrdPatterns.get(k.getId()).matcher(searchText).find())|| 
			//		(k.getLang().equalsIgnoreCase("all") && kwrdPatterns.get(k.getId()).matcher(searchText).find()))
			{	
				System.err.println("MSM::TwitterStreamClient::parseTweetForKeywords - independent key found!!!: "+k.getText()+" id: "+k.getId());
				result.add(k);
				screenTagList.add(currentScreenTag);					
			}								
		}			
		//keywords that need and anchor, only if anchors where found
		if (anchorFound)
		{
			for (Keyword k : dependentkwrds)
			{
				//check if a keyword with the same tag has been already matched, if so do not check the key.
				String currentScreenTag = k.getScreenTag();
				if (screenTagList.contains(currentScreenTag))
				{
					System.err.println("MSM::TwitterStreamClientReader::parseTweetForKeywords - dpndnt keyword found,"
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
				
				
				String kLang = k.getLang();
				//if ((k.getLang().equalsIgnoreCase(lang) && kwrdPatterns.get(k.getId()).matcher(searchText).find())||
				//(k.getLang().equalsIgnoreCase("all") && kwrdPatterns.get(k.getId()).matcher(searchText).find()))
				if (kwrdFound && (kLang.equalsIgnoreCase(lang)|| kLang.equalsIgnoreCase("all") || lang.equalsIgnoreCase("unk")))
				{
					System.err.println("MSM::TwitterStreamClient::parseTweetForKeywords - dependent key found!!!: "+k.getText()+" id: "+k.getId());					
					result.add(k);	
					screenTagList.add(k.getScreenTag());					
				}					
			}
		}	
		
		System.err.println("MSM::TwitteStreamClient::parseTweetForKeywords - keywords found: "+result.size());						
		
		return result;
	}
	    
	/**
	 * Process status coming from twitter: check for language and keywords, 
	 * If it passes the tests store it and its derivatives (the originals if it is a rt or a quote)
	 * 
	 * @param status
	 */
	private void processMention(Status status) {   
		String text = status.getText();
		String lang = status.getLang();			
		//we do not blindly trust twitter language identification, so we do our own checking.
		lang = LID.detectTwtLanguage(text, lang)[0];
		int success = 0;
		//language must be accepted
		if ((acceptedLangs.contains("all") || acceptedLangs.contains(lang) || lang.equalsIgnoreCase("unk")))// && (! retweetPattern.matcher(text).matches()))
		{				
			Set<Keyword> kwrds = parseTweetForKeywords(text,lang);
			boolean local = census.contains(status.getUser().getId());
			// all tweets in basque are considered local (2016:09:09)
			if (lang.equalsIgnoreCase("eu"))
			{
				local=true;
			}
			//if no keyword is found in the tweet it is discarded. 
			// This discards some valid tweets, as the keyword maybe in an attached link. 
			if (kwrds != null && !kwrds.isEmpty())
			{				
				Mention m = new Mention (status, lang, local);
				m.setKeywords(kwrds);
				success =storeMention(m, status);	
			}
			// If there is no keywords but locations or users are not empty,
			// it means we are not doing a keyword based crawling. 
			// In that case store all tweets in the database.
			else if (!locations.isEmpty() || !users.isEmpty())
			{
				Mention m = new Mention (status, lang, local);
				success =storeMention(m, status);					
			}
			System.err.println("MSM::TwitterStreamClient - mention and derivations stored. success: "+String.valueOf(success));				
		}
		else
		{
			System.err.println("MSM::TwitterStreamClient - mention discarded because of lang requirements. lang: "+lang+" - "+acceptedLangs.toString());				
		}

	}

	/**
	 *  Function store mention is responsible to store a mention and its retweets and quotes if needed.
	 *  Keyword-based searches are treated differently, because in the rest of the cases all tweets, 
	 *  rts and quotes will be stored 
	 *  
	 * @param m
	 * @param s
	 * @return int success
	 */
	private int storeMention(Mention m, Status s) {

		int success = 0;
		boolean kwordBasedSearch = !keywords.isEmpty();
		System.err.println("MSM::TwitterStreamClient -  storMention: "+kwordBasedSearch+" "+keywords.size());
		String lang =m.getLang();
		switch (getStore()) // print the message to stdout
		{				
		case "db":
			try {
				Connection conn = MSMUtils.DbConnection(
						params.getProperty("dbuser"),
						params.getProperty("dbpass"),
						params.getProperty("dbhost"),
						params.getProperty("dbname"));
				
				//if mention is in db 
				if (m.existsInDB(conn)>0)
				{
					System.err.println("MSM::TwitterStreamClient -  mention already in DB! "+m.getNativeId());
					break;
				}
				
				User u = s.getUser();
				Source author = new Source(u,census.contains(u.getId()));
				int authorStored = 0;
				int authorUpdated = 0;
				if (!author.existsInDB(conn))
				{
					authorUpdated = author.source2db(conn);
				}
				else
				{
					authorUpdated = author.updateLocation2db(conn);
				}
				success = m.mention2db(conn);
				System.err.println("MSM::TwitterStreamClient -  mention stored into the DB! "+success+" author: "+authorStored+" author update: "+authorUpdated);

				//orig status is used to stored original statuses from retweets and quotes
				Status origStatus = null;
				//mention is a retweet, so store the original tweet as well, or update it in case 
				//it is already in the database							
				if (m.getIsRetweet())
				{   				
					origStatus = s.getRetweetedStatus();        				
					System.err.println("MSM::TwitterStreamClient -  RT found!! ");
				}
				//mention is a quote, so store the original tweet as well, or update it in case 
				//it is already in the database							
				else if (m.getIsQuote())
				{
					origStatus = s.getQuotedStatus();
					System.err.println("MSM::TwitterStreamClient -  Quote found!! ");
				}

				//if mentions was a quote or a rt process it as well 
				if (origStatus != null)
				{
					//System.err.println("MSM::TwitterStreamClient - retweet found!!!"	);
					boolean local = census.contains(origStatus.getUser().getId());
					// all tweets in basque are considered local (2016:09:09)
					if (lang.equalsIgnoreCase("eu"))
					{
						local=true;
					}
					Mention m2 = new Mention(origStatus,lang,local);
					long mId = m2.existsInDB(conn);
					if (mId>=0)
					{
						m2.updateRetweetFavourites2db(conn, mId);									
					}				
					// there are no keywords to look for so all the tweets are to be stored.
					else if (!kwordBasedSearch)
					{
						User u2 = origStatus.getUser();
						Source author2 = new Source(u2,local);
						authorStored = 0;
						authorUpdated = 0;
						if (!author2.existsInDB(conn))
						{
							authorStored = author2.source2db(conn);
						}
						else
						{
							authorUpdated = author.updateLocation2db(conn);
						}
						success = m2.mention2db(conn);									
					}
					// keyword-based search only accept original rt or quoted text if it also contains keywords 
					else 
					{
						Set<Keyword> quotedKwrds = parseTweetForKeywords(origStatus.getText(),lang);
						if (quotedKwrds != null && !quotedKwrds.isEmpty())
						{
							//System.err.println("MSM::TwitterStreamClient - retweet found!!!"	);
							m2.setKeywords(quotedKwrds);
							User u2 = origStatus.getUser();
							Source author2 = new Source(u2,local);
							authorStored = 0;
							authorUpdated = 0;
							if (!author2.existsInDB(conn))
							{
								authorStored = author2.source2db(conn);
							}
							else
							{
								authorUpdated = author.updateLocation2db(conn);
							}
							success = m2.mention2db(conn);
						}							
					}	
					System.err.println("MSM::TwitterStreamClient - rt|quoted tweet mention stored into the DB!"+success+" author: "+authorStored+" author update: "+authorUpdated);        			        				
				}
				conn.close();
				break;
			} catch (SQLException sqle) {
				System.err.println("MSM::TwitterStreamClient - connection with the DB could not be established");
				sqle.printStackTrace();
			} catch (Exception e) {
				System.err.println("MSM::TwitterStreamClient - error when storing mention");
				e.printStackTrace();
			}
			break;
		case "stout": 
			System.out.println("------ Mention found! ------ \n");
			m.print();
			System.out.println("---------\n");
			break;
		case "solr": success = m.mention2solr(); break;
		}
		return success;
	}
     
}

