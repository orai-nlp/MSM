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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;


public final class multimediaElement {

	private long channelId;
	private long feedId;
	private String episode;
	private String desc;
	private String originURL;
	private String mediaURL;
	private String transcriptionURL;
	private String mediaType;
	private Date emisionDate;
	private Date downloadDate;
	private String showURL;
	private String lang;

	private String ffmpeg;
	

	
	public long getChannelId() {
		return channelId;
	}

	public void setChannelId(int channelId) {
		this.channelId = channelId;
	}


	public long getFeedId() {
		return feedId;
	}

	public void setFeedId(long feedId) {
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

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}
	
	public void setFFmpeg(String ffmpeg) {
		this.ffmpeg = ffmpeg;
	}

	// END of getters and setters
	
	/**
	 * Word class is used to store word xml element to ease the search of multiword keywords.
	 * 
	 * */
	private class Word {
		float start;
		float end;
		float conf; //conf is meant to store a confidence value given by the transcription engine. At the moment is is not used
		String form;
		
		Word(float s, float e, float c, String f){
			start=s;
			end=e;
			conf=c;
			form=f;
		}				
	}
	
	
	/**
	 * @param show
	 * @throws JDOMException
	 */
	public multimediaElement(Element show, String ffmpeg) throws JDOMException{
	
		
		setChannelId(Integer.valueOf(show.getChild("kanala").getAttributeValue("id")));
		setFeedId(Integer.valueOf(show.getChild("programa").getAttributeValue("id")));
		setEpisode(show.getChildText("atala"));
		setDesc(show.getChildText("deskribapena"));
		setOriginURL(show.getChildText("url"));
		setMediaURL(show.getChildText("media-url"));
		setTranscriptionURL(show.getChildText("trans-url"));
		setMediaType(show.getAttributeValue("media-mota"));
		Date emision_date = MSMUtils.parseDate(show.getAttributeValue("emisio-data"));
		Date download_date = MSMUtils.parseDate(show.getAttributeValue("deskarga-data"));
		
		//System.err.println("MSM::MultimediaElement - constructor - emision-data:"+emision_date);
		setEmisionDate(emision_date);
		setDownloadDate(download_date);
		
		setLang(show.getAttributeValue("hizkuntza"));
		
		setFFmpeg(ffmpeg);
		//URL normalization
		/*UrlValidator defaultValidator = new UrlValidator(UrlValidator.ALLOW_ALL_SCHEMES);
				
		if (defaultValidator.isValid(src)) 
		{
			setFeedURL(src);
			setLastFetchDate("1950-01-01 00:00:00 +0000");
		}
		else
		{
			System.err.println("MSM::Feed (constructor) - given feed url is not valid "+src);
			System.exit(1);
		}*/
	}


	/**
	 * 
	 *    Function search for keywords in the transcription of a multimedia file (using the audio stream).
	 *    If a keyword is found a split of size specified by the user (default is 10 seconds) is cut around 
	 *    the keyword found, and stored as a mention.  
	 *    
	 * @param kwrds
	 * @param kwrdPatterns
	 * @param indepKwrds
	 * @param depKwords
	 * @param anchors
	 * @param anchorWindow
	 * @param store
	 * @param dbconn
	 * @param splitDuration : float, defines in seconds the duration of the video split around keyword.

	 * @throws JDOMException
	 * @throws IOException
	 */
	public void parseForKeywords(Set<Keyword> kwrds, HashMap<Integer,Pattern> kwrdPatterns, Set<Keyword> indepKwrds, Set<Keyword> depKwords, Pattern anchors, float anchorWindow, String store, Connection dbconn, float splitDuration, String tableprefix) throws JDOMException, IOException{
		
		// this variable defines the step (in second) to move the split window forward.
		float step = splitDuration/2;
		
		// create a temporal mkv file
		String converted = getMediaURL();//.replaceFirst("\\.[^\\.]+$",".mp4");

		System.err.println("MSM::MultimediaElement::parseForKeywords - converted file path: "+converted);
						
		//this.convertStream(getMediaURL(), converted);
		
		System.err.println("MSM::MultimediaElement::parseForKeywords - stream "+getMediaURL()
				+" converted to "+converted);
	
		//xml parser to parse the transcription
		SAXBuilder sax = new SAXBuilder();
		XPathFactory xFactory = XPathFactory.instance();
		
		InputStream stream;
		try {
			stream = new FileInputStream(getTranscriptionURL());	
		} catch (FileNotFoundException fe){
			stream = new FileInputStream(StringUtils.stripAccents(getTranscriptionURL()));
		}		
		Document transcription = sax.build(stream);
		//This xpath extracts all the words in a transcription. 
		//NOTE: This is a simplification of the structure,
		//      both "turn" (speaker changes) and lang (language changes) elements are ignored.
		XPathExpression<Element> expr = xFactory.compile("//word",
				Filters.element());
		List<Element> buffer = expr.evaluate(transcription);
		List<Word> transWords = new ArrayList<Word>();
		for (Element w : buffer) {
			transWords.add(new Word(
						Float.valueOf(w.getAttributeValue("start")),
						Float.valueOf(w.getAttributeValue("end")),
						Float.valueOf(w.getAttributeValue("conf")),
						w.getText()));
		}

		float transcriptionEnd = transWords.get(transWords.size()-1).end;	
		float wstart=0;
		float wend=wstart+anchorWindow;
		
		// anchor patterns are searched in a window determined by the parameter anchorwindow (in seconds)
		while (wstart < transcriptionEnd)
		{
			System.err.println("MSM::MultimediaElement::parseForKeywords - anchor window: "
					+String.valueOf(wstart)+" - "+String.valueOf(wend));
			Set<Keyword> result = new HashSet<Keyword>();
		
			String anchorText = StringUtils.stripAccents(windowWords(transWords, wstart, wend).toLowerCase()); 
			boolean anchorFound = anchors.matcher(anchorText).find();
		//System.err.println("MSM::FeedReader::parseArticleForKeywords - anchorPattern: "+anchorPattern.toString()
		//		+"\n -- found? "+anchorFound+" lang: "+lang+" indep/dep:"+independentkwrds.size()+"/"+dependentkwrds.size());
			
			float splitStart = wstart; 
			float splitEnd = splitStart+splitDuration;
			
			while (splitStart < wend)
			{
			    System.err.println("MSM::MultimediaElement::parseForKeywords - split window: "
						+String.valueOf(splitStart)+" - "+String.valueOf(splitEnd));
				
				result = new HashSet<Keyword>();
				// this set controls that two keywords belonging to the same screen tag won't be assigned to a mention 
				Set<String> screenTagList = new HashSet<String>();
			
				// capitalization must be respected in order to accept keywords found in paragraphs.
				// as of 2016/08/03 press mentions have to respect keyword capitalization. 
				String splitText = windowWords(transWords, splitStart, splitEnd); 	
				String searchText = StringUtils.stripAccents(splitText); 
				String searchTextLC = StringUtils.stripAccents(splitText).toLowerCase();
				//keywords that do not need any anchor
				for (Keyword k : indepKwrds)
				{	
					//check if a keyword with the same tag has been already matched, if so do not check the key. 
					if (screenTagList.contains(k.getScreenTag()))
					{
						System.err.println("MSM::MultimediaElement::parseForKeywords - indpndnt keyword found,"
								+ " but not stored because another key was matched with the same screen tag: "+k.getText());
						continue;
					}
					boolean kwrdFound = false;
					//check if keywords are found in the sentence
					// case sensitive search
					if (k.getCaseSensitiveSearch())
					{
						kwrdFound = kwrdPatterns.get(k.getId()).matcher(searchText).find();
					}
					// case insensitive search
					else
					{
						kwrdFound = kwrdPatterns.get(k.getId()).matcher(searchTextLC).find();
					}

					//System.err.println("MSM::FeedReader::parseArticleForKeywords - independent key:"
					//	+k.getText()+" l="+k.getLang()+" pattern:"+kwrdPatterns.get(k.getId()).toString());
					if(k.getLang().equalsIgnoreCase(lang) && kwrdFound)
					{	
						//System.err.println("MSM::FeedReader::parseArticleForKeywords - independent key found!!!: "+k.getText()+" id: "+k.getId());
						result.add(k);
						screenTagList.add(k.getScreenTag());
					}								
				}			
				//keywords that need and anchor, only if anchors where found
				if (anchorFound)
				{
					for (Keyword k : depKwords)
					{
						//check if a keyword with the same tag has been already matched, if so do not check the key. 
						if (screenTagList.contains(k.getScreenTag()))
						{
							System.err.println("MSM::MultimediaElement::parseForKeywords - dpndnt keyword found,"
									+ " but not stored because another key was matched with the same screen tag: "+k.getText());
							continue;
						}

						boolean kwrdFound = false;
						//check if keywords are found in the sentence
						// case sensitive search
						if (k.getCaseSensitiveSearch())
						{
							kwrdFound = kwrdPatterns.get(k.getId()).matcher(searchText).find();
						}
						// case insensitive search
						else
						{
							kwrdFound = kwrdPatterns.get(k.getId()).matcher(searchTextLC).find();
						}

						if (k.getLang().equalsIgnoreCase(lang) && kwrdFound)
						{
							//System.err.println("MSM::FeedReader::parseArticleForKeywords - dependent key found!!!: "+k.getText()+" id: "+k.getId());						
							result.add(k);
							screenTagList.add(k.getScreenTag());
						}					
					}
				}

				if (result != null && !result.isEmpty())
				{
					// cut the video and get the split containing the current mention
					String splitPath = getMentionSplit(converted, splitStart,(long)splitDuration);
					// generate subtitles for the split
					createSplitSubtitles(transWords, splitStart, splitEnd, splitPath,result,kwrdPatterns);
					String splitURL = Paths.get(splitPath).getFileName().toString();
					
					//compute mention date based on the emision date of the programa and the starting offset of the split.
					Calendar cal = Calendar.getInstance();
					cal.setTime(getEmisionDate());
					cal.add(Calendar.SECOND, Math.round(splitStart));
					Date mentionDate=cal.getTime();
					
					//BE CAREFUL: isLocal is hardcoded to true
					Mention m = new Mention(this.getLang(),splitText,mentionDate,this.getOriginURL(),this.getChannelId(),true,String.valueOf(splitStart),splitURL,this.getFeedId());
					m.setKeywords(result);
					if (store.equalsIgnoreCase("db"))
					{
						m.mention2db(dbconn, tableprefix);
						System.err.println("MSM::FeedReader::parseArticleForKeywords - mention2db: "+splitText);
					}
					else
					{
						System.out.println("MSM::FeedReader::parseArticleForKeywords - mention found!: "+splitText);
						m.print();		
					}
				}
				
				//update window
				splitStart = splitStart+step;
				splitEnd = splitStart+splitDuration;
				if (splitEnd > wend)
				{
					splitEnd=wend;
				}
			}//while - split
			//update anchor window
			wstart=wend;
			wend=wstart+anchorWindow;
			if (wend > transcriptionEnd)
			{
				wend=transcriptionEnd;
			}
		}//while - anchor window
								
	}//parseForKeywors
	
	public void convertStream (String mediaUrl, String converted) throws IOException{
		//convert original video to usable format
	    System.err.println("MSM::MultimediaElement::convertStream - "+mediaUrl+" to "+converted);
		FFmpeg ffmpegWrapper = new FFmpeg(this.ffmpeg+File.separator+"ffmpeg");				
		FFprobe ffprobeWrapper = new FFprobe(this.ffmpeg+File.separator+"ffprobe");
		FFmpegProbeResult ffInput = ffprobeWrapper.probe(mediaUrl);

		FFmpegBuilder builder = new FFmpegBuilder()
				.setInput(ffInput)     // Filename, or a FFmpegProbeResult
				.overrideOutputFiles(true) // Override the output if it exists
		    
				.addOutput(converted)   // Filename for the destination
				.setFormat("mp4")        // Format is inferred from filename, or can be set
		    //.setTargetSize(250_000)  // Aim for a 250KB file
		    //.setVideoBitRate(ffInput.getStreams().get(0).bit_rate)
		    .disableSubtitle()       // No subtiles

				.setAudioChannels(1)         // Mono audio
				.setAudioCodec("copy")        // using the aac codec
				.setAudioSampleRate(48_000)  // at 48KHz
				.setAudioBitRate(32768)      // at 32 kbit/s

				.setVideoCodec("copy")     // Video using x264
				.setVideoFrameRate(24, 1)     // at 24 frames per second
				.setVideoResolution(320, 240) // at 640x480 resolution

				.setStrict(FFmpegBuilder.Strict.EXPERIMENTAL) // Allow FFmpeg to use experimental specs
				.done();

		FFmpegExecutor executor = new FFmpegExecutor(ffmpegWrapper, ffprobeWrapper);
		//executor.createTwoPassJob(builder).run();
		executor.createJob(builder).run();

	}
	
	
	private String windowWords(List<Word> wrds, float s, float e){
		StringBuilder sb = new StringBuilder();
		// words are order by time
		for (Word w : wrds){
			//if start time is previous to the given start time ignore the word
			if (w.start<s){
				continue;
			}
			//if the start time is previous to the given end time add the word to the window
			if (w.start<=e){
				sb.append(w.form).append(" ");					
			}
			//the end time is posterior to the given end time. End loop and return the window
			else {
				break;					
			}
		}
		return sb.toString();
	}
	
	/**
	 * @param fullVideoPath
	 * @param splitStart : the second in the original video to start spliting
	 * @param duration: length of the split in seconds
	 * @return
	 * @throws IOException
	 */
	private String getMentionSplit(String fullVideoPath, float splitStart,long duration) throws IOException
	{
		String fileUrl = getMediaURL().replaceFirst("\\.[^\\.]+$", "_"+splitStart+".mp4");
		
		//create ffmpeg wrapper object
		FFmpeg ffmpegWrapper = new FFmpeg(this.ffmpeg+File.separator+"ffmpeg");				
		FFprobe ffprobeWrapper = new FFprobe(this.ffmpeg+File.separator+"ffprobe");
		
		FFmpegBuilder builder = new FFmpegBuilder()
		    .setInput(ffprobeWrapper.probe(fullVideoPath))     // Filename, or a FFmpegProbeResult
				.overrideOutputFiles(true) // Override the output if it exists
				
				.addOutput(fileUrl)   // Filename for the destination
				.setFormat("mp4")        // Format is inferred from filename, or can be set
		    //.setTargetSize(250_000)  // Aim for a 250KB file
				
				.disableSubtitle()       // No subtiles
				
				.setStartOffset((long) splitStart, TimeUnit.SECONDS) //set split start
				.setDuration(duration, TimeUnit.SECONDS) //set split duration
				
				.setAudioChannels(1)         // Mono audio
				.setAudioCodec("aac")        // using the aac codec
				.setAudioSampleRate(48_000)  // at 48KHz
				.setAudioBitRate(32768)      // at 32 kbit/s
				
				.setVideoCodec("libx264")     // Video using x264
				.setVideoCopyInkf(true)    //copy including previous key frames at the begining.
				.setVideoFrameRate(24, 1)     // at 24 frames per second
				.setVideoResolution(320, 240) // at 640x480 resolution
				.setStrict(FFmpegBuilder.Strict.EXPERIMENTAL) // Allow FFmpeg to use experimental specs				
				.done();
		
		FFmpegExecutor executor = new FFmpegExecutor(ffmpegWrapper, ffprobeWrapper);
		executor.createJob(builder).run();
		//executor.createTwoPassJob(builder).run();
		
		return fileUrl;
	}
	
	/**
	 * Function that creates subtitle tracks from mention splits.
	 * 
	 * @param wrds
	 * @param s
	 * @param e
	 * @param url
	 */
	private void createSplitSubtitles(List<Word> wrds, float s, float e, String url, Set<Keyword> kwrds,HashMap<Integer,Pattern> kwrdPatterns){
		String fileUrl = url.replaceFirst("\\.[^\\.]+$", ".vtt");
		// sentTimeInterval is the size of time window where words must be concatenated as a single sentence. 
		float sentTimeInterval = (float) 1.5;		
		StringBuilder sb = new StringBuilder();
		String timeCue = "";
		String text = ""; 
		sb.append("WEBVTT\n\n");
		//shift stores position of the split with respect to the whole video, to adapt the subtitles accordingly.
		int shift = (int)s;
		float interval = s+sentTimeInterval;
		// words are order by time
		for (Word w : wrds){
			//if start time is previous to the given start time ignore the word
			if (w.start<s){
				continue;
			}
			//if the end time previous to the given end time add the word to the window
			if (w.start<=e){
				// the following conditions pack together the words inside a time interval defined by sentTimeInterval
				if (timeCue.equalsIgnoreCase(""))
				{
					timeCue = secToTime(w.start,shift)+" --> ";
					text = w.form.trim()+" ";
					if (w.form.matches(".*[.:;?!]\\s*$"))
					{
						sb.append(timeCue).append(secToTime(w.end, shift))
						.append("\n").append(text).append("\n\n");
						timeCue = "";
						text = "";
						interval=w.end+sentTimeInterval;						
					}					
				}
				else if ((w.start <= interval) && (! w.form.matches(".*[.:;?!]\\s*$")))
				{					
					text += w.form.trim()+" ";
				}
				else  //create subtitle line
				{					
					sb.append(timeCue).append(secToTime(w.end, shift))
					.append("\n").append(text).append(" ").append(w.form.trim()).append("\n\n");
					timeCue = "";
					text = "";
					interval=w.end+sentTimeInterval;
				}
			}
			//the start time is posterior to the given end time. End loop and return the window
			else {
				break;					
			}
		}
		//mark keywords found in the subtitle.			
		String sbs = sb.toString().replaceAll(" +", " ");		
		sb= new StringBuilder(sbs);
		StringBuilder sbFinal = sb;
		for (Keyword k : kwrds){
			Matcher m;
			//check if keywords are found in the sentence
			// case sensitive search
			if (k.getCaseSensitiveSearch())
			{
				m = kwrdPatterns.get(k.getId()).matcher(sb);
				if (m.find())
				{
					sbFinal=new StringBuilder();
					sbFinal.append(sb.subSequence(0, m.start())).append("<b>").append(sb.subSequence(m.start(),m.end())).append("</b>").append(sb.subSequence(m.end(), sb.length()));
					sb=sbFinal;					
				}
			}
			// case insensitive search
			else
			{
				m = kwrdPatterns.get(k.getId()).matcher(sb.toString().toLowerCase());
				if (m.find())
				{
					sbFinal=new StringBuilder();
					sbFinal.append(sb.subSequence(0, m.start())).append("<b>").append(sb.subSequence(m.start(),m.end())).append("</b>").append(sb.subSequence(m.end(), sb.length()));
					sb=sbFinal;
				}
			}
			
		}
		try{
			FileOutputStream out = new FileOutputStream(new File(fileUrl));
			out.write(sbFinal.toString().getBytes());
			out.close();			
		} catch	(IOException ioe){
			System.err.println("MSM::multimediaElement::creatSplitSubtitles - error when writing to file: "+fileUrl);
		}
		
	}
	
	private String secToTime(float sec, int shift) {
	    float seconds = (sec-shift) % 60;
	    return String.format("00:00:%06.3f", seconds);
	}
	
	private String secToTime2(float sec) {
	    float seconds = sec % 60;
	    int minutes = (int)sec / 60;
	    if (minutes >= 60) {
	        int hours = minutes / 60;
	        minutes %= 60;
	        if( hours >= 24) {
	            int days = hours / 24;
	            return String.format("%d days %02d:%02d:%02.3f", days,hours%24, minutes, seconds);
	        }
	        return String.format("00:00:%02.3f", seconds);
	    }
	    return String.format("00:00:%02.3f", seconds);
	}
}
