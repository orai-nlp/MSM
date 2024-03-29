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

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.commons.validator.routines.UrlValidator;

public class Feed {
	
	private int id;	
	private long srcId;
	private String feedURL;
	private String lastFetchDate;
	private String langs;
	private double influence;
	private String usr;
	private String pass;
	private String srcDomain;
	private FeedCredential loginCredentials;


	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public long getSrcId() {
		return srcId;
	}
	
	public void setSrcId(long srcId) {
		this.srcId = srcId;
	}

	
	public String getFeedURL() {
		return feedURL;
	}
	
	public void setFeedURL(String fURL) {
		this.feedURL = fURL;
	}
	
	
	public String getLastFetchDate() {
		return lastFetchDate;
	}
	
	public void setLastFetchDate(String lfDate) {
		this.lastFetchDate = lfDate;
	}
	
	public String getLangs() {
		return langs;
	}
	
	public void setLangs(String l) {
		this.langs = l;
	}
	
	
	public double getInfluence() {
		return influence;
	}

	public void setInfluence(double influence) {
		this.influence = influence;
	}
	
	
	public String getUsr() {
		return usr;
	}

	public void setUsr(String usr) {
		this.usr = usr;
	}

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

	public String getSrcDomain() {
		return srcDomain;
	}

	public void setSrcDomain(String srcDomain) {
		this.srcDomain = srcDomain;
	}


	public FeedCredential getLoginCredentials() {
		return loginCredentials;
	}

	public void setLoginCredentials(FeedCredential loginCredentials) {
		this.loginCredentials = loginCredentials;
	}
	
	/**
	 * @param src
	 */
	public Feed(String src){
		//URL normalization
		UrlValidator defaultValidator = new UrlValidator(UrlValidator.ALLOW_ALL_SCHEMES);
				
		if (defaultValidator.isValid(src)) 
		{
			setFeedURL(src);
			setLastFetchDate("1950-01-01 00:00:00 +0000");
			try {
				URI ttt = new URI(src);
				String domain = ttt.getHost();
				setSrcDomain(domain);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			System.err.println("MSM::Feed (constructor) - given feed url is not valid "+src);
			System.exit(1);
		}
	}
	
	/**
	 * @param id
	 * @param srcId
	 * @param furl
	 * @param lfDate
	 * @param type
	 * @param domain
	 * @param inf
	 */
	public Feed(int id, long srcId,String furl, String lfDate, String langs, double inf, String domain){
		setId(id);
		setSrcId(srcId);
		setFeedURL(furl);
		setLastFetchDate(lfDate);
		setLangs(langs);
		setInfluence(inf);
		setSrcDomain(domain);
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
	public static Set<Feed> retrieveFromDB(Connection conn, String type, String tableprefix) throws SQLException {

		Set<Feed> result = new HashSet<Feed>(); 
		Statement stmt = conn.createStatement();

		// our SQL SELECT query. 
		String query= "SELECT * FROM "+tableprefix+"_app_source AS s INNER JOIN"          // s.source_name, s.source_id, f.description, f.id, f.lang, uf.active, f.last_fetch "
				+ "("+tableprefix+"_app_feed AS f INNER JOIN "+tableprefix+"_app_user_feed AS uf ON f.id=uf.feed_id)"
				+ " ON  s.source_id=f.source_id WHERE s.type='"+type+"' AND uf.active=1";
		
		// execute the query, and get a java resultset
		ResultSet rs = stmt.executeQuery(query);

		// iterate through the java resultset
		while (rs.next())
		{
			long sid = rs.getLong("s.source_id");
			int fid = rs.getInt("f.id");
			String url = rs.getString("f.url");
			String lastFetch = rs.getString("f.last_fetch");
			String langs = rs.getString("f.lang");
			String domain = rs.getString("s.domain");
			
			Feed src = new Feed(fid,sid,url,lastFetch,langs, rs.getDouble("s.influence"),domain);
		
			if (rs.getString("f.login_url") != null)
			{
				FeedCredential cred = new FeedCredential(rs.getString("f.url"), rs.getString("f.login_url"), rs.getString("f.login_username"),
					rs.getString("f.login_passwd"), rs.getString("f.login_usr_field"),rs.getString("f.login_passwd_field"),rs.getString("f.logged_check_field"),
					rs.getString("f.login_cookie_button"));
			
				src.setLoginCredentials(cred);
			}
			result.add(src);	
		}
		rs.close();			
		stmt.close();

		return result;
	}

	
}
