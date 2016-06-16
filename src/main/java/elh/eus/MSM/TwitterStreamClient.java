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
import com.twitter.hbc.core.endpoint.StatusesSampleEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.BasicClient;
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
import java.util.regex.Matcher;
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
	
	private Set<Keyword> independentkwrds = new HashSet<Keyword>();
	private Set<Keyword> dependentkwrds = new HashSet<Keyword>();

	private static Pattern anchorPattern; //pattern for anchor kwrds. they are usually general terms.
	private HashMap<Integer,Pattern> kwrdPatterns = new HashMap<Integer,Pattern>(); //patterns for keywords.

	
	private static Pattern retweetPattern = Pattern.compile("^RT[^\\p{L}\\p{M}\\p{Nd}]+.*");
	
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
					System.err.println("elh-MSM::TwitterStreamClient -IO error when writing to twitter client log");
					e.printStackTrace();
				}
        	}
        	switch (getStore()) // print the message to stdout
			{
			case "stout": System.out.println(status.toString()); break;
			case "db":
				System.out.println("db - @" + status.getUser().getScreenName() + " - " + status.getText());
				storeMention(status);
				break;
			case "solr": System.out.println("solr - @" + status.getUser().getScreenName() + " - " + status.getText()); break;
			} 
        }

        private void storeMention(Status status) {        	
			String text = status.getText();
			String lang = status.getLang();			
			//we do not blindly trust twitter language identification, so we do our own checking.
			lang = LID.detectTwtLanguage(text, lang);
			
			//language must be accepted and tweet must not be a retweet
			if ((acceptedLangs.contains("all") || acceptedLangs.contains(lang) || lang.equalsIgnoreCase("unk")))// && (! retweetPattern.matcher(text).matches()))
			{				
				Set<Keyword> kwrds = parseTweetForKeywords(text,lang);
				//if no keyword is found in the tweet it is discarded. 
				// This discards some valid tweets, as the keyword maybe in an attached link. 
				if (kwrds != null && !kwrds.isEmpty())
				{
					Mention m = new Mention (status, lang);
					m.setKeywords(kwrds);
					int success =1;
					switch (getStore()) // print the message to stdout
					{				
					case "db":
						try {
							Connection conn = Utils.DbConnection(
									params.getProperty("dbuser"),
									params.getProperty("dbpass"),
									params.getProperty("dbhost"),
									params.getProperty("dbname"));
							Source author = new Source(status.getUser().getId(), status.getUser().getScreenName(), "Twitter","",-1);
							int authorStored = 0;
							if (!author.existsInDB(conn))
							{
								authorStored = author.source2db(conn);
							}
							success = m.mention2db(conn);
							
							System.err.println("elh-MSM::TwitterStreamClient - mention stored into the DB!"+success+" "+authorStored);
							
							//mention is a retweet, so store the original tweet as well, or update it in case 
							//it is already in the database							
							if (m.getIsRetweet())
							{
								//System.err.println("elh-MSM::TwitterStreamClient - retweet found!!!"	);
								Mention m2 = new Mention(status.getRetweetedStatus(),lang);
								m2.setKeywords(kwrds);
								long mId = m2.existsInDB(conn);
								if (mId>=0)
								{
									m2.updateRetweetFavouritesInDB(conn, mId);									
								}
								else
								{
									Source author2 = new Source(status.getUser().getId(), status.getUser().getScreenName(), "Twitter","",-1);
									authorStored = 0;
									if (!author2.existsInDB(conn))
									{
										authorStored = author2.source2db(conn);
									}
									success = m2.mention2db(conn);									
								}
								System.err.println("elh-MSM::TwitterStreamClient - retweeted mention stored into the DB!"+success+" "+authorStored);
							}
							conn.close();
							break;
						} catch (SQLException sqle) {
							System.err.println("elh-MSM::TwitterStreamClient - connection with the DB could not be established");
							sqle.printStackTrace();
						} catch (Exception e) {
							System.err.println("elh-MSM::TwitterStreamClient - error when storing mention");
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
				}
				// If there is no keywords but locations or users are not empty,
				// it means we are not doing a keyword based crawling. 
				// In that case store all tweets in the database.
				else if (!locations.isEmpty() || !users.isEmpty())
				{
					Mention m = new Mention (status, lang);
					int success =1;
					switch (getStore()) // print the message to stdout
					{				
					case "db":
						try {
							Connection conn = Utils.DbConnection(
									params.getProperty("dbuser"),
									params.getProperty("dbpass"),
									params.getProperty("dbhost"),
									params.getProperty("dbname"));
							Source author = new Source(status.getUser());
							int authorStored = 0;
							if (!author.existsInDB(conn))
							{
								authorStored = author.source2db(conn);
							}
							success = m.mention2db(conn);
							System.err.println("elh-MSM::TwitterStreamClient -  mention stored into the DB!"+success+" "+authorStored);
							
							//mention is a retweet, so store the original tweet as well, or update it in case 
							//it is already in the database							
							if (m.getIsRetweet())
							{
								Status rtStatus = status.getRetweetedStatus();
								//System.err.println("elh-MSM::TwitterStreamClient - retweet found!!!"	);								
								Mention m2 = new Mention(rtStatus,lang);								
								long mId = m2.existsInDB(conn);
								if (mId>=0)
								{
									System.err.println("elh-MSM::TwitterStreamClient - retweet - original already in DB"	);																	
									m2.updateRetweetFavouritesInDB(conn, mId);									
								}
								else
								{
									System.err.println("elh-MSM::TwitterStreamClient - retweet  - original new, add to DB"	);																	
									//m2.setKeywords(kwrds);
									Source author2 = new Source(rtStatus.getUser());
									authorStored = 0;
									if (!author2.existsInDB(conn))
									{
										authorStored = author2.source2db(conn);
									}
									success = m2.mention2db(conn);									
								}
								System.err.println("elh-MSM::TwitterStreamClient - retweeted mention stored into the DB!"+success+" "+authorStored);
							}
							conn.close();
							break;
						} catch (SQLException sqle) {
							System.err.println("elh-MSM::TwitterStreamClient - connection with the DB could not be established");
							sqle.printStackTrace();
						} catch (Exception e) {
							System.err.println("elh-MSM::TwitterStreamClient - error when storing mention");
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
				}
			}
			else
			{
				System.err.println("elh-MSM::TwitterStreamClient - mention discarded because of lang requirements. lang: "+lang+" - "+acceptedLangs.toString());				
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
	public TwitterStreamClient (String config, String store, String parameters)
	{
		try {
			params.load(new FileInputStream(new File(config)));
		} catch (FileNotFoundException fe){
			System.err.println("elh-MSM::TwitterStreamClient - Config file not found "+config);
			System.exit(1);
		} catch (IOException ioe){
			System.err.println("elh-MSM::TwitterStreamClient - Config file could not read "+config);
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
				System.err.println("elh-MSM::TwitterStreamClient - retrieved "+keywords.size()+" keywords from config file");											
			}
			
			// If no search terms were defined in config file try to get search terms from the DB
			if(terms.isEmpty())
			{
				try{
					Connection conn = Utils.DbConnection(
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
					System.err.println("elh-MSM::TwitterStreamClient - retrieved "+keywords.size()+" keywords from DB");				

				} catch (Exception e){
					System.err.println("elh-MSM::TwitterStreamClient - connection with the DB could not be established,"
							+ "MSM will try to read search terms from config file.");
					//e.printStackTrace();			
				}
			}//terms from db

			System.err.println("elh-MSM::TwitterStreamClient - Search terms: "+terms.toString());

			//tracking terms
			if (!terms.isEmpty())
			{
				constructKeywordsPatterns();
				hosebirdEndpoint.trackTerms(terms);
			}
			else if (parameters.equals("terms"))
			{
				System.err.println("elh-MSM::TwitterStreamClient - WARNING: no search terms could be found."
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
					System.err.println("elh-MSM::TwitterStreamClient - location: "+s);
					String[] coords = s.split(",");
					Location loc = new Location(
							new Coordinate(Double.parseDouble(coords[0]),Double.parseDouble(coords[1])),
							new Coordinate(Double.parseDouble(coords[2]),Double.parseDouble(coords[3]))
							);
					locations.add(loc);
				}
			}
			System.err.println("elh-MSM::TwitterStreamClient - retrieved "+locations.size()+" locations from config file");
			if (!locations.isEmpty())
			{
				hosebirdEndpoint.locations(locations);								
			}
			else if (parameters.equals("geo"))
			{
				System.err.println("elh-MSM::TwitterStreamClient - WARNING: no geolocations could be found."
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
					//System.err.println("elh-MSM::TwitterStreamClient - followings: "+s);
					users.add(Long.parseLong(s));
				}				
			}
			System.err.println("elh-MSM::TwitterStreamClient - retrieved "+users.size()+" users from config file");
			if (!users.isEmpty())
			{
				hosebirdEndpoint.followings(users);				
			}
			else if (parameters.equals("users"))
			{
				System.err.println("elh-MSM::TwitterStreamClient - WARNING: no users to follow could be found."
						+ "this can result in an Flood of tweets...");
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
			System.err.println("elh-MSM::TwitterStreamClient - Authentication problem");			
			e.printStackTrace();
			System.exit(1);
		}

	}

	private void loadAcceptedLangs(String property) {
		this.acceptedLangs=Arrays.asList(property.split(","));	
		System.err.println("elh-MSM::TwitterStreamClient - Accepted languages: "+acceptedLangs);
	}
	
	
	/**
	 * This void creates Patterns for all the keywords and stores them in two structures depending
	 * if the keywords need anchors or not.
	 * 
	 */	
	private void constructKeywordsPatterns() {
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
			//create and store pattern;
			Pattern p = Pattern.compile("\\b"+k.getText().replace('_',' ').toLowerCase());
			//System.err.println("elh-MSM::TwitterStreamClient::constructKeywordPatterns - currentPattern:"+p.toString());

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
	
	
	/**
	 * Function looks for keywords in a given text
	 * 
	 * @param text
	 * @param lang
	 * @return
	 */
	private Set<Keyword> parseTweetForKeywords(String text, String lang) {
				
		Set<Keyword> result = new HashSet<Keyword>();
		
		if (anchorPattern == null)
		{
			return result;
		}
		String searchText = StringUtils.stripAccents(text).toLowerCase().replace('\n', ' '); 
		boolean anchorFound = anchorPattern.matcher(searchText).find();
	
		System.err.println("elh-MSM::TwitterStreamClientReader::parseTweetForKeywords - independent:"+independentkwrds.size()
				+" - dependent: "+dependentkwrds.size()+"\n - searchText:"+searchText);
				
		
		//keywords that do not need any anchor
		for (Keyword k : independentkwrds)
		{				
			//System.err.println("elh-MSM::TwitterStreamClient::parseTweetForKeywords - independent key:"
			//	+k.getText()+" l="+k.getLang()+" pattern:"+kwrdPatterns.get(k.getId()).toString());
			String kLang = k.getLang();
			if ((kwrdPatterns.get(k.getId()).matcher(searchText).find()) && 
					(kLang.equalsIgnoreCase(lang)|| kLang.equalsIgnoreCase("all") || lang.equalsIgnoreCase("unk")))
			//if((k.getLang().equalsIgnoreCase(lang) && kwrdPatterns.get(k.getId()).matcher(searchText).find())|| 
			//		(k.getLang().equalsIgnoreCase("all") && kwrdPatterns.get(k.getId()).matcher(searchText).find()))
			{	
				System.err.println("elh-MSM::TwitterStreamClient::parseTweetForKeywords - independent key found!!!: "+k.getText()+" id: "+k.getId());
				result.add(k);
			}								
		}			
		//keywords that need and anchor, only if anchors where found
		if (anchorFound)
		{
			for (Keyword k : dependentkwrds)
			{
				String kLang = k.getLang();
				//if ((k.getLang().equalsIgnoreCase(lang) && kwrdPatterns.get(k.getId()).matcher(searchText).find())||
				//(k.getLang().equalsIgnoreCase("all") && kwrdPatterns.get(k.getId()).matcher(searchText).find()))
				if ((kwrdPatterns.get(k.getId()).matcher(searchText).find()) && 
							(kLang.equalsIgnoreCase(lang)|| kLang.equalsIgnoreCase("all") || lang.equalsIgnoreCase("unk")))
				{
					System.err.println("elh-MSM::TwitterStreamClient::parseTweetForKeywords - dependent key found!!!: "+k.getText()+" id: "+k.getId());						
					result.add(k);
				}					
			}
		}	
		
		System.err.println("elh-MSM::TwitteStreamClient::parseTweetForKeywords - keywords found: "+result.size());						
		
		return result;
	}
	    
	
}

