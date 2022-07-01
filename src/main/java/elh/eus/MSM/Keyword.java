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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.sql.* ;  // for standard JDBC programs
import javax.naming.NamingException;


public class Keyword {

	private int keyword_id;
	private String text;
	private String lang;
	private String subcat;
	private String cat;
	private String screenTag;
	private boolean needsAnchor;
	private boolean isAnchor;
	private boolean isKword;
	private boolean caseSensitiveSearch = false;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}
	
	public String getCat() {
		return cat;
	}

	public void setCat(String cat) {
		this.cat = cat;
	}
	
	public String getSubcat() {
		return subcat;
	}

	public void setSubcat(String cat) {
		this.subcat = cat;
	}

	public String getScreenTag() {
		return screenTag;
	}

	public void setScreenTag(String tag) {
		this.screenTag = tag;
	}

	public void setIsAnchor(boolean anc) {
		this.isAnchor = anc;
	}
	
	public boolean isAnchor()
	{
		return isAnchor;
	}
	
	public void setNeedsAnchor(boolean anc) {
		this.needsAnchor = anc;
	}
	
	public boolean needsAnchor()
	{
		return needsAnchor;
	}
	
	public void setIsKword(boolean kw) {
		this.isKword = kw;
	}
	
	public boolean isKword()
	{
		return isKword;
	}
	
	public void setCaseSensitiveSearch(boolean css) {
		this.caseSensitiveSearch = css;
	}
	
	public boolean getCaseSensitiveSearch()
	{
		return caseSensitiveSearch;
	}
	
	/**
	 *  Constructor
	 */
	public Keyword() {		 
	}
	
	/**
	 * Constructor
	 * 
	 * @param txt
	 * @param lng
	 */
	public Keyword(String txt, String lng) {
		this(txt,lng,false,false,true,txt);
	}
	
	/**
	 * Constructor
	 * 
	 * @param txt
	 * @param lng
	 */
	public Keyword(String txt, String lng, boolean isAnch, boolean needsAnch, boolean isKey, String scrTag) {
		if (txt.startsWith(":"))
		{
			setText(txt.substring(1, txt.length()));
			setCaseSensitiveSearch(true);
		}
		else
		{
			setText(txt);
		}
		setLang(lng);
		setIsAnchor(isAnch);
		setNeedsAnchor(needsAnch);
		setIsKword(isKey);
		setScreenTag(scrTag);
	}
	
	
	/**
	 * @return
	 */
	public int getId(){
		return keyword_id;
	}
	
	/**
	 * 
	 */
	public void setId(int id){
		keyword_id = id;
	}

	
	/**
	 * Retrieve keywords from database. Normally in order to launch a crawler or search engine queries
	 * 
	 * @param conn
	 * @param type (twitter|press|behagune)
	 * @return
	 * @throws NamingException
	 * @throws SQLException
	 */
	public static Set<Keyword> retrieveFromDB(Connection conn, String type, String lang, String tableprefix) throws NamingException, SQLException {

		Set<Keyword> result = new HashSet<Keyword>(); 
		Statement stmt = conn.createStatement();
		String langCondition = "";
		if (!lang.equalsIgnoreCase("all"))
		{
			String[]langs = lang.split(",");
			StringBuilder sb = new StringBuilder();
			sb.append(" AND lang in ( ");
			for (String l: langs)
			{
				sb.append("'").append(l).append("',"); 
			}
			langCondition=sb.substring(0, sb.length()-1)+")";
		}
		
		String query = "SELECT * FROM "+tableprefix+"_app_keyword where type='"+type+"'"+langCondition;
		//System.err.println("MSM::Keyword::retrieveFromDB - query:"+query);
		ResultSet rs = stmt.executeQuery(query);
		
		
		try{	
			while (rs.next()) {
				Keyword kwrd = new Keyword(rs.getString("term"), rs.getString("lang"),rs.getBoolean("is_anchor"),rs.getBoolean("anchor"),rs.getBoolean("is_kword"),rs.getString("screen_tag"));
				kwrd.setId(rs.getInt("keyword_id"));
				result.add(kwrd);
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
	
	public static List<Keyword> getAnchors (List<Keyword> kwrds)
	{
		List<Keyword> result = new ArrayList<Keyword>(); 
		for (Keyword k: kwrds)
		{
			if (k.isAnchor())
			{
				result.add(k);
			}
		}
		return result;
	}

	/*public List<String> getAllTexts ()
	{
		List<String> rslt = new ArrayList<String>();
		rslt.add(getTextEu());
		rslt.add(getTextEs());
		rslt.add(getTextEn());
		rslt.add(getTextFr());
		
		return rslt;
	}*/
	
	/**
	 * Retrieve keywords from database. Normally in order to launch a crawler or search engine queries
	 * 
	 * @param conn
	 * @param type (twitter|press|behagune)
	 * @return
	 * @throws NamingException
	 * @throws SQLException
	 */
	public static Set<Keyword> createFromList(List<String> keyList, List<String> langs) 
	{
		Set<Keyword> result = new HashSet<Keyword>(); 
		if (keyList.isEmpty()) {
			return result;
		}
		for (String key : keyList)
		{
			//System.err.println("creating kword list: "+ key);
			for (String l: langs)
			{
				Keyword k = new Keyword(key,l,false,false,true,key);
				k.setId(result.size()+1);
				result.add(k);				 
			}
		}
		return result;
	}
	
}
