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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;

import org.apache.commons.validator.routines.UrlValidator;
import org.jdom2.Element;
import org.jdom2.JDOMException;

public class multimediaElement {

	private int channelId;
	private int feedId;
	private String episode;
	private String desc;
	private String originURL;
	private String mediaURL;
	private String transcriptionURL;
	private String mediaType;
	private Date emisionDate;
	private Date downloadDate;
	private String showURL;

	
	public int getChannelId() {
		return channelId;
	}

	public void setChannelId(int channelId) {
		this.channelId = channelId;
	}


	public int getFeedId() {
		return feedId;
	}

	public void setFeedId(int feedId) {
		this.feedId = feedId;
	}

	public String getEpisode() {
		return episode;
	}

	public void setEpisode(String episode) {
		this.episode = episode;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getOriginURL() {
		return originURL;
	}

	public void setOriginURL(String originURL) {
		this.originURL = originURL;
	}

	public String getMediaURL() {
		return mediaURL;
	}

	public void setMediaURL(String mediaURL) {
		this.mediaURL = mediaURL;
	}

	public String getTranscriptionURL() {
		return transcriptionURL;
	}

	public void setTranscriptionURL(String transcriptionURL) {
		this.transcriptionURL = transcriptionURL;
	}

	public String getMediaType() {
		return mediaType;
	}

	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	public Date getEmisionDate() {
		return emisionDate;
	}

	public void setEmisionDate(Date emisionDate) {
		this.emisionDate = emisionDate;
	}

	public Date getDownloadDate() {
		return downloadDate;
	}

	public void setDownloadDate(Date downloadDate) {
		this.downloadDate = downloadDate;
	}

	public String getShowURL() {
		return showURL;
	}

	public void setShowURL(String showURL) {
		this.showURL = showURL;
	}

	
	
	/**
	 * @param show
	 * @throws JDOMException
	 */
	public multimediaElement(Element show) throws JDOMException{
	
		
		setChannelId(Integer.valueOf(show.getChild("kanala").getAttributeValue("id")));
		setFeedId(Integer.valueOf(show.getChild("programa").getAttributeValue("id")));
		setEpisode(show.getChildText("atala"));
		setDesc(show.getChildText("deskribapena"));
		setOriginURL(show.getChildText("url"));
		setMediaURL(show.getChildText("media-url"));
		setTranscriptionURL(show.getChildText("trans-url"));
		setMediaType(show.getAttributeValue("media-mota"));
		Date emision_date = Utils.parseDate(show.getAttributeValue("emisio-data"));
		Date download_date = Utils.parseDate(show.getAttributeValue("deskarga-data"));
			
		setEmisionDate(emision_date);
		setDownloadDate(download_date);
		
		
		//URL normalization
		/*UrlValidator defaultValidator = new UrlValidator(UrlValidator.ALLOW_ALL_SCHEMES);
				
		if (defaultValidator.isValid(src)) 
		{
			setFeedURL(src);
			setLastFetchDate("1950-01-01 00:00:00 +0000");
		}
		else
		{
			System.err.println("elh-MSM::Feed (constructor) - given feed url is not valid "+src);
			System.exit(1);
		}*/
	}
	
	
	
}
