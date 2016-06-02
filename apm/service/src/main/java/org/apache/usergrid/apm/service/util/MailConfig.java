/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.usergrid.apm.service.util;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MailConfig {
	private static final Log log = LogFactory.getLog(MailConfig.class);
	String senderEmail;
	String smtpUser;
	String senderName;
	String senderPassword;
	String emailtoCC;
	String adminUsers;
	private static MailConfig mailConfig = null;
	
	private MailConfig() {
		
	}

	public static MailConfig getMailConfig() {
		if (mailConfig != null)
			return mailConfig;

		Properties props = new Properties();
		try {
			props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("conf/email.properties"));
		} catch (IOException e) {			
			e.printStackTrace();
		}

		String senderEmail = props.getProperty("mail.smtp.sender.email");
		String smtpUser = props.getProperty("mail.smtp.user");
		String senderName = props.getProperty("mail.smtp.sender.name");
		String senderPassword = props.getProperty("senderPassword");
		String emailtoCC = props.getProperty("instaopsOpsEmailtoCC");
		String adminUsers = props.getProperty("adminUsers");
		
		mailConfig = new MailConfig();
		mailConfig.senderEmail = senderEmail;
		mailConfig.smtpUser = smtpUser;
		mailConfig.senderName = senderName;
		mailConfig.senderPassword = senderPassword;
		mailConfig.emailtoCC = emailtoCC;
		mailConfig.adminUsers = adminUsers;
		
		return mailConfig;
			
	}

	public String getSenderEmail() {
		return senderEmail;
	}

	public void setSenderEmail(String senderEmail) {
		this.senderEmail = senderEmail;
	}

	public String getSmtpUser() {
		return smtpUser;
	}

	public void setSmtpUser(String smtpUser) {
		this.smtpUser = smtpUser;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}

	public String getSenderPassword() {
		return senderPassword;
	}

	public void setSenderPassword(String senderPassword) {
		this.senderPassword = senderPassword;
	}

	public String getEmailtoCC() {
		return emailtoCC;
	}

	public void setEmailtoCC(String emailtoCC) {
		this.emailtoCC = emailtoCC;
	}

	public String getAdminUsers() {
		return adminUsers;
	}

	public void setAdminUsers(String adminUsers) {
		this.adminUsers = adminUsers;
	}

}
