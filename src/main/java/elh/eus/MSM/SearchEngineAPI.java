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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Search engine API query launcher.
 * 
 * @author Iñaki San Vicente
 *
 */
public class SearchEngineAPI {

	private Properties params = new Properties();	
	private URL queryStr;

	public URL getQueryStr() {
		return queryStr;
	}

	public void setQueryStr(URL feedUrl) {
		this.queryStr = feedUrl;
	}

	public SearchEngineAPI(String query) {
		try {
			setQueryStr(new URL(query));
		} catch (MalformedURLException ue ) {
			System.err.println("MSM::SearchEngineAPI - ERROR: malformed source url given"+ue.getMessage());
		} 

		queryAPI();
	}

	public SearchEngineAPI(String config, String store) {
		try {
			params.load(new FileInputStream(new File(config)));
		} catch (FileNotFoundException fe){
			System.err.println("elh-MSM::SearchEngineAPI - Config file not found "+config);
			System.exit(1);
		} catch (IOException ioe){
			System.err.println("elh-MSM::SearchEngineAPI - Config file could not read "+config);
			System.exit(1);
		} 

		String source = params.getProperty("feedURL", "none");
		
		try {
			setQueryStr(new URL(source));
		} catch (MalformedURLException ue ) {
			System.err.println("MSM::SearchEngineAPI - ERROR: malformed source url given"+ue.getMessage());
		} 

		queryAPI();
	}


	private void queryAPI (){

		boolean ok = false;
		try {
			ok = true;
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("ERROR: "+ex.getMessage());
		} 	        

		if (!ok) {
			System.out.println();
			System.out.println("SearchEngineAPI launches queries to search engine APIs.");
			System.out.println("The first parameter must be the query to launch to search engines.");
			System.out.println();
		}	
	}




}
