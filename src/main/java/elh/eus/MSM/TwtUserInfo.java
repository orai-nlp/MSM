package elh.eus.MSM;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;

public class TwtUserInfo {
	
	private Properties params = new Properties();	
	private String store = "";
		
	public String getStore() {
		return store;
	}

	public void setStore(String store) {
		this.store = store;
	}
	
	
	/**
	 * 
	 * Constructor and main function. Starts a connections and gives the messages to the corresponding handlers.
	 * 
	 * @param config
	 * @param store
	 */
	public TwtUserInfo(String config, String store)
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

		List<Long> usrIds = new ArrayList<Long>();	
		//Config file settings have preference over DB, if config file parameter is null then try the DB
		if (!params.getProperty("userIds","none").equalsIgnoreCase("none"))
		{
			String[] follows = params.getProperty("userIds").split(",");
			for (String s : follows)
			{
				//System.err.println("elh-MSM::TwtUserInfo - followings: "+s);
				usrIds.add(Long.parseLong(s));
			}	
			System.err.println("elh-MSM::TwtUserInfo - retrieved "+usrIds.size()+" users from config file");
		}
		// If no user Ids were defined in config file try to get them from the DB
		if (usrIds.isEmpty()){	
			try{
				Connection conn = Utils.DbConnection(
						params.getProperty("dbuser"), 
						params.getProperty("dbpass"), 
						params.getProperty("dbhost"), 
						params.getProperty("dbname")); 
				Set<Source> srcList = Source.retrieveFromDB(conn,"twitter","all");
				for (Source src : srcList)
				{
					usrIds.add(src.getId());
				}
				System.err.println("elh-MSM::TwtUserInfo - retrieved "+usrIds.size()+" users from DB");
			} catch (Exception e){
				System.err.println("elh-MSM::TwtUserInfo - connection with the DB could not be established,"
						+ "MSM will try to read user ids from config file.");
				//e.printStackTrace();			
			}
		}
		
		if (usrIds.isEmpty())
		{
			System.err.println("elh-MSM::TwtUserInfo - No user info could be found, aborting process");
			System.exit(1);
		}
		

		// Twitter OAUTH credentials these secrets should be read from a config file		
		String ckey = params.getProperty("consumerKey");
		String csecret = params.getProperty("consumerSecret");
		String token = params.getProperty("accessToken");
		String tsecret = params.getProperty("accesTokenSecret");

		TwitterFactory factory = new TwitterFactory();
		Twitter twitter = factory.getInstance();
		twitter.setOAuthConsumer(ckey, csecret);
		twitter.setOAuthAccessToken(new AccessToken(token, tsecret));

		for (long id : usrIds) 
		{
			try
			{
				// fetch friends and followers from twitter for the current user.			
				User u = twitter.showUser(id);
				int ff = u.getFollowersCount();
				int fr = u.getFriendsCount();
				
				long[] friends = twitter.getFriendsIDs(id, -1).getIDs();
				long[] followers = twitter.getFollowersIDs(id, -1).getIDs();

				System.out.println("--> "+id+"\t"+ff+"\t"+fr);

				//print format: id1 '\t' id2 --> id2 is follower of id1
				//print followers
				for (long i : followers)
				{
					System.out.println(id+"\t"+i);
				}

				//print friends
				for (long i : friends)
				{
					System.out.println(i+"\t"+id);
				}
			}catch (TwitterException twte){
				System.err.println("elh-MSM::TwtUserInfo - Error when retrieving user info from Twitter id: "+id);
				//System.out.println("--> "+id+"\t-1\t-1");
				twte.printStackTrace();					
			}		
			try
			{
				Thread.sleep(1000*61); //1000 milliseconds is one second.					
			} catch (InterruptedException inte) {
				System.err.println("elh-MSM::TwtUserInfo - Error when waiting for the next call to Twitter");			
				inte.printStackTrace();
			}
		}				
	}
}
