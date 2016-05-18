package com.ideawheel.portal.util;

import java.net.URI;

public class DomainParsing {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String domain = null;
		String webAddress = null;
		try {
			webAddress = "http://www.cfdasfsddfasdfdafdasfaafsnn.com?xyx=bah";
			if (webAddress.indexOf("?") != -1)
				webAddress = webAddress.substring(0,webAddress.indexOf('?'));
			domain = new URI(webAddress).getHost();
			System.out.println(domain);
		} catch (Exception e) {
			domain = webAddress.substring(0, Math.min(20,webAddress.length()));
		}

	}

}
