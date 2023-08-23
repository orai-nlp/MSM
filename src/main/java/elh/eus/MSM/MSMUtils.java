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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.openqa.selenium.Pdf;
import org.xml.sax.InputSource;

import twitter4j.JSONArray;
import twitter4j.JSONException;
import twitter4j.JSONObject;

import org.jsoup.parser.Parser;

//import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;


public final class MSMUtils {

	
	private static List<DateFormat> dateFormats= new ArrayList<DateFormat>(
			Arrays.asList(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z"),
					new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
					new SimpleDateFormat("yyyy-MM-dd HH:mm"),
					new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z")));
	
	public static final Pattern duplicatedDomainInUrl = Pattern.compile("http[s]?://");

	private static final Map<Pattern,String> unicodeConversions =  new HashMap<Pattern, String>() {
		{
			put(Pattern.compile("(\u201c|\u201d|\u0093|\u0094)"), "\"");
			put(Pattern.compile("(\u2018|\u2019|\u0092)"), "\'");			
			put(Pattern.compile("\u20ac"),"€");
			put(Pattern.compile("\u2026"), "...");
			put(Pattern.compile("\u0096"), "-");
			put(Pattern.compile("(\u2008|\u2028|\u2009|\u200a\u008d)"), " ");
		}
	};
		
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
	 * IMPORTANT NOTE: ssl is disabled in order to avoid connection problems, and because we work in secure environments. 
	 *                 If connection is going to be made with a remote DB and security may be compromised this should be changed to user proper identity verification.
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
			ds.setUseSSL(false);
			//System.err.println("Utils::DbConnection -> connection attributes: "+ds.getUrl());
			Connection conn = ds.getConnection();
			return conn;		
	}
	
	
	/**
	 * Function checks if there are mentions in comming from a certain url. 
	 * Used by the FeedReader class to check if a feed element has already been parsed. 
	 * 
	 * @param dbconn
	 * @param url
	 * @return
	 */
	public static int mentionsInDB(Connection dbconn, String url, String tableprefix) {
		int urlKop = 0 ;
		try 
		{	
				String query = "SELECT count(*) FROM "
					+ tableprefix +"_app_mention "
					+ "WHERE url='"+url+"'";

				Statement st = dbconn.createStatement();			       
				// execute the query, and get a java resultset
				ResultSet rs = st.executeQuery(query);
				
				while (rs.next()){
					urlKop = rs.getInt(1); 
				}
				st.close();				
		} catch(SQLException sqle) {
			System.err.println("MSMUtils::mentionsInDB ->  MYSQL ERROR when looking for parsed urls in DB "+url);
		}
		return urlKop;
	}
	
	
	
	/**
	 * Function to read a JSON object from an url
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 * @throws KeyStoreException 
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyManagementException 
	 * @throws URISyntaxException 
	 */
	public static JSONObject readJsonFromUrl(URL url) throws IOException, JSONException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException, URISyntaxException
	{				
		System.err.println(url.toString());
		//HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();
		CloseableHttpClient client = httpClient();
		RequestConfig localConfig = RequestConfig.custom()
				.setCookieSpec(CookieSpecs.STANDARD)
				.build();				
				
		URI linkUri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef()); 		
		HttpUriRequest get = new HttpGet(linkUri);        		
		CloseableHttpResponse response = client.execute(get);
		InputStream stream = response.getEntity().getContent();	
		BufferedReader rd = new BufferedReader(new InputStreamReader(stream));
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) 
		{
			sb.append((char) cp);
		}
		//System.err.println(sb.toString());
		JSONObject json = new JSONObject(sb.toString());	
	     
		return json;
	  }

	/**
	 * Function to read a JSON array of objects from an url
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	public static JSONArray readJsonArrayFromUrl(URL url) throws IOException, JSONException
	{			
		System.err.println(url.toString());
		BufferedReader rd = new BufferedReader(new InputStreamReader(url.openStream()));
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) 
		{
			sb.append((char) cp);
		}
		//System.err.println(sb.toString());
		JSONArray json = new JSONArray(sb.toString());	
	     
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
	
	
	/**
	 *  Function parses a string and returns a date element if the string contains an accepted format. 
	 *  Else an empty date element is returned. accepted formats are store in dateFormats constant.
	 *   
	 * @param strDate
	 * @return
	 */
	public static Date parseDate (String strDate){
		Date date = new Date();
		for (DateFormat df : dateFormats)
		{
			try {
				date = df.parse(strDate);
				break;						
			}catch(ParseException pe){
				//continue loop
			}
		}
		return date;
	}
	
	/**
	 *  Creates an http client connection to fetch urls.
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws KeyStoreException
	 */
	public static CloseableHttpClient httpClient () throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
		//HttpClient client = HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategy()).build();
		RequestConfig globalConfig = RequestConfig.custom()
				.setCookieSpec(CookieSpecs.DEFAULT)
				.build();
		CloseableHttpClient client = HttpClients.custom()
				.setDefaultRequestConfig(globalConfig)
				.setUserAgent("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:63.0) Gecko/20100101 Firefox/63.0")
				.setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
			    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
				.build();
		//client.setRedirectStrategy(new LaxRedirectStrategy());
		return client;
		
	}
	
	/**
	 * save html content to pdf by means of openhtmltopdf library.
	 * 
	 * @param in
	 * @param storePath
	 * @param filename
	 * @return true if succesfull, false otherwise
	 */
	public static boolean saveHtml2pdf(FeedArticle in, String storePath, String link) 
	{
		String filename=link;
		try {
			filename=duplicatedDomainInUrl.matcher(filename).replaceFirst("");
			String filename_pdf=filename.replaceAll(".htm[l]?$", ".pdf");
			String fullfilepath=storePath+filename_pdf;
			File outfile = new File(fullfilepath);
			outfile.getParentFile().mkdirs();
			System.err.println("MSMUtils::saveHtml2pdf -> path to store file: "+fullfilepath);
			//File outfilehtml=new File(storePath+filename);
			//PrintWriter p = new PrintWriter(new FileOutputStream(outfilehtml));
			for (String img : in.getImages()){
				System.err.println("MSMUtils::saveHtml2pdf -> image for the article: "+img);			
			}
			String validxml="<html><head></head>"
					+ "<body><h1><a href=\""+link+"\">"+in.getTitle()+"</a></h1><p>"+in.getAuthor()+"</p>"+
					"<p>"+in.getText()+"</p></body></html>";
			for (String img : in.getImages()){
				validxml="<html><head><style>\n"
						+ "        @page {\n"
						+ "			  size: A4 landscape;\n"
						+ "			}\n"
						+ "			* {\n"
						+ "            margin: 0;\n"
						+ "            padding: 0;\n"
						+ "        }\n"
						+ "        .imgbox {\n"
						+ "            display: grid;\n"
						+ "            height: 100%;\n"
						+ "        }\n"
						+ "        .center-fit {\n"
						+ "            max-width: 100%;\n"
						+ "            max-height: 100vh;\n"
						+ "            margin: auto;\n"
						+ "        }\n"
						+ "    </style></head><body><h1><a href=\""+link+"\">"+in.getTitle()+"</a></h1><p>"+in.getAuthor()+"</p>"+
							"<div class=\"imgbox\"><img class=\"center-fit\" src=\""+img+"\"></img></div>"+
							"<p>"+in.getText()+"</p></body></html>";
				break;
			}
			/*
			//clean and make html valid xml
			org.jsoup.nodes.Document doc= Jsoup.parse(in,link);
			doc.select("script").remove();
			//org.jsoup.nodes.Elements selector = document.select("style");
			//for (Element element : selector) {
			//    String elem_html = element.html().replaceAll("<", "&lt;")..replaceAll("&", "&amp;");
			//}
		    doc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);  
		    doc.outputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);
			String validxml=doc.toString();*/
			//System.err.println("MSMUtils::saveHtml2pdf -> xml to store: "+validxml);			
			//p.print(validxml);
			//p.close();
			OutputStream os = new FileOutputStream(outfile);
			PdfRendererBuilder builder = new PdfRendererBuilder();
			builder.useFastMode();
			builder.withHtmlContent(validxml,fullfilepath); //)withUri(is);
			builder.toStream(os);
			builder.run();
			os.close();
			return true;
		}catch (Exception e) {
			System.err.println("MSMUtils::saveHtml2pdf -> io error when writing "+filename+
					"\n"+e.getMessage()+
					"\n"+e.getStackTrace().toString());
			return false;
		}
	}

	public static boolean saveHtml2pdfSelenium(Pdf pdf, String storePath, String filename) {
		boolean success=false;
		try {
			filename=duplicatedDomainInUrl.matcher(filename).replaceFirst("");
			String filename_pdf=filename.replaceAll(".htm[l]?$", ".pdf");
			String fullfilepath=storePath+filename_pdf;
			FileOutputStream fileOutputStream = new FileOutputStream(fullfilepath);
		    byte[] byteArray = Base64.getDecoder().decode(pdf.getContent());
		    fileOutputStream.write(byteArray);
		    fileOutputStream.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
		return success;
	}
	
	
	public static String inputSource2String (InputSource is) {
		String xml="";
		try{
			Reader r=is.getCharacterStream();
			r.reset(); // Ensure to read the complete String
			StringBuilder b=new StringBuilder();
			int c;
			while((c=r.read())>-1)
				b.appendCodePoint(c);
			r.reset(); // Reset for possible further actions
			xml=b.toString();
			
		}catch (Exception e) {
			System.err.println("MSMUtils::inputSource2Sting -> Error when reading inputSource"+
					"\n"+e.toString()+
					"\n"+e.getStackTrace().toString());
			
		}
		return xml;
	}
	
	/**
	 * Unescape \\uxxxx like chars in a json string. String to convert must be a valid json string
	 * 
	 * @param s
	 * @param emit_unicode
	 * @return
	 */
	public static String JSONcleanEntitiesUnicode(String s)
	{
		String result = Parser.unescapeEntities(s, false).replaceAll("\\p{Pd}", "-");
		for (Pattern reg: unicodeConversions.keySet()) {
			String rep = unicodeConversions.get(reg);
			result = reg.matcher(result).replaceAll(rep);//result.rereplaceAll(reg, rep);
		}
		return result;
	}
	
	/**
	 * This void creates Patterns for a list of regexes and stores them in a set structure.
	 * @param patterns
	 */
	public static Set<Pattern> constructPatterns(String[] patterns) {
	
		Set<Pattern> result = new HashSet<Pattern>();
		if (patterns.length < 1)
		{
			System.err.println ("feedReader-elh::Utils - No pattern given");
			return result;
			//System.exit(1);
		}

		for (String s : patterns)
		{
			//create and store pattern;
			Pattern p=Pattern.compile(s);//.toLowerCase());
			result.add(p);
		} 
		return result;
	}
}
