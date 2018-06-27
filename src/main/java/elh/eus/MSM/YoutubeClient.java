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



import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.util.Joiner;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.GeoPoint;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.*;
import com.google.api.services.youtube.YouTube;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;



import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;


   





public class YoutubeClient {

	private Properties params = new Properties();	
	private String store = "";
	private List<String> acceptedLangs;
	private Set<Keyword> keywords;
	private String location = "";
	private String locationRadius = "";
	private List<Long> users = new ArrayList<Long>();
	private LangDetect LID;
	
	private Set<Keyword> independentkwrds = new HashSet<Keyword>();
	private Set<Keyword> dependentkwrds = new HashSet<Keyword>();

	private static Pattern anchorPattern; //pattern for anchor kwrds. they are usually general terms.
	private HashMap<Integer,Pattern> kwrdPatterns = new HashMap<Integer,Pattern>(); //patterns for keywords.
	
	
	
	private static final long NUMBER_OF_VIDEOS_RETURNED = 25;

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private static YouTube youtube;
	
	//private static Pattern retweetPattern = Pattern.compile("^RT[^\\p{L}\\p{M}\\p{Nd}]+.*");
	
	public String getStore() {
		return store;
	}

	public void setStore(String store) {
		this.store = store;
	}

	 
	
	/**
	 * Shared class used by every sample. Contains methods for authorizing a user and caching credentials.
	 */
	private static class Auth {

	    /**
	     * Define a global instance of the HTTP transport.
	     */
	    private final static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	    /**
	     * Define a global instance of the JSON factory.
	     */
	    private final static JsonFactory JSON_FACTORY = new JacksonFactory();

	    /**
	     * This is the directory that will be used under the user's home directory where OAuth tokens will be stored.
	     */
	    private static final String CREDENTIALS_DIRECTORY = ".oauth-credentials";

		private static final Collection<String> SCOPES = null;

		private static final DataStoreFactory DATA_STORE_FACTORY = null;

		private static final File DATA_STORE_DIR = null;

	    
		
		/**
	     * Creates an authorized Credential object.
	     * @return an authorized Credential object.
	     * @throws IOException
	     */
	    public static Credential authorize() throws IOException {
	        // Load client secrets.
	        InputStream in = YoutubeClient.class.getResourceAsStream("/client_secret.json");
	        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader( in ));

	        // Build flow and trigger user authorization request.
	        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
	        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
	            .setDataStoreFactory(DATA_STORE_FACTORY)
	            .setAccessType("offline")
	            .build();
	        Credential credential = new AuthorizationCodeInstalledApp(
	        flow, new LocalServerReceiver()).authorize("user");
	        System.out.println(
	            "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
	        return credential;
	    }
	    
	    /**
	     * Authorizes the installed application to access user's protected data.
	     *
	     * @param scopes              list of scopes needed to run youtube upload.
	     * @param credentialDatastore name of the credential datastore to cache OAuth tokens
	     */
	    public Credential authorize(List<String> scopes, String credentialDatastore) throws IOException {

	        // Load client secrets.
	        Reader clientSecretReader = new InputStreamReader(Auth.class.getResourceAsStream("/client_secrets.json"));
	        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, clientSecretReader);

	        // Checks that the defaults have been replaced (Default = "Enter X here").
	        if (clientSecrets.getDetails().getClientId().startsWith("Enter")
	                || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
	            System.out.println(
	                    "Enter Client ID and Secret from https://console.developers.google.com/project/_/apiui/credential "
	                            + "into src/main/resources/client_secrets.json");
	            System.exit(1);
	        }

	        // This creates the credentials datastore at ~/.oauth-credentials/${credentialDatastore}
	        FileDataStoreFactory fileDataStoreFactory = new FileDataStoreFactory(new File(System.getProperty("user.home") + "/" + CREDENTIALS_DIRECTORY));
	        DataStore<StoredCredential> datastore = fileDataStoreFactory.getDataStore(credentialDatastore);

	        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
	                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, scopes).setCredentialDataStore(datastore)
	                .build();

	        // Build the local server and bind it to port 8080
	        LocalServerReceiver localReceiver = new LocalServerReceiver.Builder().setPort(8080).build();

	        // Authorize.
	        return new AuthorizationCodeInstalledApp(flow, localReceiver).authorize("user");
	    }
	}

	


	
	
        public void onResult(Video videoResult) 
        {
        	//if log is active store status into log file.
        	String logFilePath = params.getProperty("twitter_log", "no");
        	if (! logFilePath.equalsIgnoreCase("no"))
        	{
        		try {
					FileUtils.writeStringToFile(new File(logFilePath), videoResult.toPrettyString());
				} catch (IOException e) {
					System.err.println("MSM::YoutubeSearchClient -IO error when writing to twitter client log");
					e.printStackTrace();
				}
        	}
        	switch (getStore()) // print the message to stdout
			{
			case "stout": prettyPrintVideo(videoResult); break;
			case "db":
				System.out.println("db - @" + videoResult.getEtag() + " - " + videoResult.getContentDetails());
				storeMention(videoResult);
				break;
			case "solr": System.out.println("solr - @" + videoResult.getEtag() + " - " + videoResult.getContentDetails()); break;
			} 
        }

        private void storeMention(Video video) {        	
			String text = video.getSnippet().getTitle();
			String lang = video.getSnippet().getDefaultLanguage();			
			//we DO blindly trust youtube language identification for the moment
			//lang = LID.detectTwtLanguage(text, lang);
			
			//language must be accepted and tweet must not be a retweet
			if ((acceptedLangs.contains("all") || acceptedLangs.contains(lang) || lang.equalsIgnoreCase("unk")))// && (! retweetPattern.matcher(text).matches()))
			{				
				Set<Keyword> kwrds = parseTweetForKeywords(text,lang);
				//if no keyword is found in the tweet it is discarded. 
				// This discards some valid tweets, as the keyword maybe in an attached link. 
				if (kwrds != null && !kwrds.isEmpty())
				{
					Mention m = new Mention (video, lang, false); //local area treatment not done
					m.setKeywords(kwrds);
					int success =1;
					switch (getStore()) // print the message to stdout
					{				
					case "db":
						try {
							Connection conn = MSMUtils.DbConnection(
									params.getProperty("dbuser"),
									params.getProperty("dbpass"),
									params.getProperty("dbhost"),
									params.getProperty("dbname"));
							Source author = new Source(video.getSnippet().getChannelId().length(), video.getSnippet().getChannelTitle(), "youtube","",-1, false);
							int authorStored = 0;
							if (!author.existsInDB(conn))
							{
								authorStored = author.source2db(conn);
							}
							success = m.mention2db(conn);
							
							System.err.println("MSM::YoutubeSearchClient - mention stored into the DB!"+success+" "+authorStored);
														
							conn.close();
							break;
						} catch (SQLException sqle) {
							System.err.println("MSM::YoutubeSearchClient - connection with the DB could not be established");
							sqle.printStackTrace();
						} catch (Exception e) {
							System.err.println("MSM::YoutubeSearchClient - error when storing mention");
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
				else if (!location.equalsIgnoreCase(""))
				{
					Mention m = new Mention (video, lang, false); //local area treatment not done
					int success =1;
					switch (getStore()) // print the message to stdout
					{				
					case "db":
						try {
							Connection conn = MSMUtils.DbConnection(
									params.getProperty("dbuser"),
									params.getProperty("dbpass"),
									params.getProperty("dbhost"),
									params.getProperty("dbname"));
							Source author = new Source(video.getSnippet().getChannelId());
							int authorStored = 0;
							if (!author.existsInDB(conn))
							{
								authorStored = author.source2db(conn);
							}
							success = m.mention2db(conn);
							System.err.println("MSM::YoutubeSearchClient -  mention stored into the DB!"+success+" "+authorStored);
														
							conn.close();
							break;
						} catch (SQLException sqle) {
							System.err.println("MSM::YoutubeSearchClient - connection with the DB could not be established");
							sqle.printStackTrace();
						} catch (Exception e) {
							System.err.println("MSM::YoutubeSearchClient - error when storing mention");
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
				System.err.println("MSM::YoutubeSearchClient - mention discarded because of lang requirements. lang: "+lang+" - "+acceptedLangs.toString());				
			}
			
		}


    


	/**
	 * 
	 * Constructor and main function. Starts a connections and gives the messages to the corresponding handlers.
	 * 
	 * @param config
	 * @param store
	 * @throws IOException 
	 */
	public YoutubeClient (String config, String store, String parameters) throws IOException
	{
		try {
			params.load(new FileInputStream(new File(config)));
		} catch (FileNotFoundException fe){
			System.err.println("MSM::YoutubeSearchClient - Config file not found "+config);
			System.exit(1);
		} catch (IOException ioe){
			System.err.println("MSM::YoutubeSearchClient - Config file could not read "+config);
			System.exit(1);
		} 
		
		//where should the retrieved messages be stored [db|solr|stout]
		setStore(store);
		//Language identification
		loadAcceptedLangs(params.getProperty("langs", "all"));
		LID = new LangDetect();
		
		// Define the API request for retrieving search results.
        YouTube.Search.List search = youtube.search().list("id,snippet");

        // Define the API term for retrieving search results.
		String queryTerm = "";
				
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
				System.err.println("MSM::YoutubeSearchClient - retrieved "+keywords.size()+" keywords from config file");											
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
					System.err.println("MSM::YoutubeSearchClient - retrieved "+keywords.size()+" keywords from DB");				

				} catch (Exception e){
					System.err.println("MSM::YoutubeSearchClient - connection with the DB could not be established,"
							+ "MSM will try to read search terms from config file.");
					//e.printStackTrace();			
				}
			}//terms from db

			System.err.println("MSM::YoutubeSearchClient - Search terms: "+terms.toString());

			//tracking terms
			if (!terms.isEmpty())
			{
				constructKeywordsPatterns();
				queryTerm=terms.toString();
	            search.setQ(queryTerm);
			}
			else if (parameters.equals("terms"))
			{
				System.err.println("MSM::YoutubeSearchClient - WARNING: no search terms could be found."
						+ "this can result in an Flood of tweets...");
			}

		} //tracking terms handling end
		
		String location = "";
		String locRadius = "";
		//location parameters 		
		if (parameters.equals("all") || parameters.equals("geo"))
		{
			if (!params.getProperty("location", "none").equalsIgnoreCase("none"))
			{			
				List<String> locs = Arrays.asList(params.getProperty("location").split("::"));
				for (String s : locs)
				{
					System.err.println("MSM::YoutubeSearchClient - location: "+s);
					String[] coords = s.split(",");
					// TODO location handling not implemented					
				}
			}
			System.err.println("MSM::YoutubeSearchClient - retrieved location and radius from config file");
			if (!location.equalsIgnoreCase(""))
			{							
				search.setLocation(location);
				search.setLocationRadius(locRadius);
			}
			else if (parameters.equals("geo"))
			{
				System.err.println("MSM::YoutubeSearchClient - WARNING: no geolocations could be found."
						+ "this can result in an Flood of tweets...");
			}
			
		}
	

		//END OF FILTER STREAM API PARAMETER HANDLING
		
		try {
            // This object is used to make YouTube Data API requests. The last
            // argument is required, but since we don't need anything
            // initialized when the HttpRequest is initialized, we override
            // the interface and provide a no-op function.
            youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest request) throws IOException {
                }
            }).setApplicationName("youtube-cmdline-geolocationsearch-sample").build();
                   
            // Set your developer key from the {{ Google Cloud Console }} for
            // non-authenticated requests. See:
            // {{ https://cloud.google.com/console }}
            String apiKey = params.getProperty("youtube.apikey");
            search.setKey(apiKey);
            

            // Restrict the search results to only include videos. See:
            // https://developers.google.com/youtube/v3/docs/search/list#type
            search.setType("video");

            // As a best practice, only retrieve the fields that the
            // application uses.
            search.setFields("items(id/videoId)");
            search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

            // Call the API and print results.
            SearchListResponse searchResponse = search.execute();
            List<SearchResult> searchResultList = searchResponse.getItems();
            List<String> videoIds = new ArrayList<String>();

            if (searchResultList != null) {

                // Merge video IDs
                for (SearchResult searchResult : searchResultList) {
                    videoIds.add(searchResult.getId().getVideoId());
                }
                Joiner stringJoiner = Joiner.on(',');
                String videoId = stringJoiner.join(videoIds);

                // Call the YouTube Data API's youtube.videos.list method to
                // retrieve the resources that represent the specified videos.
                YouTube.Videos.List listVideosRequest = youtube.videos().list("snippet, recordingDetails").setId(videoId);
                VideoListResponse listResponse = listVideosRequest.execute();

                List<Video> videoList = listResponse.getItems();

                if (videoList != null) {
                    prettyPrint(videoList.iterator(), queryTerm);
                }
            }
        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
	}

	private void loadAcceptedLangs(String property) {
		this.acceptedLangs=Arrays.asList(property.split(","));	
		System.err.println("MSM::YoutubeSearchClient - Accepted languages: "+acceptedLangs);
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
			System.err.println ("elh-MSM::YoutubeSearchClient - No keywords loaded");
			return;
			//System.exit(1);
		}

		StringBuilder sb_anchors = new StringBuilder();
		sb_anchors.append("\\b(");
		for (Keyword k : keywords)
		{
			//create and store pattern;
			String pstr;
			if (k.getText().startsWith("#"))
			{
				pstr = k.getText().replace('_',' ').toLowerCase();
			}
			else
			{
				pstr = "\\b"+k.getText().replace('_',' ').toLowerCase();
			}
			Pattern p = Pattern.compile(pstr);
			System.err.println("MSM::YoutubeSearchClient::constructKeywordPatterns - currentPattern:"+p.toString());

			kwrdPatterns.put(k.getId(), p);
			if (k.isAnchor())
			{
				sb_anchors.append(k.getText().replace('_',' ').toLowerCase()).append("|");
				anchors=true;
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
				
		String searchText = StringUtils.stripAccents(text).toLowerCase().replace('\n', ' ');
		boolean anchorFound = false;
		if (anchorPattern == null)
		{
			anchorFound=false;
		}
		else
		{
			anchorFound = anchorPattern.matcher(searchText).find();
		}
	
		System.err.println("MSM::YoutubeSearchClientReader::parseTweetForKeywords - independent:"+independentkwrds.size()
				+" - dependent: "+dependentkwrds.size()+"\n - searchText:"+searchText);
				
		
		//keywords that do not need any anchor
		for (Keyword k : independentkwrds)
		{				
			//System.err.println("MSM::YoutubeSearchClient::parseTweetForKeywords - independent key:"
			//	+k.getText()+" l="+k.getLang()+" pattern:"+kwrdPatterns.get(k.getId()).toString());
			String kLang = k.getLang();
			if ((kwrdPatterns.get(k.getId()).matcher(searchText).find()) && 
					(kLang.equalsIgnoreCase(lang)|| kLang.equalsIgnoreCase("all") || lang.equalsIgnoreCase("unk")))
			//if((k.getLang().equalsIgnoreCase(lang) && kwrdPatterns.get(k.getId()).matcher(searchText).find())|| 
			//		(k.getLang().equalsIgnoreCase("all") && kwrdPatterns.get(k.getId()).matcher(searchText).find()))
			{	
				System.err.println("MSM::YoutubeSearchClient::parseTweetForKeywords - independent key found!!!: "+k.getText()+" id: "+k.getId());
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
					System.err.println("MSM::YoutubeSearchClient::parseTweetForKeywords - dependent key found!!!: "+k.getText()+" id: "+k.getId());						
					result.add(k);
				}					
			}
		}	
		
		System.err.println("MSM::YoutubeSearchClient::parseTweetForKeywords - keywords found: "+result.size());						
		
		return result;
	}
	    
	 /**
     * Prints out all results in the Iterator. For each result, print the
     * title, video ID, location, and thumbnail.
     *
     * @param iteratorVideoResults Iterator of Videos to print
     *
     * @param query Search query (String)
     */
    private static void prettyPrint(Iterator<Video> iteratorVideoResults, String query) {

        System.out.println("\n=============================================================");
        System.out.println(
                "   First " + NUMBER_OF_VIDEOS_RETURNED + " videos for search on \"" + query + "\".");
        System.out.println("=============================================================\n");

        if (!iteratorVideoResults.hasNext()) {
            System.out.println(" There aren't any results for your query.");
        }

        while (iteratorVideoResults.hasNext()) {

            Video singleVideo = iteratorVideoResults.next();

            prettyPrintVideo(singleVideo);            
        }
    }
    
    /**
     * Prints out a single video to the standard outputall results in the Iterator. For each result, print the
     * title, video ID, location, and thumbnail.
     *
     * @param iteratorVideoResults Iterator of Videos to print
     *
     * @param query Search query (String)
     */
    private static void prettyPrintVideo(Video singleVideo) {

        if (!singleVideo.isEmpty()) {

            Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails().getDefault();
            GeoPoint location = singleVideo.getRecordingDetails().getLocation();

            System.out.println(" Video Id" + singleVideo.getId());
            System.out.println(" Title: " + singleVideo.getSnippet().getTitle());
            System.out.println(" Location: " + location.getLatitude() + ", " + location.getLongitude());
            System.out.println(" Thumbnail: " + thumbnail.getUrl());
            System.out.println("\n-------------------------------------------------------------\n");
        }
    }
}

