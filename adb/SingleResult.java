package adb;

/**
 * Class SingerResult: Contains url, title and summary of one document.
 * Also contains the relevance feedback from the user.
 * Package: adb
 * Advanced Database Systems Project 1
 * Group 3
 * Ziyi Luo (zl2471) & Guihao Liang (gl2520)
 */
public class SingleResult {
	private String url;
	private String title;
	private String summary;
	private boolean isRelevant;
	public SingleResult(String url, String title, String summary) {
		this.url = url;
		this.title = title;
		this.summary = summary;
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getSummary() {
		return summary;
	}
	
	@Override
	public String toString() {
		return "[\n URL: " + url + "\n Title: " + title + "\n Summary: " + summary + "\n]"; 
	}

	public boolean getIsRelevant() {
		return isRelevant;
	}

	public void setIsRelevant(boolean isRelevant) {
		this.isRelevant = isRelevant;
	}
}
