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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Date;
import java.util.Set;

import twitter4j.JSONObject;
import twitter4j.Status;

import java.sql.* ;  // for standard JDBC programs
import java.math.* ; // for BigDecimal and BigInteger support

public class Mention {

	private int mention_id;
	private String source_id;
	private String text;
	private String url;
	private Set<Keyword> keywords;
	private String lang;
	private Date date;
	private String polarity;
	private int retweets;
	private int favourites;
	
	/**
	 * Setter and getter functions  
	 */
	
	public int getMention_id() {
		return mention_id;
	}

	public void setMention_id(int mention_id) {
		this.mention_id = mention_id;
	}

	public String getSource_id() {
		return source_id;
	}

	public void setSource_id(String l) {
		this.source_id = l;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Set<Keyword> getKeywords() {
		return keywords;
	}

	public void setKeywords(Set<Keyword> keywords) {
		this.keywords = keywords;
	}

	public void addKeyword(Keyword k){
		this.keywords.add(k);
	}
	
	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public String getPolarity() {
		return polarity;
	}

	public void setPolarity(String polarity) {
		this.polarity = polarity;
	}

	public int getRetweets() {
		return retweets;
	}

	public void setRetweets(int rt) {
		this.retweets = rt;
	}

	public int getFavourites() {
		return favourites;
	}

	public void setFavourites(int favourites) {
		this.favourites = favourites;
	}
	
	
	public Mention(JSONObject json) {
		
	}
	
	public Mention(Status statusTwitter4j, String lang) {
		mentionFromTweet(statusTwitter4j,lang);
	}
	
	public Mention(String lang, String text, Date date, String url, String source_id) {
		setLang(lang);
		setText(text);
		setDate(date);
		setUrl(url);
		//setKeywords(kwrds);
		setSource_id(source_id);
		setPolarity("NULL");
	}

	private void mentionFromTweet(Status statusTwitter4j, String lang) {
		setLang(lang);
		setText(statusTwitter4j.getText());
		setDate(statusTwitter4j.getCreatedAt());
		setUrl("https://twitter.com/"+statusTwitter4j.getUser().getScreenName()+"/status/"+statusTwitter4j.getId());
		setRetweets(statusTwitter4j.getRetweetCount());
		setFavourites(statusTwitter4j.getFavoriteCount());
		setSource_id("tw_"+statusTwitter4j.getUser().getId());
		setPolarity("NULL");
	}
	
	/**
	 * @deprecated
	 */
	private void mentionFromFeed() {
		
	}
	
	/**
	 * Store mention into the database.
	 * 
	 * @param conn
	 * @return
	 */
	public int mention2db(Connection conn) {	

		PreparedStatement stmtM = null;
		PreparedStatement stmtKM = null;
		
		//PreparedStatement stmtS = null;		
		try {	
			
			//retrieve id of the last mention in db
			Statement stmt = conn.createStatement();
			ResultSet rs1 = stmt.executeQuery("SELECT max(mention_id) AS maxid FROM behagunea_app_mention");
			int id = 0;
			while (rs1.next()){
				id = rs1.getInt("maxid");
			}
			rs1.close();
			stmt.close();
			
			// prepare the sql statements to insert the mention in the DB and insert.
	        String mentionIns = "insert ignore into behagunea_app_mention (mention_id, date, source_id, url, text, lang, polarity, favourites, retweets) values (?,?,?,?,?,?,?,?,?)";
	        stmtM = conn.prepareStatement(mentionIns, Statement.RETURN_GENERATED_KEYS);
	        stmtM.setInt(1, id+1);
	       // System.err.println("daaaaaaataaaaa: "+getDate());
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");				
	        String dateString = dateFormat.format(getDate());
	        stmtM.setString(2, dateString);
	        //System.err.println("source: "+getSource_id());
	        stmtM.setString(3, getSource_id());
	        stmtM.setString(4, getUrl());
	        stmtM.setString(5, getText());
	        stmtM.setString(6, getLang());	        
	        stmtM.setString(7, getPolarity());
	        stmtM.setInt(8, getRetweets());
	        stmtM.setInt(9, getFavourites());
						
	        stmtM.executeUpdate();
			//ResultSet rs = stmtM.getGeneratedKeys();
			//retrieve the generated mention id, in order to use it in the keyword_mention table.
			//if(rs != null && rs.next()){
			//	setMention_id(rs.getInt(1));
				//System.out.println("Generated Emp Id: "+rs.getInt(1));
			//}
			stmtM.close();
				
			setMention_id(id+1);
			//connect mention to keywords
			String keywordMentionIns = "insert ignore into behagunea_app_keyword_mention (keyword_id, mention_id) values (?,?)";			
			stmtKM = conn.prepareStatement(keywordMentionIns, Statement.RETURN_GENERATED_KEYS);	        
			
			if (getKeywords()!=null && !getKeywords().isEmpty())
			{
				for (Keyword k: getKeywords()){
					//System.out.println("Mention2db: text: "+k.getText()+" cat: "+k.getCat()+" lang: "+k.getLang()+" subcat: "+k.getSubcat()+" id: "+k.getId());
					stmtKM.setInt(1, k.getId());
					stmtKM.setInt(2, getMention_id());
					stmtKM.executeUpdate();				
				}
			}
			stmtKM.close();			
			//stmtS.close();
		} catch (SQLException e) {
			System.err.println("elh-MSM::Mention mention2db - Error when trying to store mention into db.");
			e.printStackTrace();
		}

		return 1;
	}
	
	/**
	 * Add keyword connection into the into the database to an already existing mention..
	 * 
	 * @param conn
	 * @return
	 */
	public int addKwrd2db(Connection conn, int kwrdId) {	

		PreparedStatement stmtKM = null;
		//PreparedStatement stmtS = null;		
		try {			
			// prepare the sql statements to insert the mention in the DB and insert.						
			String keywordMentionIns = "insert ignore into keyword_mention (mention_id, keyword_id) values (?,?)";			
			
			//connect mention to keywords
			stmtKM.setInt(1, getMention_id());
			stmtKM.setInt(2, kwrdId);
			stmtKM.executeUpdate();
			stmtKM.close();						
			
		} catch (SQLException e) {
			System.err.println("elh-MSM::Mention mention2db - Error when trying add keyword to mention "+getMention_id()+" into db.");
			e.printStackTrace();
		}

		return 1;
	}
	
	/**
	 *  Function prints a mention to the stout;
	 * 
	 * @return
	 */
	public void print()
	{
		System.out.println("\ntext: "+text+"\nlang: "+lang+"\nkeywords: "+keywords.toString());
	}
	
	public int mention2solr(){
		return 1;
	}
}
