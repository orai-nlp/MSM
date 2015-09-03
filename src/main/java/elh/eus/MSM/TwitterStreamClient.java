package elh.eus.MSM;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesSampleEndpoint;
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

import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;


public class TwitterStreamClient {

	private Properties params = new Properties();	

	public TwitterStreamClient (String config, boolean db)
	{
		params.load(new FileInputStream(new File(config)));
	
		/** Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */
		BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);
		BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<Event>(1000);

		/** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
		Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
		StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
		// Optional: set up some followings and track terms
		//List<Long> followings = Lists.newArrayList(1234L, 566788L);
		List<String> terms = Lists.newArrayList("twitter", "api");
		//hosebirdEndpoint.followings(followings);
		hosebirdEndpoint.trackTerms(terms);

		try {
			// These secrets should be read from a config file		
			Authentication hosebirdAuth = new OAuth1(params.getProperty("consumerKey"),params.getProperty("consumerSecret"), params.getProperty("token"), params.getProperty("secret"));
		} catch (Exception e) {
			System.err.println("elh-Crawler::TwitterStreamClient - Authentication problem");
		}
		
		//After we have created a Client, we can connect and process messages:

		client.connect();
		while (!client.isDone()) {
		 String message = queue.take();
		 if (!db) // print the message to stdout
		 {
			 System.out.println(message); 
		 }
		 else   //message to database
		 {
			 
		 }
	}



	// A bare bones listener
	private StatusListener listener1 = new StatusListener() {
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
		public void onDisconnectMessage(DisconnectMessage message) {}

		@Override
		public void onStallWarningMessage(StallWarningMessage warning) {}

		@Override
		public void onUnknownMessageType(String s) {}
	};


	public void oauth(String consumerKey, String consumerSecret, String token, String secret) throws InterruptedException {
		// Create an appropriately sized blocking queue
		BlockingQueue<String> queue = new LinkedBlockingQueue<String>(10000);

		// Define our endpoint: By default, delimited=length is set (we need this for our processor)
		// and stall warnings are on.
		StatusesSampleEndpoint endpoint = new StatusesSampleEndpoint();

		Authentication auth = new OAuth1(consumerKey, consumerSecret, token, secret);
		// Authentication auth = new BasicAuth(username, password);

		// Create a new BasicClient. By default gzip is enabled.
		BasicClient client = new ClientBuilder()
		.hosts(Constants.STREAM_HOST)
		.endpoint(endpoint)
		.authentication(auth)
		.processor(new StringDelimitedProcessor(queue))
		.build();

		// Create an executor service which will spawn threads to do the actual work of parsing the incoming messages and
		// calling the listeners on each message
		int numProcessingThreads = 4;
		ExecutorService service = Executors.newFixedThreadPool(numProcessingThreads);

		// Wrap our BasicClient with the twitter4j client
		Twitter4jStatusClient t4jClient = new Twitter4jStatusClient(
				client, queue, Lists.newArrayList(listener1, listener2), service);

		// Establish a connection
		t4jClient.connect();
		for (int threads = 0; threads < numProcessingThreads; threads++) {
			// This must be called once per processing thread
			t4jClient.process();
		}

		Thread.sleep(5000);

		client.stop();
	}
}

