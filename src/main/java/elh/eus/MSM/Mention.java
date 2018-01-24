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
import java.util.Date;
import java.util.Set;

import twitter4j.GeoLocation;
import twitter4j.Status;

import java.sql.* ;  // for standard JDBC programs
//import java.math.* ; // for BigDecimal and BigInteger support

public class Mention {

	private int mention_id;
	private long source_id;
	private long nativeId;
	private String text;
	private String url;
	private Set<Keyword> keywords;
	private String lang;
	private Date date;
	private String polarity;
	private int retweets;
	private int favourites;
	private String geoInfo;
	private boolean isRetweet;
	private boolean isQuote;
	//private boolean inReply; //not used for the moment 
	private long origTweetId;
	private long quotedTweetId;
	private boolean isLocalArea;
	private String offset;
	private String mediaUrl;
	private Long feedId;
	
	/**
	 * Setter and getter functions  
	 */
	
	public int getMention_id() {
		return mention_id;
	}

	public void setMention_id(int mention_id) {
		this.mention_id = mention_id;
	}

	public long getSource_id() {
		return source_id;
	}

	public void setSource_id(long l) {
		this.source_id = l;
	}

	public long getNativeId() {
		return nativeId;
	}

	public void setNativeId(long l) {
		this.nativeId = l;
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
	
	public String getGeoInfo() {
		return geoInfo;
	}

	public void setGeoInfo(String geo) {
		this.geoInfo = geo;
	}
	
	public boolean getIsRetweet() {
		return isRetweet;
	}

	public void setIsRetweet(boolean rt) {
		this.isRetweet = rt;
	}
	
	public long getOrigTweetId() {
		return origTweetId;
	}

	public void setOrigTweetId(long twtId) {
		this.origTweetId = twtId;
	}
	
	public boolean getIsQuote() {
		return isQuote;
	}

	public void setIsQuote(boolean q) {
		this.isQuote = q;
	}
	
	public long getQuotedTweetId() {
		return quotedTweetId;
	}

	public void setQuotedTweetId(long twtId) {
		this.quotedTweetId = twtId;
	}
		
	public boolean getIsLocalArea() {
		return isLocalArea;
	}

	public void setIsLocalArea(boolean ila) {
		this.isLocalArea = ila;
	}
		
	public String getOffset() {
		return offset;
	}

	public void setOffset(String offset) {
		this.offset = offset;
	}

	public String getMediaUrl() {
		return mediaUrl;
	}

	public void setMediaUrl(String mediaUrl) {
		this.mediaUrl = mediaUrl;
	}

	public long getFeedId() {
		return feedId;
	}

	public void setFeedId(Long feedId) {
		this.feedId = feedId;
	}
	//END OF GETTERS AND SETTERS
	
	//CONSTRUCTORS
		
		
	/**
	 * 
	 * Constructor for tweet derived mentions
	 * 
	 * @param statusTwitter4j
	 * @param lang
	 * @param isLocal
	 */
	public Mention(Status statusTwitter4j, String lang, boolean isLocal) {
		mentionFromTweet(statusTwitter4j,lang, isLocal);
	}
	
	/**
	 * 
	 * Constructor for rss feed origin mentions
	 * 
	 * @param lang
	 * @param text
	 * @param date
	 * @param url
	 * @param source_id
	 * @param isLocal
	 */
	public Mention(String lang, String text, Date date, String url, long source_id, boolean isLocal) {
		this(lang,text,date,url,source_id,isLocal,"-1","",(long) 0);		
	}
	
	/**
	 * 
	 * Constructor for multimedia origin mentions
	 * 
	 * @param lang
	 * @param text
	 * @param date
	 * @param url
	 * @param source_id
	 * @param isLocal
	 * @param offset
	 * @param mediaUrl
	 * @param feedId
	 */
	public Mention(String lang, String text, Date date, String url, long source_id, boolean isLocal, String offset, String mediaUrl,Long feedId) {
		setLang(lang);
		setText(text);
		setDate(date);
		System.err.println("MSM::Mention - multimedia constructor: date:"+date);
		setUrl(url);
		//setKeywords(kwrds);
		setSource_id(source_id);
		setPolarity("NULL");
		setIsRetweet(false);
		setIsQuote(false);
		setOrigTweetId(0);
		setQuotedTweetId(0);
		setIsLocalArea(isLocal);
		setOffset(offset);
		setMediaUrl(mediaUrl);
		setFeedId(feedId);
	}

	//END OF CONSTRUCTORS
	
	/**
	 * 
	 *  This is the void that actually creates the mentions starting from a tweet
	 * 
	 * @param statusTwitter4j
	 * @param lang
	 * @param isLocal
	 */
	private void mentionFromTweet(Status statusTwitter4j, String lang, boolean isLocal) {
		setLang(lang);			
		setText(statusTwitter4j.getText());
		setDate(statusTwitter4j.getCreatedAt());
		setNativeId(statusTwitter4j.getId());
		setUrl("https://twitter.com/"+statusTwitter4j.getUser().getScreenName()+"/status/"+statusTwitter4j.getId());
		setRetweets(statusTwitter4j.getRetweetCount());
		setFavourites(statusTwitter4j.getFavoriteCount());
		setSource_id(statusTwitter4j.getUser().getId());
		setPolarity("NULL");
		setIsRetweet(false);
		setIsQuote(false);
		setOrigTweetId(0);
		setQuotedTweetId(0);
		setIsLocalArea(isLocal);
		//A tweet does not contain offset and mediaUrl information for the moment.
		setOffset("-1");
		setMediaUrl("");
		setFeedId((long) 0);

		
		//geoInformation
		String geoStr = "unknown";
		if(statusTwitter4j.getGeoLocation() != null)
		{			
			geoStr = "";
			geoStr = geoStr+"long="+String.valueOf(statusTwitter4j.getGeoLocation().getLongitude());
			geoStr = geoStr+"_lat="+String.valueOf(statusTwitter4j.getGeoLocation().getLatitude());
			setGeoInfo(geoStr);
		} 
		else if (statusTwitter4j.getPlace() != null )
		{
			GeoLocation[][] geo =statusTwitter4j.getPlace().getBoundingBoxCoordinates();
			geoStr = "";
			geoStr = geoStr+"long="+String.valueOf(geo[0][0].getLongitude());
			geoStr = geoStr+"_lat="+String.valueOf(geo[0][0].getLatitude());
			geoStr = geoStr+";long="+String.valueOf(geo[0][2].getLongitude());
			geoStr = geoStr+"_lat="+String.valueOf(geo[0][2].getLatitude());									
		}
		setGeoInfo(geoStr);
		
		if (statusTwitter4j.isRetweet())
		{
			setIsRetweet(true);
			setOrigTweetId(statusTwitter4j.getRetweetedStatus().getId());
			//statusTwitter4j = statusTwitter4j.getRetweetedStatus();
		}
		
		if (statusTwitter4j.getQuotedStatusId()>0)
		{
			setIsQuote(true);
			setQuotedTweetId(statusTwitter4j.getQuotedStatus().getId());
			//statusTwitter4j = statusTwitter4j.getRetweetedStatus();
		}
		
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
		int success = 0;
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
			
			setMention_id(id+1);
			System.err.println("mention2db: current id "+getMention_id());
			
			// prepare the sql statements to insert the mention in the DB and insert.
	        String mentionIns = "insert into behagunea_app_mention (mention_id, date, source_id, url, text, lang, polarity, favourites, retweets, geoinfo, native_id, retweet_id, quote_id, is_local_area, offset, media_url,feed_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	        stmtM = conn.prepareStatement(mentionIns, Statement.RETURN_GENERATED_KEYS);
	        stmtM.setInt(1, getMention_id());
	        System.err.println("daaaaaaataaaaa: "+getDate());
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");				
	        String dateString = dateFormat.format(getDate());
	        stmtM.setString(2, dateString);
	        //System.err.println("source: "+getSource_id());
	        stmtM.setLong(3, getSource_id());
	        stmtM.setString(4, getUrl());
	        stmtM.setString(5, getText());
	        stmtM.setString(6, getLang());	        
	        stmtM.setString(7, getPolarity());
	        stmtM.setInt(8, getRetweets());
	        stmtM.setInt(9, getFavourites());
	        String geo = getGeoInfo();
	        if (geo == null){
	        	geo = "unknown";
	        }
	        stmtM.setString(10, geo);
	        if (getNativeId()!=0){
		        stmtM.setLong(11, getNativeId());	        	
	        }
	        else{
		        stmtM.setLong(11, -getMention_id());
	        }
	        stmtM.setLong(12, getOrigTweetId());
	        stmtM.setLong(13, getQuotedTweetId());
	        stmtM.setBoolean(14, getIsLocalArea());
	        stmtM.setString(15, getOffset());
	        stmtM.setString(16, getMediaUrl());
	        stmtM.setLong(17, getFeedId());
	        
	        stmtM.executeUpdate();
			//ResultSet rs = stmtM.getGeneratedKeys();
			//retrieve the generated mention id, in order to use it in the keyword_mention table.
			//if(rs != null && rs.next()){
			//	setMention_id(rs.getInt(1));
				//System.out.println("Generated Emp Id: "+rs.getInt(1));
			//}
			stmtM.close();
						
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
			success=1;
		} catch (SQLException e) {
			System.err.println("elh-MSM::Mention mention2db - Error when trying to store mention into db.");
			e.printStackTrace();			
		}

		return success;
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
			String keywordMentionIns = "insert ignore into behagunea_app_keyword_mention (mention_id, keyword_id) values (?,?)";			
			stmtKM = conn.prepareStatement(keywordMentionIns, Statement.RETURN_GENERATED_KEYS);	  
			
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
	 *  Function looks if the current mention exists in the DB based on each
	 *  native id (Twitter status id, for example)
	 *  
	 * 
	 * @param conn
	 * @return
	 */
	public long existsInDB (Connection conn)
	{
		long result = -1;
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT mention_id FROM behagunea_app_mention where native_id="+getNativeId());
			if(rs.next()){
	            result = rs.getLong(1);
	        }
			stmt.close();
		} catch (SQLException e){
			e.printStackTrace();
		}
		return (result);
	}
	
	/**
	 *  Function updates the retweet and favourite counts of a mention (tweet) in the DB backed system,
	 *  based on each native id (Twitter status id, for example)
	 *  
	 * 
	 * @param conn
	 * @return
	 */
	public int updateRetweetFavouritesInDB (Connection conn, long mId)
	{
		int success = 1;
		try {
			Statement stmt = conn.createStatement();
			String updateComm = "UPDATE behagunea_app_mention "
					+ "SET retweets="+getRetweets()+", favourites="+getFavourites()+" WHERE mention_id="+mId;
			// execute update
			stmt.executeUpdate(updateComm);
			stmt.close();
		} catch (SQLException e){
			e.printStackTrace();
			success=0;
		}
		return (success);
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
