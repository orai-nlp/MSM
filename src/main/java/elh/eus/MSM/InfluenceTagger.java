/*
 * Copyright 2015 Elhuyar Fundazioa

This file is part of MSM.

    EliXa is free software: you can redistribute it and/or modify
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
		try {
			params.load(new FileInputStream(new File(config)));
		} catch (FileNotFoundException fe){
			System.err.println("elh-MSM::InfluenceTagger - Config file not found "+config);
			System.exit(1);
		} catch (IOException ioe){
			System.err.println("elh-MSM::InfluenceTagger - Config file could not read "+config);
			System.exit(1);
		} 
		
		KloutKey = params.getProperty("KloutKey", "none");
		if (KloutKey.equalsIgnoreCase("none"))
		{
			System.err.println("MSM::InfluenceTagger WARNING - no Klout Key could be found, Klout index won't be retrieved");
		}
		
		PRKey = params.getProperty("PRKey", "none");
		if (PRKey.equalsIgnoreCase("none"))
		{
			System.err.println("MSM::InfluenceTagger WARNING - no PageRank Key could be found, PageRank index won't be retrieved");
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
		return 0;
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

