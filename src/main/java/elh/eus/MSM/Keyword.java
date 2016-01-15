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
import java.util.List;
import java.util.Properties;
import java.sql.* ;  // for standard JDBC programs
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.* ; // for BigDecimal and BigInteger support

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;


public class Keyword {

	private int keyword_id;
	private String text;
	private String lang;
	private String subcat;
	private String cat;
	private String screenTag;
	private boolean needsAnchor;
	private boolean isAnchor;

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
		this(txt,lng,false,false);
	}
	
	/**
	 * Constructor
	 * 
	 * @param txt
	 * @param lng
	 */
	public Keyword(String txt, String lng, boolean isAnch, boolean needsAnch) {
		setText(txt);
		setLang(lng);
		isAnchor=isAnch;
		needsAnchor=needsAnch;
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
	public static List<Keyword> retrieveFromDB(Connection conn, String type, String lang) throws NamingException, SQLException {

		List<Keyword> result = new ArrayList<Keyword>(); 
		Statement stmt = conn.createStatement();
		String[]langs = lang.split(",");
		StringBuilder sb = new StringBuilder();
		sb.append("( ");
		for (String l: langs)
		{
			sb.append("'").append(l).append("',"); 
		}
		String whereClause=sb.substring(0, sb.length()-1)+")";
		ResultSet rs = stmt.executeQuery("SELECT * FROM keyword where type="+type+" AND lang in"+whereClause);
		
		try{	
			while (rs.next()) {
				Keyword kwrd = new Keyword(rs.getString("term"), rs.getString("lang"),rs.getBoolean("is_anchor"),rs.getBoolean("anchor"));
				kwrd.setId(rs.getInt("keyword_id"));
				result.add(kwrd);
			}
		} catch (SQLException sqle ) {
			sqle.printStackTrace();
		} finally {
			if (stmt != null) { stmt.close(); }
		}

		rs.close();
		conn.close();
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
	
	
	
}
