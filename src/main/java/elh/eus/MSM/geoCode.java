package elh.eus.MSM;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import twitter4j.JSONArray;
import twitter4j.JSONException;
import twitter4j.JSONObject;

/**
 * TODO : implement rate limit control based on each API returned information.
 * */
public class geoCode {

	
	private Properties params = new Properties();

	private HashMap<String,String> geoAPIs = new HashMap<String, String>();	
	
	private HashMap<String,Integer> geoAPIlimits = new HashMap<String, Integer>();	
	
	/**
	 * Initialization of available geocoding APIs. Each API has an request url and a rate limit 
	 * associated to their free plans. If higher pricing plans are obtained this values should be changed.
	 * 
	 * 	Key Rate Limits : 
	 *     - OpenStreetMap: 10 Calls per second | 20,000 Calls per day, no key needed
	 *     - LocationIQ:  1 call per second | 10.000 Calls per day, on the free account, non commercial.
	 *     - Mapquest: 15,000 requests per month on the free plan
	 *     - OpenCage: 2,500 calls per day on the free account.
	 *     - GoogleMaps: unlimited for testing?
	 *
	 */
	private void initializeGeoAPIs(){
		geoAPIs.put("openstreetmaps","http://nominatim.openstreetmap.org/search?format=json&limit=1&q=");
		geoAPIlimits.put("openstreetmaps",20000);
		//mapquest open api (based on openstreetmaps)
		geoAPIs.put("mapquest-open", "http://open.mapquestapi.com/nominatim/v1/search.php?key=##KEY##&format=json&limit=1&q=");
		geoAPIlimits.put("mapquest-open", 500);
		// mapquest geocoding api
		geoAPIs.put("mapquest", "http://mapquestapi.com/geocoding/v1/address?key=##KEY##&format=json&maxResults=1&location=");
		geoAPIlimits.put("mapquest", 500);
		geoAPIs.put("googlemaps","https://maps.googleapis.com/maps/api/geocode/json?key=##KEY##&address=");
		geoAPIlimits.put("googlemaps",20000);
		geoAPIs.put("LocationIQ","https://eu1.locationiq.org/v1/search.php?key=##KEY##&format=json&limit=1&q=");
		geoAPIlimits.put("LocationIQ",10000);
		geoAPIs.put("OpenCage", "https://api.opencagedata.com/geocode/v1/json?key=##KEY##&q=");
		geoAPIlimits.put("OpenCage", 2500);
		
	}
	
	//private String geoAPI;
	
	//private final Pattern httpProt = Pattern.compile("^[hf]t?tps?://", Pattern.CASE_INSENSITIVE);

	public geoCode (String config, boolean db)
	{
		try {
			params.load(new FileInputStream(new File(config)));
		} catch (FileNotFoundException fe){
			System.err.println("MSM::geoCode - Config file not found "+config);
			System.exit(1);
		} catch (IOException ioe){
			System.err.println("MSM::geoCode - Config file could not read "+config);
			System.exit(1);
		} 				
		
		initializeGeoAPIs();
		loadGeoAPIs(params);
		//if (APIKey.equalsIgnoreCase("none"))
		//{
		//	System.err.println("MSM::geoCode WARNING - no API Key could be found, geocode won't be retrieved, if you have no code default to OpenStreetMap API");
		//}		

	}
	

	private void loadGeoAPIs(Properties params2) {
		List<String> keys2rm = new ArrayList<String>(geoAPIs.keySet());
		for (String gapi: keys2rm)
		{						
			String key= params.getProperty("geoAPI."+gapi,"none");
			if (key.equalsIgnoreCase("none") && !gapi.equalsIgnoreCase("openstreetmaps")){
				geoAPIs.remove(gapi);
			}
			else
			{
				String urlnokey =geoAPIs.get(gapi);
				String urlkey = urlnokey.replace("##KEY##", key);
				geoAPIs.put(gapi, urlkey);
			}
			
			//"http://open.mapquestapi.com/nominatim/v1/search.php?key=KEY&format=json&limit=1&q="
		}
		System.err.println("MSM::geoCode - provided keys for these APIs "+geoAPIs.keySet().toString());		
	}

	public void selectAPI(String API) {
		HashMap<String, String> api = new HashMap<String, String> ();
		api.put(API, geoAPIs.get(API));
		geoAPIs = api;
	}
	/** 
	 * Geo Code retrieving functions
     * Key Rate Limits : 
     *     - OpenStreetMap: 10 Calls per second | 20,000 Calls per day, no key needed
     *     - LocationIQ:  1 call per second | 10.000 Calls per day, on the free account, non commercial.
     *     - Mapquest: 15,000 requests per month on the free plan
     *     			- Mapquest has two APIs, openstreetmap based and their own based on licensed data.
     *     			- We implement the open data API at the moment. In house evaluations showed that the
     *     			  licensed data API has lower performance, specially for search strings
     *     			  that have no real location behind. 
     *     - OpenCage: 2,500 calls per day on the free account. We do not use this API normally. 
     *                In house evaluation show a low performance for unreal location strings.
     *     - GoogleMaps: unlimited for testing?
     * 
     * WARNING: this function controls the remaining requests for each of the APIs provided, 
     *          however this is done on an execution call basis. 
     *          If you reach the day limit of an API but execute MSM again it won't be aware that the limit
     *          has been reached. This should be in the future done by controlling the limit with the responses returned by the APIs
     * 
     */	
	private String retrieveGeoCode (String query) 
	{
		if (query.length() == 0){
			return "error";
		}
		
		
		String qstr;
		try {
			qstr = URLEncoder.encode(query.replace("#", "").replace("\n", " ").replaceAll("\\s+", " "),"UTF-8");
		} catch (UnsupportedEncodingException e1) {
			System.err.println("MSM::geoCode - location couldn't be encoded unencoded. request will be sent");					
			qstr = query.replace(" ","+").replace("#", "");
		}
		
		int APInum = geoAPIs.size();
		
		Random random = new Random();
		List<String> keys = new ArrayList<String>(geoAPIs.keySet());
		String randomKey = keys.get( random.nextInt(APInum) );
		String APIaddress  = geoAPIs.get(randomKey);

		System.err.println("MSM::geoCode - API to use for the next query: "+randomKey);	

		// update the remaining requests in the API.
		Integer remaining = geoAPIlimits.get(randomKey);		
		geoAPIlimits.put(randomKey,remaining-1);
		if (remaining-1 < 1){
			geoAPIs.remove(randomKey);
		}		
				
		try	{
			URL APIurl = new URL(APIaddress+qstr);			
			System.err.println("MSM::geoCode - Request url: "+APIurl.toString());					
			
			JSONObject json=new JSONObject();
			switch (randomKey.toLowerCase())
			{
				case "opencage": case "googlemaps":			
					// get results from geocode api          
					json=MSMUtils.readJsonFromUrl(APIurl);
					//System.err.println("MSM::geoCode - json: "+json);	
					if (json.has("results") && json.getJSONArray("results").length()>0){
						//openCage stores the coordinates info in the geometry attribute.
						//OpenCage - open cage also returns how many request remain. For the moment this is not used.
						JSONObject geometry = json.getJSONArray("results").getJSONObject(0).getJSONObject("geometry");
				
						//googlemaps stores the coordinates info in the location attribute under geometry.
						if (randomKey.equalsIgnoreCase("googlemaps")){
							geometry = geometry.getJSONObject("location");
						}
				
						String geocode= "long="+geometry.getString("lng")+"_lat="+geometry.getString("lat");
						System.err.println("MSM::geoCode - Success!! retrieved the geoCode for location "+query+" - "+geocode);					
	            
						return geocode;
					}
					break;
				case "mapquest":
					// get results from geocode api          
					json=MSMUtils.readJsonFromUrl(APIurl);
					//mapquest stores the coordinates info in the geometry attribute.
					if (json.has("results") && json.getJSONArray("results").length()>0){
						JSONObject georesult = json.getJSONArray("results").getJSONObject(0).getJSONArray("locations").getJSONObject(0);
								
						// mapquest has latLng objects
						if (georesult.has("latLng")){
							JSONObject geoJSON = georesult.getJSONObject("latLng");
							String geocode= "long="+geoJSON.getString("lng")+"_lat="+geoJSON.getString("lat");
							System.err.println("MSM::geoCode - Success!! retrieved the geoCode for location "+query+" - "+geocode);									

							return geocode;				
						}
					}
					break;
				default:
					// get results from geocode api          
					JSONArray jsona=MSMUtils.readJsonArrayFromUrl(APIurl);				
					if (jsona.length()>0){
						json=jsona.getJSONObject(0);
					}
				
					// openstreetmaps, locationiq, mapquest-open have bounding box objects. From documentation:
					//Array of bounding box coordinates where this element is located. The order is as below:
					//	- min lat / bottom-left Latitude
					//	- max lat / top-right Latitude
					//	- min lon / bottom-left Longitude
					//	- max lon / top-right Longitude
					if (json.has("boundingbox"))
					{
						JSONArray geoJSON = json.getJSONArray("boundingbox");
						String geocode= "long="+geoJSON.getString(2)+"_lat="+geoJSON.getString(0)+
		            		";long="+geoJSON.getString(3)+"_lat="+geoJSON.getString(1);
						System.err.println("MSM::geoCode - Success!! retrieved the geoCode for location "+query+" - "+geocode);					

						return geocode;
					}
					break;
				
			}
			System.err.println("MSM::geoCode - Fail!! no geocode could be retrieved "+query+" - "+json.toString());					
							
		} 
		catch (JSONException je)
		{
			System.err.println("MSM::geoCode - JSON error when trying to get the coordinates for location "+APIaddress+qstr);
			je.printStackTrace();
		}
		catch (MalformedURLException mue)
		{
			System.err.println("MSM::geoCode - Malformed request url when trying to retrieve geocode for location "+APIaddress+qstr);
			mue.printStackTrace();
		}
		catch (IOException ioe)
		{
			System.err.println("MSM::geoCode - Reading error when trying to read the JSON object of the geocode for location "+APIaddress+qstr);
			ioe.printStackTrace();
		}
		
		
         //if there is no id or it has no geocode return 0;
         return "error";

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
	public Set<Source> tagGeoCode(Set<Source> srcList) throws InterruptedException{
		
		//kk
		for (Source src : srcList)
		{
			//System.err.println("MSM::InfluenceTagger::tagInfluence - name: "+src.getScreenName()+" type: "+src.getType());
			if (src.getType().equalsIgnoreCase("twitter"))
			{
				src.setGeoInfo(retrieveGeoCode(src.getLocation()));	
				System.err.println("MSM::geoCode - found geocode: "+src.getGeoInfo());
			}
			else
			{
				src.setGeoInfo(retrieveGeoCode(src.getLocation()));
				System.err.println("MSM::geoCode - found geocode: "+src.getGeoInfo());
			}
			//wait not to get banned;
			Thread.sleep(1010);
		}
		return srcList;
	}
	
	public int geocode2db(Set<Source> srcList, Connection conn) throws SQLException 
	{
		PreparedStatement infUpdate = conn.prepareStatement("UPDATE behagunea_app_source SET geoinfo=? where source_id=? and type=?");

		int count = 0;
		for (Source src : srcList)
		{	
			infUpdate.setString(1, src.getGeoInfo());
			infUpdate.setLong(2, src.getId());
			infUpdate.setString(3, src.getType());
				
			infUpdate.executeUpdate();
			count++;
		}
		infUpdate.close();
		return count;
	}
}