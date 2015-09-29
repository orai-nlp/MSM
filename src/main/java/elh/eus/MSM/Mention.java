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

import java.util.List;

import twitter4j.JSONObject;
import twitter4j.Status;

public class Mention {

	private int id;
	private int author_id;
	private String text;
	private String url;
	private List<String> keywords;
	private String lang;
	private String polarity;
	
	
	public Mention(JSONObject json) {
		
	}
	
	public Mention(Status statusTwitter4j) {
		
	}
	
	public Mention() {
		
	}

	private void mentionFromTweet() {
		
	}
	
	private void mentionFromFeed() {
		
	}
	
	public int mention2db() {
		return 1;
	}
	
	public int mention2solr(){
		return 1;
	}
}
