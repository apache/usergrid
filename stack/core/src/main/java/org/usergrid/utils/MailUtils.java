/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Core.
 * 
 * Usergrid Core is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Usergrid Core is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Usergrid Core. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.usergrid.utils;

import static org.usergrid.utils.MapUtils.asProperties;
import static org.usergrid.utils.MapUtils.filter;

import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;

public class MailUtils {

	private static final Logger logger = Logger
			.getLogger(ConversionUtils.class);

	public static Properties getMailProperties(Map<String, String> properties) {
		return asProperties(filter(properties, "mail."));
	}

	public static Properties getMailProperties(Properties properties) {
		return filter(properties, "mail.");
	}

	public static String getHtmlMailTo(String email) {
		return "<a href=\"mailto:" + email + "\">";
	}

	public static void sendMail(String to, String from, String subject,
			String content) {
		sendMail(null, to, from, subject, content, null);
	}

	public static void sendMail(Properties props, String to, String from,
			String subject, String plainText, String htmlText) {
		try {
			if (props == null) {
				props = System.getProperties();
			}

			String protocol = props.getProperty("mail.transport.protocol",
					"smtp");
			String host = props.getProperty("mail." + protocol + ".host");
			String username = props.getProperty("mail." + protocol
					+ ".username");
			String password = props.getProperty("mail." + protocol
					+ ".password");

			Session session = Session.getInstance(props);
			// session.setDebug(true);

			MimeMultipart msgContent = new MimeMultipart("alternative");

			if ((htmlText != null) && (plainText == null)) {
				try {
					plainText = Jsoup.parse(htmlText).body()
							.wrap("<pre></pre>").text();
				} catch (Exception e) {
					logger.error(e);
				}
			}

			if (plainText != null) {
				MimeBodyPart plainPart = new MimeBodyPart();
				plainPart.setContent(plainText, "text/plain");
				plainPart.setHeader("MIME-Version", "1.0");
				plainPart.setHeader("Content-Type",
						"text/plain; charset=iso-8859-1");
				msgContent.addBodyPart(plainPart);
			}

			if (htmlText != null) {
				MimeBodyPart htmlPart = new MimeBodyPart();
				htmlPart.setContent(htmlText, "text/html");
				htmlPart.setHeader("MIME-Version", "1.0");
				htmlPart.setHeader("Content-Type",
						"text/html; charset=iso-8859-1");
				msgContent.addBodyPart(htmlPart);
			}

			MimeMessage msg = new MimeMessage(session);
			msg.setContent(msgContent);
			msg.setFrom(new InternetAddress(from));
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
			msg.setSubject(subject);

			Transport transport = session.getTransport();

			transport.connect(host, username, password);

			transport.sendMessage(msg, msg.getAllRecipients());
			transport.close();
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public static void sendHtmlMail(String to, String from, String subject,
			String html) {
		sendMail(null, to, from, subject, null, html);
	}

	public static void sendHtmlMail(Properties props, String to, String from,
			String subject, String html) {
		sendMail(props, to, from, subject, null, html);
	}

}
