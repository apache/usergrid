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

import java.util.Properties;

import org.junit.Ignore;

@Ignore
public class MailUtilsTest {

	public static void main(String[] args) throws Exception {
		MailUtilsTest test = new MailUtilsTest();
		test.testSendMail();
	}

	public void testSendMail() {

		Properties properties = new Properties();
		properties.setProperty("mail.transport.protocol", "smtps");
		properties.setProperty("mail.smtps.host", "smtp.gmail.com");
		properties.setProperty("mail.smtps.port", "465");
		properties.setProperty("mail.smtps.auth", "true");
		properties.setProperty("mail.smtps.username", "mailer@usergrid.com");
		properties.setProperty("mail.smtps.password", "ugmail123$");
		properties.setProperty("mail.smtps.quitwait", "false");

		MailUtils
				.sendHtmlMail(
						properties,
						"Ed Anuff <ed@anuff.com>",
						"Usergrid Mailer <mailer@usergrid.com>",
						"test",
						"this is a test, click here <a href=\"http://www.usergrid.com\">http://www.usergrid.com</a>");
	}
}
