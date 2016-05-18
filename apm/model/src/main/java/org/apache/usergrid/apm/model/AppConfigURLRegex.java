package org.apache.usergrid.apm.model;



/**
 * 
 * @author prabhat
 *
 */

public class AppConfigURLRegex {

	
	private String regex;

	public AppConfigURLRegex() {
		// TODO Auto-generated constructor stub
	}

	public AppConfigURLRegex (String regex) {
		this.regex = regex;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	@Override
	public String toString() {
		return "Regex  is " + regex;
	}

}
