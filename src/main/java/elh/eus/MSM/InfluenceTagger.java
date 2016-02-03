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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Set;

import javax.naming.NamingException;

import twitter4j.JSONException;
import twitter4j.JSONObject;

import org.apache.commons.validator.routines.UrlValidator;



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
			JSONObject json=Utils.readJsonFromUrl("http://api.klout.com/v2/identity.json/twitter?screenName="+userId+"&key="+KloutKey);

			// if the json object contains the user id ask for its score (Klout has the user tracked).
			if (json.has("id"))
			{
				String kloutId = json.getString("id");
				json=Utils.readJsonFromUrl("http://api.klout.com/v2/user.json/"+kloutId+"/score?key="+KloutKey);
				if (json.has("score"))
				{
					System.err.println("MSM::InfluenceTagger - Success!! retrieved the Klout index for source "+userId);					
					return Double.valueOf(json.getString("score"));
				}        	
			}
		} 
		catch (JSONException je)
		{
			System.err.println("MSM::InfluenceTagger - JSON error when trying to get the Klout index for source "+userId);
			//je.printStackTrace();
		}
		catch (IOException ioe)
		{
			System.err.println("MSM::InfluenceTagger - Reading error when trying to read the JSON object of the Klout index for source "+userId);
			//ioe.printStackTrace();
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
		String result = "";

		JenkinsHash jenkinsHash = new JenkinsHash();
		long hash = jenkinsHash.hash(("info:" + id).getBytes());

		//Append a 6 in front of the hashing value.
		String url = "http://toolbarqueries.google.com/tbr?client=navclient-auto&hl=en&"
		   + "ch=6" + hash + "&ie=UTF-8&oe=UTF-8&features=Rank&q=info:" + id;

		System.out.println("Sending request to : " + url);

		try {
			URLConnection conn = new URL(url).openConnection();

			BufferedReader br = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));

			String input;
			while ((input = br.readLine()) != null) {

				// What Google returned? Example : Rank_1:1:9, PR = 9
				System.out.println(input);
				result = input.substring(input.lastIndexOf(":") + 1);
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		if ("".equals(result)) {
			if (id.endsWith(".eus"))
			{
				return PageRankIndex(id.replaceAll("\\.eus$", ".info"));
			}
			else
			{
				return 0;
			}
		} else {
			return Double.valueOf(result)*10;
		}
	}
	
	/**
	 * Tag the influence of the sources in the given list. 
	 * Function returns the same list with the influence property filled. 
	 * 
	 * 
	 * @param srcList
	 * @return
	 */
	public Set<Source> tagInfluence(Set<Source> srcList){
		
		//kk
		for (Source src : srcList)
		{
			//System.err.println("MSM::InfluenceTagger::tagInfluence - name: "+src.getScreenName()+" type: "+src.getType());
			if (src.getType().equalsIgnoreCase("twitter"))
			{
				src.setInfluence(KloutIndex(src.getScreenName()));
			}
			else
			{
				src.setInfluence(PageRankIndex(src.getDomain()));
			}
		}
		return srcList;
	}
	
	public int influence2db(Set<Source> srcList, Connection conn) throws SQLException 
	{
		PreparedStatement infUpdate = conn.prepareStatement("UPDATE behagunea_app_source SET influence=? where source_id=? and type=?");

		int count = 0;
		for (Source src : srcList)
		{
			if (src.getInfluence() == 0.0)
			{
				System.err.println("elh-MSM::InfluenceTagger::influence2db - 0 influence for source "+src.getScreenName()
						+" - -1 will be stored in the database");
				src.setInfluence(-1);
			}
			
			infUpdate.setDouble(1, src.getInfluence());
			infUpdate.setLong(2, src.getId());
			infUpdate.setString(3, src.getType());
				
			infUpdate.executeUpdate();
			count++;
		}
		infUpdate.close();
		return count;
	}
	
}

