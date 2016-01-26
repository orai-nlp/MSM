package elh.eus.MSM;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.commons.validator.routines.UrlValidator;

public class Source {

	private int id;	
	private String screenName;
	private String type;
	private String domain;
	private double influence;
	


	public int getId() {
		return id;
	}
	
	public void setId(int id) {
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
				
		if (defaultValidator.isValid(src)) 
		{
			setDomain(src);
			setScreenName(domain);			
			setType("domain");
		}
		else
		{
			setScreenName(src);
			setType("twitter");
		}
	}
	
	public Source(int id, String screenName, String type, String domain,double inf){
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
	public static Set<Source> retrieveFromDB(Connection conn, String type) throws NamingException, SQLException {

		Set<Source> result = new HashSet<Source>(); 
		Statement stmt = conn.createStatement();
		String typeCondition = "";
		if (type.equalsIgnoreCase("twitter") || type.equalsIgnoreCase("domain"))
		{
			typeCondition ="and type='"+type+"'";
		}
		
		String query = "SELECT * FROM behagunea_app_source where influence='-1' "+typeCondition;
		//System.err.println("elh-MSM::Keyword::retrieveFromDB - query:"+query);
		ResultSet rs = stmt.executeQuery(query);		
		
		try{	
			while (rs.next()) {
				Source src = new Source(rs.getInt("id"), rs.getString("screen_tag"),rs.getString("type"),rs.getString("domain"));				
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
