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
	private String textEu;
	private String textEs;
	private String textEn;
	private String textFr;

	public String getTextEu() {
		return textEu;
	}

	public void setTextEu(String textEu) {
		this.textEu = textEu;
	}

	public String getTextEs() {
		return textEs;
	}

	public void setTextEs(String textEs) {
		this.textEs = textEs;
	}

	public String getTextEn() {
		return textEn;
	}

	public void setTextEn(String textEn) {
		this.textEn = textEn;
	}

	public String getTextFr() {
		return textFr;
	}

	public void setTextFr(String textFr) {
		this.textFr = textFr;
	}
	
	
	/**
	 *  Constructor
	 */
	public Keyword() {		 
	}
	
	/**
	 * Constructor
	 * 
	 * @param txtEu
	 * @param txtEs
	 * @param txtEn
	 * @param txtFr
	 */
	public Keyword(String txtEu, String txtEs, String txtEn, String txtFr) {
		textEu = txtEu;
		textEs = txtEs;
		textEn = txtEn;
		textFr = txtFr;
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
	 * @return
	 * @throws NamingException
	 * @throws SQLException
	 */
	public static List<Keyword> retrieveFromDB(Connection conn) throws NamingException, SQLException {

		List<Keyword> result = new ArrayList<Keyword>(); 
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT * FROM keyword");
		try{	
			while (rs.next()) {
				Keyword kwrd = new Keyword(rs.getString("term_eu"), rs.getString("term_es"), rs.getString("term_en"), rs.getString("term_fr"));
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

	public List<String> getAllTexts ()
	{
		List<String> rslt = new ArrayList<String>();
		rslt.add(getTextEu());
		rslt.add(getTextEs());
		rslt.add(getTextEn());
		rslt.add(getTextFr());
		
		return rslt;
	}
	
}
