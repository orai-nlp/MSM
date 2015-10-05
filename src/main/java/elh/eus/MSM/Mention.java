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

import java.util.List;
import java.util.Date;

import twitter4j.JSONObject;
import twitter4j.Status;

import java.sql.* ;  // for standard JDBC programs
import java.math.* ; // for BigDecimal and BigInteger support

public class Mention {

	private int mention_id;
	private String source_id;
	private String text;
	private String url;
	private List<Keyword> keywords;
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

	public List<Keyword> getKeywords() {
		return keywords;
	}

	public void setKeywords(List<Keyword> keywords) {
		this.keywords = keywords;
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
		setLang(lang);
		setText(statusTwitter4j.getText());
		setDate(statusTwitter4j.getCreatedAt());
		setUrl("https://twitter.com/"+statusTwitter4j.getUser().getScreenName()+"/status/"+statusTwitter4j.getId());
		setRetweets(statusTwitter4j.getRetweetCount());
		setFavourites(statusTwitter4j.getFavoriteCount());
		setSource_id("tw_"+statusTwitter4j.getUser().getId());		
	}
	
	public Mention() {
		
	}

	private void mentionFromTweet() {
		
	}
	
	private void mentionFromFeed() {
		
	}
	
	public int mention2db(Connection conn) {	

		PreparedStatement stmt = null;
		try {			
			// prepare the sql statements to insert the mention in the DB and insert.
	        String mentionIns = "insert ignore into mention (date, source_id, url, text, lang, polarity, favourites, retweets) values (?,?,?,?,?,?,NULL)";
	        String keywordMentionIns = "insert ignore into keyword_mention (mention_id, keyword_term, keyword_lang) values (?,?,?)";
	        String source="insert ignore into source (id, type, influence) values (?,?,NULL)";	        
			stmt = conn.prepareStatement(mentionIns);
	        stmt.setString(1, getDate().toString());
	        stmt.setString(2, getSource_id());
	        stmt.setString(3, getUrl());
	        stmt.setString(4, getLang());
	        stmt.setString(5, getPolarity());
	        stmt.setInt(6, getRetweets());
	        stmt.setInt(7, getFavourites());
			stmt.executeUpdate(mentionIns);
			stmt.close();
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return 1;
	}
	
	public int mention2solr(){
		return 1;
	}
}
