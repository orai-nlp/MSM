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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.naming.NamingException;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

//import org.jdom2.JDOMException;




/**
 * Main class of elh-crawler, the elhuyar crawling module
 * 
 *
 * @author isanvi
 * @version 2015-04-24
 *
 */
public class CLI {
	/**
	 * Get dynamically the version of elh-MSM by looking at the MANIFEST
	 * file.
	 */
	private final String version = getVersion();
	/**
	 * Name space of the arguments provided at the CLI.
	 */
	private Namespace parsedArguments = null;
	/**
	 * Argument parser instance.
	 */
	private ArgumentParser argParser = ArgumentParsers.newFor("MSM-" + version + ".jar")
			.build()
				.defaultHelp(true)
				.description("MSM-" + version + " is a crawling module developed by Elhuyar Foundation.\n");
	/**
	 * Sub parser instance.
	 */
	private Subparsers subParsers = argParser.addSubparsers().help(
			"sub-command help");
	
	/**
	 * The parser that manages the twitter crawler sub-command.
	 */
	private Subparser tweetCrawlParser;
	
	/**
	 * The parser that manages the twitter crawler sub-command.
	 */
	private Subparser gplusCrawlParser;

	
	/**
	 * The parser that manages the feed reader sub-command.
	 */	
	private Subparser feedReaderParser;
	
	/**
	 * The parser that manages the influence tagger sub-command.
	 */
	private Subparser influenceTaggerParser;

	/**
	 * The parser that manages the twitter user info sub-command.
	 */
	private Subparser twitterUserParser;
	
	/**
	 * The parser that manages the geocoding of the user locations.
	 */
	private Subparser userLocationGeocoderParser;
	
	/**
	 * Construct a CLI object with the three sub-parsers to manage the command
	 * line parameters.
	 */
	public CLI() {
		tweetCrawlParser = subParsers.addParser("twitter").help("Twitter Stream crawling CLI");
		loadTwitterCrawlerParameters();
		gplusCrawlParser = subParsers.addParser("gplus").help("gplus crawling CLI");
		loadGplusCrawlerParameters();
		feedReaderParser = subParsers.addParser("feed").help("Feed reaader CLI");
		loadFeedReaderParameters();
		influenceTaggerParser = subParsers.addParser("influence").help("Influence tagger CLI");
		loadInfluenceTaggerParameters();
		twitterUserParser = subParsers.addParser("twtUser").help("Twitter user info CLI");
		loadTwitterUserInfoParameters();
		userLocationGeocoderParser = subParsers.addParser("geocode").help("Geocoder for twitter user locations info CLI");
		loadUserLocationGeocoderParameters();

	}
	
	
	private void loadGplusCrawlerParameters() {
		// TODO Auto-generated method stub
		gplusCrawlParser.addArgument("-c", "--config")
		.required(true)
		.help("Configuration file that contains the necessary parameters to connect to the twitter public "
				+ "stream for crawling.\n");
		gplusCrawlParser.addArgument("-p", "--params")		
		.choices("terms", "users", "geo", "all")
		.setDefault("all")
		.help("Search parameters to use when connecting to the Twitter Streaming API. Parameter values are"
				+ "either stored in the database or specified in the configuration file.\n"
				+ "\t - \"terms\" : terms to track.\n"
				+ "\t - \"users\" : users to follow.\n"
				+ "\t - \"geo\" : bounding boxes defining geographic areas\n"
				+ "\t - \"all\" : check for all of the previous parameters\n");
		gplusCrawlParser.addArgument("-s", "--store")		
		.choices("stout", "db", "solr")
		.setDefault("stout")
		.help("Whether tweets shall be stored in a database, an Apache solr Index or printed to stdout (default).\n"
				+ "\t - \"stout\" : standard output\n"
				+ "\t - \"db\" : standard output\n"
				+ "\t - \"solr\" : standard output\n");
	}


	private void gPlusCrawler() {
		// TODO Auto-generated method stub
		String cfg = parsedArguments.getString("config");
		String store = parsedArguments.getString("store");
		String params = parsedArguments.getString("params");
		
		try {
			gPlusClient gplusClient = new gPlusClient(cfg, store,params);
		} catch (Exception e) {			
			e.printStackTrace();
		} 		

	}


	/**
	 * Main entry point of elh-crawler.
	 *
	 * @param args
	 * the arguments passed through the CLI
	 * @throws IOException
	 * exception if input data not available
	 */
	public static void main(final String[] args) throws IOException {
		CLI cmdLine = new CLI();
		cmdLine.parseCLI(args);
	}
	
	
	/**
	 * Parse the command interface parameters with the argParser.
	 *
	 * @param args
	 * the arguments passed through the CLI
	 * @throws IOException
	 * exception if problems with the incoming data
	 */
	public final void parseCLI(final String[] args) throws IOException {
		try {
			parsedArguments = argParser.parseArgs(args);
			System.err.println("CLI options: " + parsedArguments);
			if (args[0].equals("twitter")) {
				twitterCrawler();
			} 
			else if (args[0].equals("gplus")) {
				gPlusCrawler();
			} 
			else if (args[0].equals("feed")) {
				feedReader();
			} 
			else if (args[0].equals("influence")) {
				tagInfluence();
			}
			else if (args[0].equals("geocode")) {
				tagGeoCode();
			}
			else if (args[0].equals("twtUser")) {
				twtUserUInfo();
			}
			
		} catch (ArgumentParserException e) {
			argParser.handleError(e);
			System.out.println("Run java -jar target/MSM-" + version
					+ ".jar (twitter|feed|influence|twtUser|geocode) -help for details");
			System.exit(1);
		}
	}
	
	public final void twitterCrawler()
	{
		String cfg = parsedArguments.getString("config");
		String store = parsedArguments.getString("store");
		String params = parsedArguments.getString("params");
		String census = parsedArguments.getString("census");
		
		try {
			TwitterStreamClient twitterClient = new TwitterStreamClient(cfg, store,params,census);
		} catch (Exception e) {			
			e.printStackTrace();
		} 		
	}
	
	
	public final void feedReader()
	{
		String cfg = parsedArguments.getString("config");
		String store = parsedArguments.getString("store");
		String url = parsedArguments.getString("url");
		String census = parsedArguments.getString("census");
		String type = parsedArguments.getString("type");
		String ffmpeg = parsedArguments.getString("ffmpeg");


		Properties params = new Properties();
		try {
			params.load(new FileInputStream(new File(cfg)));
		} catch (FileNotFoundException fe){
			System.err.println("MSM::CLI (feedReader) - Config file not found "+cfg);
			System.exit(1);
		} catch (IOException ioe){
			System.err.println("MSM::CLI (feedReader) - Config file could not read "+cfg);
			System.exit(1);
		} 				
		
		
		Set<Keyword> kwrdList = new HashSet<Keyword>();
		Set<Feed> feedList = new HashSet<Feed>();

		try {
			// load feeds from db
			if (url.equalsIgnoreCase("db"))
			{
				Connection conn = MSMUtils.DbConnection(params.getProperty("dbuser"),
													params.getProperty("dbpass"),
													params.getProperty("dbhost"),
													params.getProperty("dbname"));
				feedList = Feed.retrieveFromDB(conn, type);
				//retrieve keywords from DB. For the moment both multimedia and press sources use keywords of "press" type
				kwrdList = Keyword.retrieveFromDB(conn, "press", params.getProperty("langs", "all"));
				conn.close();
			}
			else //load feeds from command line
			{
				String[] srcSplit = url.split("\\s*,\\s*");
				for (String feed : srcSplit)
				{
					feedList.add(new Feed(feed));
				}				
			}
			//last resort try to load feed from config file
			if (feedList.isEmpty())
			{
				String fsource = params.getProperty("feedURL", "none");
				String[] srcSplit = fsource.split("\\s*,\\s*");
				for (String feed : srcSplit)
				{
					feedList.add(new Feed(feed));
				}	
			}
			//no source feed was found hence exit
			if (feedList.isEmpty())
			{
				System.err.println("ERROR: either there no feed or no keywords were specified."
						+ "Feed reader can not continue.");
				System.exit(1);
			}
			else
			{	
				//if no keyword was found try to load them from config file
				if (kwrdList.isEmpty())
				{					
					String kwrds = params.getProperty("searchTerms", "none");
					if (!kwrds.equalsIgnoreCase("none"))
					{
						String[] kwrdSplit = kwrds.split("\\s*,\\s*");
						for (String kwrd : kwrdSplit)
						{
							kwrdList.add(new Keyword(kwrd,"all"));
						}
					}
					// if no keyword is found in config file try to load them from the database as a last resort.
					else{
						Connection conn = MSMUtils.DbConnection(params.getProperty("dbuser"),
								params.getProperty("dbpass"),
								params.getProperty("dbhost"),
								params.getProperty("dbname"));
						
						//retrieve keywords from DB. For the moment both multimedia and press sources use keywords of "press" type
						kwrdList = Keyword.retrieveFromDB(conn, "press", params.getProperty("langs", "all"));
						conn.close();
					}
				}
				
				
				System.err.println("MSM::FeedReader (CLI) - "+feedList.size()+" "+type+" feeds and "+kwrdList.size()+" keywords");
				FeedReader fReader = new FeedReader(cfg, feedList, kwrdList, store, census);
				fReader.processFeeds(store, type, ffmpeg);
			}
			
		} catch (SQLException se) {			
			se.printStackTrace();	
			System.err.println("MSM::FeedReader (CLI) - ERROR when connecting to the DB: "+params.getProperty("dbuser")+" "+params.getProperty("dbpass")+" "+params.getProperty("dbhost")+" "+params.getProperty("dbname"));
		} catch (Exception e) {			
			e.printStackTrace();			
		} 
		
		
	}
	
	/**
	 * Influence tagging interface
	 * 
	 * */
	public final void tagInfluence()
	{
		String cfg = parsedArguments.getString("config");	
		String sources = parsedArguments.getString("sources");	
		boolean db = parsedArguments.getBoolean("database");
		String type = parsedArguments.getString("type");
		String opt = parsedArguments.getString("which");
		Integer limit = parsedArguments.getInt("limit");
		
		Properties params = new Properties();
		try {
			params.load(new FileInputStream(new File(cfg)));
		} catch (FileNotFoundException fe){
			System.err.println("MSM::InfluenceTagger - Config file not found "+cfg);
			System.exit(1);
		} catch (IOException ioe){
			System.err.println("MSM::InfluenceTagger - Config file could not read "+cfg);
			System.exit(1);
		} 				
		
		
		Set<Source> sourceList = new HashSet<Source>();
		InfluenceTagger infTagger = new InfluenceTagger(cfg, db);
		try {
			//load source locations from db
			if (sources.equalsIgnoreCase("db"))
			{
				Connection conn = MSMUtils.DbConnection(params.getProperty("dbuser"),
													params.getProperty("dbpass"),
													params.getProperty("dbhost"),
													params.getProperty("dbname"));
				sourceList = Source.retrieveFromDB(conn,type,opt,limit);
				//System.err.println("MSM::Influcence CLI (db): sources found to look for their influence: "+sourceList.size());				
				infTagger.tagInfluence(sourceList);
				conn.close();
			}
			else //load locations from command line arguments
			{
				String[] srcSplit = sources.split("\\s*,\\s*");
				for (String src : srcSplit)
				{
					sourceList.add(new Source(src));
				}
				//System.err.println("MSM::Influcence CLI (commandline): sources found to look for their influence: "+sourceList.size());
				infTagger.tagInfluence(sourceList);				
			}
			
		} catch (Exception e) {			
			e.printStackTrace();
		} 
		
		if (db) // store influences into the database
		{
			try {
				Connection conn = MSMUtils.DbConnection(params.getProperty("dbuser"),
						params.getProperty("dbpass"),
						params.getProperty("dbhost"),
						params.getProperty("dbname"));
				int count = infTagger.influence2db(sourceList, conn);
				System.out.println("influence for "+count+" sources stored in the database");
				conn.close();
			} catch (NamingException | SQLException e) {
				System.err.println("MSM::InfluenceTagger - Error when storing influence in the DB ");
				e.printStackTrace();
			} 					
		}
		else // print influence to stdout
		{
			for (Source src : sourceList)
			{
				System.out.println("Src: "+src.getScreenName()+" - influence:"+src.getInfluence());
			}
		}
				
	}
	
	/**
	 * Geocode tagging interface
	 * 
	 * */
	public final void tagGeoCode()
	{
		String cfg = parsedArguments.getString("config");	
		String sources = parsedArguments.getString("sources");	
		boolean db = parsedArguments.getBoolean("database");
		String type = parsedArguments.getString("type");
		String opt = parsedArguments.getString("which");
		Integer limit = parsedArguments.getInt("limit");
		String API = parsedArguments.getString("api");

		
		Properties params = new Properties();
		try {
			params.load(new FileInputStream(new File(cfg)));
		} catch (FileNotFoundException fe){
			System.err.println("MSM::tagGeoCode - Config file not found "+cfg);
			System.exit(1);
		} catch (IOException ioe){
			System.err.println("MSM::tagGeoCode - Config file could not read "+cfg);
			System.exit(1);
		} 				
		
		
		Set<Source> sourceList = new HashSet<Source>();
		geoCode geoTagger = new geoCode(cfg, db);
		if (!API.equalsIgnoreCase("all")){
			geoTagger.selectAPI(API);
		}
		try {
			
			if (sources.equalsIgnoreCase("db"))
			{
				Connection conn = MSMUtils.DbConnection(params.getProperty("dbuser"),
													params.getProperty("dbpass"),
													params.getProperty("dbhost"),
													params.getProperty("dbname"));
				sourceList = Source.retrieveForGeoCodingFromDB(conn,type,opt,limit);
				System.err.println("MSM::Influcence CLI (db): sources found to look for their location: "+sourceList.size());				
				geoTagger.tagGeoCode(sourceList);
				conn.close();
			}
			else //try to get locations from command line or config file
			{
				String[] srcSplit = sources.split("::");
				//create dummy sources to work on.
				int n=1;
				for (String src : srcSplit)
				{
					Source s = new Source("usr_"+String.valueOf(n));
					s.setLocation(src);
					sourceList.add(s);					
					n++;
				}
				//System.err.println("MSM::Influcence CLI (commandline): sources found to look for their influence: "+sourceList.size());
				geoTagger.tagGeoCode(sourceList);				
			}
			
		} catch (Exception e) {			
			e.printStackTrace();
		} 
		
		if (db) // store influences into the database
		{
			try {
				Connection conn = MSMUtils.DbConnection(params.getProperty("dbuser"),
						params.getProperty("dbpass"),
						params.getProperty("dbhost"),
						params.getProperty("dbname"));
				int count = geoTagger.geocode2db(sourceList, conn);
				System.out.println("geolocation for "+count+" sources stored in the database");
				conn.close();
			} catch (NamingException | SQLException e) {
				System.err.println("MSM::tagGeoCode - Error when storing geolocation in the DB ");
				e.printStackTrace();
			} 					
		}
		else // print influence to stdout
		{
			System.out.println("Src_screenName\tLocation\tgeolocation");
			for (Source src : sourceList)
			{
				System.out.println(src.getScreenName()+"\t"+src.getLocation()+"\t"+src.getGeoInfo());
			}
		}
				
	}

	/**
	 * 
	 * Twitter user information retrieval interface
	 * 
	 * */
	public final void twtUserUInfo()
	{
		String cfg = parsedArguments.getString("config");
		String store = parsedArguments.getString("store");
		Integer limit = parsedArguments.getInt("limit");

		//String params = parsedArguments.getString("params");
		
		try {
			TwtUserInfo userInfoClient = new TwtUserInfo(cfg, store,limit);
		} catch (Exception e) {			
			e.printStackTrace();
		} 		
	}
	
	
	public final void loadTwitterCrawlerParameters()
	{
		tweetCrawlParser.addArgument("-c", "--config")
		.required(true)
		.help("Configuration file that contains the necessary parameters to connect to the twitter public "
				+ "stream for crawling.\n");
		tweetCrawlParser.addArgument("-p", "--params")		
		.choices("terms", "users", "geo", "all")
		.setDefault("all")
		.help("Search parameters to use when connecting to the Twitter Streaming API. Parameter values are"
				+ "either stored in the database or specified in the configuration file.\n"
				+ "\t - \"terms\" : terms to track.\n"
				+ "\t - \"users\" : users to follow.\n"
				+ "\t - \"geo\" : bounding boxes defining geographic areas\n"
				+ "\t - \"all\" : check for all of the previous parameters\n");
		tweetCrawlParser.addArgument("-s", "--store")		
		.choices("stout", "db", "solr")
		.setDefault("stout")
		.help("Whether tweets shall be stored in a database, an Apache solr Index or printed to stdout (default).\n"
				+ "\t - \"stout\" : standard output\n"
				+ "\t - \"db\" : database\n"
				+ "\t - \"solr\" : solr (not implemented yet)\n");
		tweetCrawlParser.addArgument("-cn", "--census")				
		.help("Census is used to store tweets by users in a certain geographic area. A path to a file must be given as an argument. "
				+ "The file contains a list of Twitter users. The users and their tweets will be marked in"
				+ " the database as 'local'. The file must contain one user per line in the following format:\n"
				+ "\t userId<tab>screenName[<tab>Additional Fields]\n"
				+ "NOTE: if the value 'db' is give instead of a file path MSM will try to generate the census from the database.\n");
	}
	
	public final void loadFeedReaderParameters()
	{
		feedReaderParser.addArgument("-c", "--config")
		.required(false)
		.help("Configuration file that contains the necessary parameters to connect to the feed stream for crawling."
				+ "Only needed if no url is given through the --url parameter.\n");
		
		feedReaderParser.addArgument("-t", "--type")
		.choices("press", "multimedia")
		.setDefault("press")
		.help("The type of feed we are dealing with.\n"
				+ "\t - \"press\" : written digital press feeds. Standard rss feed parsers are used\n"
				+ "\t - \"multimedia\" : feed coming from multimedia source (tv/radio) a custom parser is activated in this case.\n");
		
		
		feedReaderParser.addArgument("-ff", "--ffmpeg")
		.required(false)
		.setDefault("/usr/bin")
		.help("The path to the ffmpeg executable (the directory containing ffmpeg and ffprobe binaries)."
				+ " Only needed if for multimedia source feeds."
				+ " Default value is \"/usr/bin\". Change the value to match your installation path\n");
		
		feedReaderParser.addArgument("-u", "--url")
		.required(false)
		.setDefault("db")
		.help("URL(s) of the feed(s) we want to crawl. Feeds must be separated with ',' chars.\n"
				+ "e.g. : java -jar MSM-1.0.jar feed -u 'url1,url2,...'");
		feedReaderParser.addArgument("-s", "--store")		
		.choices("stout", "db", "solr")
		.setDefault("stout")
		.help("Whether mentions shall be stored in a database, an Apache solr Index or printed to stdout (default).\n"
				+ "\t - \"stout\" : standard output\n"
				+ "\t - \"db\" : database\n"
				+ "\t - \"solr\" : solr (not implemented yet)\n");
		feedReaderParser.addArgument("-cn", "--census")				
		.help("Census is used to store mentions from sources in a certain geographic area. A path to a file must be given as an argument. "
				+ "The file contains a list of source ids (as stored in the database). The sources and their mentions will be marked in"
				+ " the database as 'local'. The file must contain one user per line in the following format:\n"
				+ "\t userId<tab>sourceName[<tab>Additional Fields]\n");
	}
	
	public final void loadInfluenceTaggerParameters()
	{
		influenceTaggerParser.addArgument("-s", "--sources")
		.required(false)
		.setDefault("db")
		.help("web domain (pageRank) or twitter screen name (KloutIndex) to look for its influence for."
				+ "Many sources may be introduced separated by ',' char."
				+ "If you want to retrieve sources from the database left this option empty or use the 'db' value\n");
		influenceTaggerParser.addArgument("-c", "--config")		
		.required(true)
		.help("Configuration file that contains the necessary parameters to connect to Influence APIs "
				+ "and Database you want to interact with the database\n");
		influenceTaggerParser.addArgument("-db", "--database")		
		.action(Arguments.storeTrue())
		.help("Whether influences shall be stored in a database or printed to stdout (default). "
				+ "Database parameters must be given in the configuration file.\n");
		influenceTaggerParser.addArgument("-t", "--type")
		.choices("twitter", "feed", "all")
		.setDefault("all")
		.help("type of the sources to look for its influence for:"
				+ "\t - \"twitter\" : sources are twitter user screen names\n"
				+ "\t - \"domain\" : sources are web domains\n"
				+ "\t - \"all\" : sources are mixed (default) system will detect the source type for each source\n");
		influenceTaggerParser.addArgument("-w", "--which")
		.choices("null", "error", "all")
		.setDefault("null")
		.help("which sources to look for its influence for (only for database interaction):\n"
				+ "\t - \"null\" : sources that have not been yet processed at all\n"
				+ "\t - \"error\" : sources that have been processed but no influence could be retrieved\n"
				+ "\t - \"all\" : all sources.\n\t\tWARNING: this will override values in the database.\n"
				+ "\t\tWARNING2:Depending on the number of sources in the database this could take a very long time.\n");
		influenceTaggerParser.addArgument("-l", "--limit")		
		.type(Integer.class)
		.setDefault(500)		
		.help("limit the number of sources processed in the execution (only for database interaction): default is 500\n"
				+ "--limit = 0 means no limit is established, and thus the command will atempt to process all sources found in the db (not processed yet).\n"
				+ "This parameter is important not to exceed the twitter api rate limits. Increase it at your own risk.\n");
	}
	
	
	
	public final void loadTwitterUserInfoParameters()
	{
		twitterUserParser.addArgument("-c", "--config")
		.required(true)
		.help("Configuration file that contains the necessary parameters to connect to "
				+ "the twitter REST API 1.1.\n");
		/*tweetCrawlParser.addArgument("-p", "--params")		
		.choices("terms", "users", "geo", "all")
		.setDefault("all")
		.help("Search parameters to use when connecting to the Twitter Streaming API. Parameter values are"
				+ "either stored in the database or specified in the configuration file.\n"
				+ "\t - \"terms\" : terms to track.\n"
				+ "\t - \"users\" : users to follow.\n"
				+ "\t - \"geo\" : bounding boxes defining geographic areas\n"
				+ "\t - \"all\" : check for all of the previous parameters\n");*/
		twitterUserParser.addArgument("-s", "--store")		
		.choices("stout", "db", "solr")
		.setDefault("stout")
		.help("Whether tweets shall be stored in a database, an Apache solr Index or printed to stdout (default).\n"
				+ "\t - \"stout\" : standard output\n"
				+ "\t - \"db\" : database\n"
				+ "\t - \"solr\" : solr (not implemented yet)\n");
		twitterUserParser.addArgument("-l", "--limit")		
		.type(Integer.class)
		.setDefault(500)
		.help("limit the number of users processed in the execution (only for database interaction): default is 500\n"
				+ "--limit = 0 means no limit is established, and thus the command will atempt to process all sources found in the db (not processed yet).\n"
				+ "This parameter is important depending on the number of APIs you have available and your usage rate limits.\n");
	}
	
	public final void loadUserLocationGeocoderParameters()
	{
		userLocationGeocoderParser.addArgument("-s", "--sources")
		.required(false)
		.setDefault("db")
		.help("location to look for its geo-coordinate info."
				+ "Many locations may be introduced separated by '::' string (semicolon may be used inside the location string, that is why they are not used as separators)."
				+ "If you want to retrieve sources from the database left this option empty or use the 'db' value\n");
		userLocationGeocoderParser.addArgument("-c", "--config")		
		.required(true)
		.help("Configuration file that contains the necessary parameters to connect to the corresponding geocoding service APIs"
				+ "and Database you want to interact with the database\n");
		userLocationGeocoderParser.addArgument("-db", "--database")		
		.action(Arguments.storeTrue())
		.help("Whether influences shall be stored in a database or printed to stdout (default). "
				+ "Database parameters must be given in the configuration file.\n");
		userLocationGeocoderParser.addArgument("-t", "--type")
		.choices("twitter", "feed", "all")
		.setDefault("all")
		.help("type of the sources to look for its geolocation for:"
				+ "\t - \"twitter\" : sources are twitter user screen names\n"
				+ "\t - \"domain\" : sources are web domains\n"
				+ "\t - \"all\" : sources are mixed (default) system will detect the source type for each source\n");
		userLocationGeocoderParser.addArgument("-w", "--which")
		.choices("unknown", "error", "all")
		.setDefault("unknown")
		.help("which sources to look for its influence for (only for database interaction):\n"
				+ "\t - \"unknown\" : sources that have not been yet processed at all\n"
				+ "\t - \"error\" : sources that have been processed but no influence could be retrieved\n"
				+ "\t - \"all\" : all sources.\n\t\tWARNING: this will override values in the database.\n"
				+ "\t\tWARNING2:Depending on the number of sources in the database this could take a very long time.\n");
		userLocationGeocoderParser.addArgument("-a", "--api")
		.choices("mapquest","mapquest-open","openstreetmaps", "googlemaps", "LocationIQ", "OpenCage","all")
		.setDefault("all")
		.help("Geocoding is by default multi API, using all the APIs for which the user obtains keys (they must be specified in the config file).\n"
				+ "Through this parameter the user may specify a single API to use for geocoding.\n"
				+ "BEWARE to set --limit option according to your usage rate limits.\n");
		userLocationGeocoderParser.addArgument("-l", "--limit")		
		.type(Integer.class)
		.setDefault(1000)
		.help("limit the number of sources processed in the execution (only for database interaction): default is 1000\n"
				+ "--limit = 0 means no limit is established, and thus the command will atempt to process all sources found in the db (not processed yet).\n"
				+ "This parameter is important depending on the number of APIs you have available and your usage rate limits.\n");			
	}
	
	
	/**
	 * Dummy function to get the version of this software from the pom.properties file.
	 * @return
	 */
	final String getVersion(){
		String v = "null";
		Properties pom = new Properties();
		try {
			pom.load(this.getClass().getResourceAsStream(
		            "/META-INF/maven/elh.eus/MSM/pom.properties"
		          ));
			v = pom.getProperty("version");
		} catch (FileNotFoundException fe){
			System.err.println("MSM::CLI - pom.properties not found ");			
		} catch (IOException ioe){
			System.err.println("MSM::InfluenceTagger - pom.properties could not be read ");
		} 	
		
		return v;
	}
	
	
}
