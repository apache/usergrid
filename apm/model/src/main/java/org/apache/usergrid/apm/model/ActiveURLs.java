package org.apache.usergrid.apm.model;

import java.io.Serializable;

public class ActiveURLs implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String regexUrl;
	private Long count = 0L;
	private int rank;
	
	public ActiveURLs(String regexUrl)
	{
		this.regexUrl = regexUrl;
	}
	
	public String getRegexUrl() {
		return regexUrl;
	}
	public void setRegexUrl(String regexUrl) {
		this.regexUrl = regexUrl;
	}
	public Long getCount() {
		return count;
	}
	public void setCount(Long count) {
		this.count = count;
	}
	public int getRank() {
		return rank;
	}
	public void setRank(int rank) {
		this.rank = rank;
	}
	
}
