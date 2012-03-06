/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
