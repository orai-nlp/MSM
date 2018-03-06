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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.apache.commons.validator.routines.UrlValidator;

import twitter4j.User;

public class Source {

	private long id;	
	private String screenName;
	private String type;
	private String domain;
	private double influence;
	private int followers;
	private int friends;
	private String geoInfo;
	private String location;
	private boolean isLocalArea;

	
	private final Pattern httpProt = Pattern.compile("^[hf]t?tps?://", Pattern.CASE_INSENSITIVE);


	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getScreenName() {
		return screenName;
	}
	
	public void setScreenName(String screenName) {
		this.screenName = screenName;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	
	public double getInfluence() {
		return influence;
	}

	public void setInfluence(double influence) {
		this.influence = influence;
	}
	
	public int getFollowers() {
		return followers;
	}
	
	public void setFollowers(int id) {
		this.followers = id;
	}

	public int getFriends() {
		return friends;
	}
	
	public void setFriends(int id) {
		this.friends = id;
	}
	
	public String getGeoInfo() {
		return geoInfo;
	}
	
	public void setGeoInfo(String id) {
		this.geoInfo = id;
	}
	
	public String getLocation() {
		return location;
	}
	
	public void setLocation(String id) {
		this.location = id;
	}
	
	public boolean getIsLocalArea() {
		return isLocalArea;
	}

	public void setIsLocalArea(boolean ila) {
		this.isLocalArea = ila;
	}
	
	//END OF GETTERS AND SETTERS
	
	//CONSTRUCTORS
	
	public Source(String src){
		//URL normalization
		UrlValidator defaultValidator = new UrlValidator(UrlValidator.ALLOW_ALL_SCHEMES);
		
		//add protocol since defaultvalidator wont't work without it.
		String check = src;
		if (!httpProt.matcher(check).find())
		{
			check ="http://"+src;
		}
		//System.err.println("Source: new source: "+check);
		if (defaultValidator.isValid(check)) 
		{
			//System.err.println("Source: new web source: "+src);
			setDomain(check);
			setScreenName(src);			
			setType("feed");
		}
		else
		{
			//System.err.println("Source: new twitter source: "+src);			
			setScreenName(src);
			setType("twitter");
		}
		setIsLocalArea(false);
	}
	
	public Source(long id, String screenName, String type, String domain,double inf, boolean isLocal){
		setId(id);
		setScreenName(screenName);
		setType(type);
		setDomain(domain);
		setInfluence(inf);
		setFollowers(-1);
		setFriends(-1);
		setIsLocalArea(isLocal);
	}
	
	public Source(long id, String screenName, String type, String domain,double inf, int ff,int fr, String location, boolean isLocal){
		this(id, screenName,type,domain,inf,isLocal);
		setFollowers(ff);
		setFriends(fr);
		//geoInfo
		String geoStr = "unknown";
		if(location != null)
		{
			geoStr = location;
		}
		setLocation(geoStr);
		setGeoInfo(geoStr);
	}
	
	/**
	 *  Constructor from twitter4j.User 
	 * @param u
	 */
	public Source(User u, boolean isLocal)
	{
		this(u.getId(), u.getScreenName(),"Twitter","",-1,u.getFollowersCount(),u.getFriendsCount(),u.getLocation(), isLocal);
	}


	/**
	 * Retrieve sources from database. Normally in order to launch a crawler or search engine queries
	 * 
	 * @param conn
	 * @param type (twitter|press|behagune)
	 * @return
	 * @throws NamingException
	 * @throws SQLException
	 */
	public static Set<Source> retrieveFromDB(Connection conn, String type, String opt) throws NamingException, SQLException {

		Set<Source> result = new HashSet<Source>(); 
		Statement stmt = conn.createStatement();
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM behagunea_app_source");
		switch (opt)
		{
		case "null": sb.append(" where influence is NULL or influence='NULL' or influence=''");break;
		case "error":  sb.append(" where influence=-1");break;
		case "all":  break;
		}
		
		String andWhere = " where ";
		if (sb.toString().contains(" where "))
		{
			andWhere=" and ";
		}
		
		if (type.equalsIgnoreCase("twitter"))
		{			
			sb.append(andWhere).append("type='Twitter'");
		}
		else if (type.equalsIgnoreCase("feed"))
		{
			sb.append(andWhere).append("type='press'");
		}
		
		//limited to 500 sources per call not to exceed rate limit.
		String query = sb.append(" order by source_id desc limit 500").toString();
		System.err.println("elh-MSM::Keyword::retrieveFromDB - query:"+query);
		ResultSet rs = stmt.executeQuery(query);		
		
		try{	
			if (type.equalsIgnoreCase("feed"))
			{
				while (rs.next()) {
					Source src = new Source(rs.getLong("source_id"), rs.getString("source_name"),rs.getString("type"),rs.getString("domain"),rs.getDouble("influence"),rs.getBoolean("is_local_area"));				
					result.add(src);
				}
			}
			else
			{
				while (rs.next()) {
					Source src = new Source(rs.getLong("source_id"), rs.getString("source_name"),rs.getString("type"),rs.getString("domain"),rs.getDouble("influence"), rs.getInt("followers"), rs.getInt("friends"),rs.getString("location"),rs.getBoolean("is_local_area"));				
					result.add(src);
				}
			}
			stmt.close();
		} catch (SQLException sqle ) {
			sqle.printStackTrace();
		} //finally {
		//	if (stmt != null) { stmt.close(); }
		//}

		rs.close();		
		return result;
	}
	
	/**
	 * Retrieve sources from database for geocoding. 
	 * 
	 * @param conn
	 * @param type (twitter|press)
	 * @return
	 * @throws NamingException
	 * @throws SQLException
	 */
	public static Set<Source> retrieveForGeoCodingFromDB(Connection conn, String type, String opt) throws NamingException, SQLException {

		Set<Source> result = new HashSet<Source>(); 
		Statement stmt = conn.createStatement();
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM behagunea_app_source where location !='unknown' ");
		switch (opt)
		{
		case "unknown": sb.append(" and (geoinfo='' or geoinfo='unknown')");break;
		case "error":  sb.append(" and geoinfo='error'");break;
		case "all":  break;
		}
		
		String andWhere = " where ";
		if (sb.toString().contains(" where "))
		{
			andWhere=" and ";
		}
		
		if (type.equalsIgnoreCase("twitter"))
		{			
			sb.append(andWhere).append("type='Twitter'");
		}
		else if (type.equalsIgnoreCase("feed"))
		{
			sb.append(andWhere).append("type='press'");
		}
		
		//limited to 500 sources per call not to exceed rate limit.
		String query = sb.append(" order by source_id desc limit 50").toString();
		System.err.println("elh-MSM::Keyword::retrieveFromDB - query:"+query);
		ResultSet rs = stmt.executeQuery(query);		
		
		try{	
			if (type.equalsIgnoreCase("feed"))
			{
				while (rs.next()) {
					Source src = new Source(rs.getLong("source_id"), rs.getString("source_name"),rs.getString("type"),rs.getString("domain"),rs.getDouble("influence"),rs.getBoolean("is_local_area"));				
					result.add(src);
				}
			}
			else
			{
				while (rs.next()) {
					Source src = new Source(rs.getLong("source_id"), rs.getString("source_name"),rs.getString("type"),rs.getString("domain"),rs.getDouble("influence"), rs.getInt("followers"), rs.getInt("friends"),rs.getString("location"),rs.getBoolean("is_local_area"));				
					result.add(src);
				}
			}
			stmt.close();
		} catch (SQLException sqle ) {
			sqle.printStackTrace();
		} //finally {
		//	if (stmt != null) { stmt.close(); }
		//}

		rs.close();		
		return result;
	}
	
	
	
	
	/**
	 * Retrieve sources belonging to local area from database. Normally in order to use with a crawler
	 * 
	 * @param conn
	 * @param type (twitter|press|behagune)
	 * @return
	 * @throws NamingException
	 * @throws SQLException
	 */
	public static Set<Long> retrieveLocalAreaFromDB(Connection conn, String type) throws NamingException, SQLException {

		Set<Long> result = new HashSet<Long>(); 
		Statement stmt = conn.createStatement();
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT source_id FROM behagunea_app_source where is_local_area=1");
		
		String andWhere = " and ";
		
		switch (type)
		{
		case "twitter": sb.append(andWhere).append("type='Twitter'");break;
		case "feed": sb.append(andWhere).append("type='Press'");break;
		case "multimedia": sb.append(andWhere).append("type='multimedia'");break;
		}
				
		String query = sb.toString();
		System.err.println("elh-MSM::Keyword::retrieveFromDB - query:"+query);
		ResultSet rs = stmt.executeQuery(query);		
		
		try{	
			while (rs.next()) {
				result.add(rs.getLong("source_id"));
			}			
			stmt.close();
		} catch (SQLException sqle ) {
			sqle.printStackTrace();
		} //finally {
		//	if (stmt != null) { stmt.close(); }
		//}

		rs.close();		
		return result;
	}
	
	
	public boolean existsInDB (Connection conn)
	{
		int result = 0;
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT count(*) FROM behagunea_app_source where source_id="+getId());
			if(rs.next()){
	            result = rs.getInt(1);
	        }
			stmt.close();
		} catch (SQLException e){
			
		}
		return (result >0);
	}
	
	/**
	 * Store source into the database.
	 * 
	 * @param conn
	 * @return
	 */
	public int source2db(Connection conn) {	

		PreparedStatement stmtM = null;
		
		int success = 0;
		//PreparedStatement stmtS = null;		
		try {	
			String sourceIns = "insert ignore into behagunea_app_source (source_id, type, source_name, user_id, followers,friends,location,geoinfo,is_local_area) values (?,?,?,?,?,?,?,?,?)";
			stmtM = conn.prepareStatement(sourceIns, Statement.RETURN_GENERATED_KEYS);
			stmtM.setLong(1, getId());
			stmtM.setString(2, "Twitter");
			stmtM.setString(3, getScreenName());
			stmtM.setInt(4, 1); //BEWARE: user_id is always given '1'. This must be reviewed in the future.	        
			stmtM.setInt(5, getFollowers());
			stmtM.setInt(6, getFriends());
			stmtM.setString(7, getLocation());
			stmtM.setString(8, getGeoInfo());
			stmtM.setBoolean(8, getIsLocalArea());
			stmtM.executeUpdate();
			stmtM.close();
			success=1;
		}catch (SQLException e){
			System.err.println("elh-MSM::Source source2db - Error when trying to store source into db.");
			e.printStackTrace();
		}
		return success;
	}
}
