package elh.eus.MSM;

public class FeedCredential {

	private String domain;
	private String ssourl;
	private String ssouser;
	private String ssopass;
	private String userField;
	private String passField;
        private String loggedCheckField;
	private String cookieNotice;
	
    public FeedCredential(String domain, String ssourl, String ssouser, String ssopass, String userField, String passField, String loggedFieldCheck, String cookieNotice) {
		this.setDomain(domain);
		this.setSsourl(ssourl);
		this.setSsouser(ssouser);
		this.setSsopass(ssopass);
		this.setUserField(userField);
		this.setPassField(passField);
		this.setLoggedCheckField(loggedFieldCheck);
		this.setCookieNotice(cookieNotice);
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getSsourl() {
		return ssourl;
	}

	public void setSsourl(String ssourl) {
		this.ssourl = ssourl;
	}

	public String getSsouser() {
		return ssouser;
	}

	public void setSsouser(String ssouser) {
		this.ssouser = ssouser;
	}

	public String getSsopass() {
		return ssopass;
	}

	public void setSsopass(String ssopass) {
		this.ssopass = ssopass;
	}

	public String getUserField() {
		return userField;
	}

	public void setUserField(String userField) {
		this.userField = userField;
	}

	public String getPassField() {
		return passField;
	}

	public void setPassField(String passField) {
		this.passField = passField;
	}

	public String getLoggedCheckField() {
		return loggedCheckField;
	}

	public void setLoggedCheckField(String loggedField) {
		this.loggedCheckField = loggedField;
	}

	public String getCookieNotice() {
		return cookieNotice;
	}

	public void setCookieNotice(String cookieNotice) {
		this.cookieNotice = cookieNotice;
	}
	
}
