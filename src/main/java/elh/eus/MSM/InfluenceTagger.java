/*
 * Copyright 2014 Iñaki San Vicente

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package elh.eus.MSM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Properties;

import twitter4j.JSONException;
import twitter4j.JSONObject;



public class InfluenceTagger {

	private Properties params = new Properties();

	private String KloutKey;
	private String PRKey;

	public InfluenceTagger (String config, boolean db)
	{
		params.load(new FileInputStream(new File(config)));
		KloutKey = params.getProperty("KloutKey", "none");
		if (KloutKey.equalsIgnoreCase("none"))
		{
			System.err.println("MSM::InflucenceTagger WARNING - no Klout Key could be found, Klout index won't be retrieved");
		}
		
		PRKey = params.getProperty("PRKey", "none");
		if (PRKey.equalsIgnoreCase("none"))
		{
			System.err.println("MSM::InflucenceTagger WARNING - no PageRank Key could be found, PageRank index won't be retrieved");
		}
	}
	
	
	/** twitter
     * Key Rate Limits : - 10 Calls per second | 20,000 Calls per day
     * 
     */
	private double KloutIndex (String userId) 
	{
		try	{	
			// get klout id          
			JSONObject json=readJsonFromUrl("http://api.klout.com/v2/identity.json/twitter?screenName="+userId+"&key="+KloutKey);

			// if the json object contains the user id ask for its score (Klout has the user tracked).
			if (json.has("id"))
			{
				String kloutId = json.getString("id");
				json=readJsonFromUrl("http://api.klout.com/v2/user.json/"+kloutId+"/score?key="+KloutKey);
				if (json.has("score"))
				{
					return (double)json.getLong("score");
				}        	
			}
		} 
		catch (JSONException je)
		{
			System.err.println("MSM::InflucenceTagger - JSON error when trying to get the Klout index");
		}
		catch (IOException ioe)
		{
			System.err.println("MSM::InflucenceTagger - Reading error when trying to read the JSON object of the Klout index");
		}
		
         //if there is no id or it has no score return 0;
         return 0;

	}
	
	
	
	/** websites - PageRank
     * Key Rate Limits : - 10 Calls per second | 20,000 Calls per day
     * 
     */
	private double PageRankIndex (String id) 
	{
         // get klout id          
         JSONObject json=get("http://api.klout.com/v2/identity.json/twitter?screenName="+userId+"&key="+key);
         // if json is empty return influence 0.
         if (!json)
         {
             return 0;
         }      
         my $decoded_json = decode_json( $json );
         my $kloutid=$decoded_json->{'id'};
         // get klout score. Klout scroe range is [1..100]
         $json=get("http://api.klout.com/v2/user.json/"+kloutId"/score?key="+key);
         $decoded_json = decode_json( $json );
         double kloutscore=$decoded_json->{'score'};
         //print STDERR "$src - $srcType - $kloutscore\n";
         return kloutscore; 

	}
	
	
	/**
	 * Function to read a JSON object from an url (this function probably should not be here...)
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		BufferedReader reader = null;
		
		URL urlObj = new URL(url);
		BufferedReader rd = new BufferedReader(new InputStreamReader(urlObj.openStream()));
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) 
		{
			sb.append((char) cp);
		}
		JSONObject json = new JSONObject(sb.toString());	
	     
		return json;
	  }

	
}

