package elh.eus.MSM;

import java.util.Date;
import java.util.Set;

import com.rometools.rome.feed.synd.SyndEntry;

public class FeedArticle {

	
	private String title; 
	private String description;
	private String text;
	private Set<String> imgs;
	private String url;
	private String lang;
	private String author;
	private String categories;
	private Date pubDate;
	private SyndEntry feedEntry; 

	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public Set<String> getImages() {
		return imgs;
	}
	public void setImages(Set<String> imgs) {
		this.imgs = imgs;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getLang() {
		return lang;
	}
	public void setLang(String lang) {
		this.lang = lang;
	}
	public Date getPubDate() {
		return pubDate;
	}
	public void setPubDate(Date pubDate) {
		this.pubDate = pubDate;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author1) {
		this.author = author1;
	}
	public String getCategories() {
		return categories;
	}
	public void setCategories(String cats) {
		this.categories = cats;
	}
	public SyndEntry getFeedEntry() {
		return feedEntry;
	}
	public void setFeedEntry(SyndEntry feedEntry) {
		this.feedEntry = feedEntry;
	}


	/**
	 * Constructor;
	 */
	public FeedArticle () {
	}
	
	
}
