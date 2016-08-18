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


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;

import javax.naming.NamingException;

import twitter4j.JSONException;
import twitter4j.JSONObject;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;


public final class Utils {

	
	
	/**
	* Check input file integrity.
	* @param name
	* the name of the file
	* @param inFile
	* the file
	*/
	public static boolean checkFile(final String name) 
	{
		try {
			return checkFile(new File(name));
		}catch(NullPointerException ne){
			return false;
		}
	}

	/**
	* Check input file integrity.
	* @param name
	* the name of the file
	* @param inFile
	* the file
	*/
	public static boolean checkFile(final File f) 
	{
		boolean isFailure = true;
		
		if (! f.isFile()) {
			isFailure = false;
		} 
		else if (!f.canRead()) {
			isFailure = false;
		}
		return isFailure;
	}

	
	/**
	* Check input directory integrity.
	* @param name
	* the name of the directory
	* @param inFile
	* the directory
	*/
	public static boolean checkDir(final String name) 
	{
		try {
			return checkDir(new File(name));
		}catch(NullPointerException ne){
			return false;
		}
	}
	
	/**
	* Check input directory integrity.
	* @param name
	* the name of the directory
	* @param inFile
	* the directory
	*/
	public static boolean checkDir(final File f) 
	{
		boolean isFailure = true;
		
		if (! f.isDirectory()) {
			isFailure = false;
		} 
		else if (!f.canRead()) {
			isFailure = false;
		}
		return isFailure;

	}
	
	/**
	* return the name (without path) of a string. The function assumes that the input is a file path.
	* 
	* @param fname : string to extract the name from
	* the directory
	*/
	public static String fileName(String fname) 
	{
		File f = new File(fname);
		return f.getName();
	}
	
	/**
	 * Function creates a temporal directory with a random name.
	 */
	public static File createTempDirectory()
			throws IOException
	{
		final File temp;

		temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

		if(!(temp.delete()))
		{
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}

		if(!(temp.mkdir()))
		{
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}

		return (temp);
	}

	/**
	 * Function renames a file to a new name. if the new name already exists throws an exception.
	 *  
	 */
	public static void renameFile (String oldFile, String newName) throws IOException 
	{
		File file1 = new File(oldFile);
		File file2 = new File(newName);
		if(file2.exists())
		{
			throw new java.io.IOException("Utils::renameFile : file exists");
		}
		else if (! file1.renameTo(file2)) 
		{
			System.err.println("Utils::renameFile : moving file failed\n\t"+file1.getAbsolutePath()+"\t"+file2.getAbsolutePath()); 
		}
		// Rename file (or directory)
		//file1.renameTo(file2);		 
	}
	
	
	/**
	 * Function opens a connection with a mysql database given connection information.
	 * 
	 * @param usr
	 * @param pass
	 * @param host
	 * @param db
	 * @return
	 * @throws NamingException
	 * @throws SQLException
	 */
	public static Connection DbConnection (String usr, String pass, String host, String db) throws NamingException, SQLException {
					
			MysqlDataSource ds = new MysqlDataSource();
			ds.setUser(usr);
			ds.setPassword(pass);
			ds.setServerName(host);
			ds.setDatabaseName(db);			
			//System.err.println("Utils::DbConnection -> connection attributes: "+ds.getUrl());
			Connection conn = ds.getConnection();
			return conn;		
	}
	
	/**
	 * Function to read a JSON object from an url
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException
	{		
		URL urlObj = new URL(url);
		BufferedReader rd = new BufferedReader(new InputStreamReader(urlObj.openStream()));
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) 
		{
			sb.append((char) cp);
		}
		JSONObject json = new JSONObject(sb.toString());	
	     
		return json;
	  }
	
	
	/**
	 * Function reads two column file and stores the values into a HashMap<String,String> object 
	 * 
	 * @param resource : InputStream containing the two column resource in tab separated format. 
	 *                   The file may contain more than two columns, ONLY the first two will be taken into account.
	 * @return HashMap<String,String> contains the elements and their respective attribute values. 
	 *         First column in the inputStream is used as key in the map.
	 * 
	 * @throws IOException if the given resource has reading problems.
	 */
	public static HashMap<String, String> loadTwoColumnResource(InputStream resource) 
			throws IOException
	{
		HashMap<String, String> result = new HashMap<String, String>();		
		
		if (resource == null)
		{
			System.err.println("utilsElh::loadTwoColumnResource - resource is null");
			return result;
		}	
		
		BufferedReader breader = new BufferedReader(new InputStreamReader(resource));
		String line;
		while ((line = breader.readLine()) != null) 
		{
			if (line.startsWith("#") || line.matches("^\\s*$"))
			{
				continue;
			}
			String[] fields = line.split("\t");
			try{
				result.put(fields[0], fields[1]);
			}catch (IndexOutOfBoundsException ioobe){
				System.err.println("utilsElh::loadTwoColumnResource - "+line);
			}
		}											
		breader.close();
		return result;
	}
	
	/**
	 * Function reads a single column file and stores the values into a HashSet<String> object 
	 * 
	 * @param resource : InputStream containing the single column resource. 
	 *                   The file may contain more columns  in tab separated format. ONLY the first one will used.
	 * @return HashSet<Long> contains the elements in the file. 
	 * 
	 * @throws IOException if the given resource has reading problems.
	 */
	public static HashSet<Long> loadOneColumnResource(InputStream resource) 
			throws IOException 
	{
		HashSet<Long> result = new HashSet<Long>();		
		if (resource == null)
		{
			System.err.println("UtilsElh::loadOneColumnResource - resource is null");
			return result;
		}		
		
		BufferedReader breader = new BufferedReader(new InputStreamReader(resource));
		String line;
		while ((line = breader.readLine()) != null) 
		{
			if (line.startsWith("#") || line.matches("^\\s*$"))
			{
				continue;
			}
			try{
				result.add(Long.valueOf(line.trim()));
				//System.err.println("FileUtilsElh::loadOneColumnResource - "+line.trim());
			}catch (IndexOutOfBoundsException ioobe){
				System.err.println("UtilsElh::loadOneColumnResource - "+line);
			}catch (NumberFormatException nfe){
				System.err.println("UtilsElh::loadOneColumnResource - unparseable line, not number: "+line);
			}
		}											
		breader.close();
		return result;
	}
	
	
}
