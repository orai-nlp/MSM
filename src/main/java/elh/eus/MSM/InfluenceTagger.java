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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;
//import java.util.regex.Pattern;

import twitter4j.JSONException;
import twitter4j.JSONObject;



public class InfluenceTagger {

	private Properties params = new Properties();

	private String KloutKey;
	/*private String PRKey;*/
	private String DomainStatsIOKey;

	//private final Pattern httpProt = Pattern.compile("^[hf]t?tps?://", Pattern.CASE_INSENSITIVE);

	
	public InfluenceTagger (String config, boolean db)
	{
		try {
			params.load(new FileInputStream(new File(config)));
		} catch (FileNotFoundException fe){
			System.err.println("MSM::InfluenceTagger - Config file not found "+config);
			System.exit(1);
		} catch (IOException ioe){
			System.err.println("MSM::InfluenceTagger - Config file could not read "+config);
			System.exit(1);
		} 				
		
		KloutKey = params.getProperty("KloutKey", "none");
		if (KloutKey.equalsIgnoreCase("none"))
		{
			System.err.println("MSM::InfluenceTagger WARNING - no Klout Key could be found, Klout index won't be retrieved");
		}

		
		/*PRKey = params.getProperty("PRKey", "none");
		if (PRKey.equalsIgnoreCase("none"))
		{
			System.err.println("MSM::InfluenceTagger WARNING - no PageRank Key could be found, PageRank index won't be retrieved");
		}*/		
			
		DomainStatsIOKey = params.getProperty("DomainStatsIOKey", "none");
		if (DomainStatsIOKey.equalsIgnoreCase("none"))
		{
			System.err.println("MSM::InfluenceTagger WARNING - no DomainStatsIOKey Key could be found, PageRank index won't be retrieved");
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
			JSONObject json=MSMUtils.readJsonFromUrl(new URL("http://api.klout.com/v2/identity.json/twitter?screenName="+userId+"&key="+KloutKey));

			// if the json object contains the user id ask for its score (Klout has the user tracked).
			if (json.has("id"))
			{
				String kloutId = json.getString("id");
				json=MSMUtils.readJsonFromUrl(new URL("http://api.klout.com/v2/user.json/"+kloutId+"/score?key="+KloutKey));
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
		catch (IOException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException | URISyntaxException httpe) {
			System.err.println("MSM::InfluenceTagger - Reading error when trying to read the JSON object of the Klout index for source "+userId);
			//ioe.printStackTrace();
		}
		
         //if there is no id or it has no score return 0;
         return 0;

	}
		
	
	/** websites - DomainStats.com domain_call API : http://api.domainstats.com/
     * Key Rate Limits : - 5 Calls per minute | 50 Calls per week
	 * 
     * 
     */
	private double webDomainIndex (String id)
	{
		String result = "";

		// by defaults sources come with the protocol (ftp|http|https) attached, 
		//domainstats.com requires that we remove the protocol.
		URL myURL;
		String host=id;
		
		try {
			myURL = new URL(id);
			host = myURL.getHost();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		String url = "https://api.domainstats.com/"+host+"?secret_token="+DomainStatsIOKey;

		System.out.println("Sending request to : " + url);

		try {
			// get domainStats io response         
			JSONObject json=MSMUtils.readJsonFromUrl(new URL(url));
			System.out.println("Object read from: " + url);

			// if the json object contains the user id ask for its score (Klout has the user tracked).
			if (json.has("data"))
			{
				//Domain info is stored in the "data" object 
				JSONObject datajson = json.getJSONObject("data");

				System.out.println("found 'data' in json: " + url);

				
				result = datajson.getString("ahrefs_domain_rank");
				System.out.println(result);
			}

		} catch (JSONException je) {
			je.printStackTrace();
			System.out.println("MSM::InfluenceTagger - JSON error when trying to get influence (AHrefs)"
					+ "for domain "+id);			
		} catch (IOException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException | URISyntaxException httpe) {			
			System.out.println("MSM::InfluenceTagger - IO error when trying to get influence (AHrefs)"
					+ "for domain "+id);
			httpe.printStackTrace();
		}

		if ("".equals(result)) {
			if (id.endsWith(".eus"))
			{
				return webDomainIndex(id.replaceAll("\\.eus$", ".info"));
			}
			else
			{
				return 0;
			}
		} else {
			return Double.valueOf(result);
		}
	}
	
	
	
	/**
	 * Tag the influence of the sources in the given list. 
	 * Function returns the same list with the influence property filled. 
	 * 
	 * 
	 * @param srcList
	 * @return
	 * @throws InterruptedException 
	 */
	public Set<Source> tagInfluence(Set<Source> srcList) throws InterruptedException{
		
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
				src.setInfluence(webDomainIndex(src.getDomain()));
				Thread.sleep(1000);
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
				System.err.println("MSM::InfluenceTagger::influence2db - 0 influence for source "+src.getScreenName()
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

