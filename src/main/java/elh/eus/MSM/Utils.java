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


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.google.common.base.Optional;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;


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
		return checkFile(new File(name));
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
		return checkDir(new File(name));
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
	
	
	public static String detectLanguage(String input, String supposedLang)
	{
		String result = "unk";
		try {
			//load all languages:
			List<LanguageProfile> languageProfiles = new LanguageProfileReader().readAllBuiltIn();			

			//build language detector:
			LanguageDetector languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
					.withProfiles(languageProfiles)
					.build();

			//create a text object factory
			TextObjectFactory textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();

			//query:
			TextObject textObject = textObjectFactory.forText("my text");
			Optional<LdLocale> lang = languageDetector.detect(textObject);
			result = (String) lang.asSet().toArray()[0];
			
		} catch (IOException ioe){
			System.err.println("Utils::detectLanguage -> Error when loading language models");
			System.exit(1);
		}
		return result;
	}
	
	public static Connection DbConnection (String usr, String pass, String host, String db) throws NamingException, SQLException {
					
			MysqlDataSource ds = new MysqlDataSource();
			ds.setUser(usr);
			ds.setPassword(pass);
			ds.setServerName(host);
			ds.setDatabaseName(db);			
			//ds.setUrl("jdbc:mysql://"+host+":3306/"+db);
			Connection conn = ds.getConnection();
			return conn;		
	}
	
}
