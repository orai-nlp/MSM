package elh.eus.MSM;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.apache.commons.validator.routines.UrlValidator;

public class Source {

	private long id;	
	private String screenName;
	private String type;
	private String domain;
	private double influence;
	
	private final Pattern httpProt = Pattern.compile("^[hf]t?tps?://", Pattern.CASE_INSENSITIVE);


	public long getId() {
		return id;
	}
	
	public void setId(long id) {
		this.id = id;
	}
	
	public String getScreenName() {
		return screenName;
	}
	
	public void setScreenName(String screenName) {
		this.screenName = screenName;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	
	public double getInfluence() {
		return influence;
	}

	public void setInfluence(double influence) {
		this.influence = influence;
	}
	
	
	public Source(String src){
		//URL normalization
		UrlValidator defaultValidator = new UrlValidator(UrlValidator.ALLOW_ALL_SCHEMES);
		
		//add protocol since defaultvalidator wont't work without it.
		String check = src;
		if (!httpProt.matcher(check).find())
		{
			check ="http://"+src;
		}
		//System.err.println("Source: new source: "+check);
		if (defaultValidator.isValid(check)) 
		{
			//System.err.println("Source: new web source: "+src);
			setDomain(check);
			setScreenName(src);			
			setType("feed");
		}
		else
		{
			//System.err.println("Source: new twitter source: "+src);			
			setScreenName(src);
			setType("twitter");
		}
	}
	
	public Source(long id, String screenName, String type, String domain,double inf){
		setId(id);
		setScreenName(screenName);
		setType(type);
		setDomain(domain);
		setInfluence(inf);
	}
	
	/**
	 * Retrieve sources from database. Normally in order to launch a crawler or search engine queries
	 * 
	 * @param conn
	 * @param type (twitter|press|behagune)
	 * @return
	 * @throws NamingException
	 * @throws SQLException
	 */
	public static Set<Source> retrieveFromDB(Connection conn, String type, String opt) throws NamingException, SQLException {

		Set<Source> result = new HashSet<Source>(); 
		Statement stmt = conn.createStatement();
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM behagunea_app_source");
		switch (opt)
		{
		case "null": sb.append(" where influence is NULL or influence='NULL' or influence=''");break;
		case "error":  sb.append(" where influence=-1");break;
		case "all":  break;
		}
		
		String andWhere = " where ";
		if (sb.toString().contains(" where "))
		{
			andWhere=" and ";
		}
		
		if (type.equalsIgnoreCase("twitter"))
		{			
			sb.append(andWhere).append("type='Twitter'");
		}
		else if (type.equalsIgnoreCase("feed"))
		{
			sb.append(andWhere).append("type='press'");
		}
		
		//limited to 500 sources per call not to exceed rate limit.
		String query = sb.append(" limit 500").toString();
		System.err.println("elh-MSM::Keyword::retrieveFromDB - query:"+query);
		ResultSet rs = stmt.executeQuery(query);		
		
		try{	
			while (rs.next()) {
				Source src = new Source(rs.getLong("source_id"), rs.getString("source_name"),rs.getString("type"),rs.getString("domain"),rs.getDouble("influence"));				
				result.add(src);
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
	
}
