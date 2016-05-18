package org.apache.usergrid.apm.service.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class Mailer {

	private static final Log log = LogFactory.getLog(Mailer.class);
	public static void send(String recipeintEmail, String subject, String messageText)  {
		/*
		 * It is a good practice to put this in a java.util.Properties file and
		 * encrypt password. Scroll down to comments below to see how to use
		 * java.util.Properties in JSF context.
		 */
		Properties props = new Properties();
		try {
			props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("conf/email.properties"));
		} catch (IOException e) {			
			e.printStackTrace();
		}

		final String senderEmail = props.getProperty("mail.smtp.sender.email");
		final String smtpUser = props.getProperty("mail.smtp.user");
		final String senderName = props.getProperty("mail.smtp.sender.name");
		final String senderPassword = props.getProperty("senderPassword");
        final String emailtoCC = props.getProperty("instaopsOpsEmailtoCC");

		Session session = Session.getDefaultInstance(props, new Authenticator() {
			@Override
			public PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(smtpUser, senderPassword);
			}
		});
		session.setDebug(false);

		try {
			MimeMessage message = new MimeMessage(session);

			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(messageText, "text/html");

			// Add message text
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);

			message.setContent(multipart);
			message.setSubject(subject);
			InternetAddress senderAddress = new InternetAddress(senderEmail, senderName);
			message.setFrom(senderAddress);
			message.addRecipient(Message.RecipientType.CC, new InternetAddress(emailtoCC));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipeintEmail));
			Transport.send(message);
			log.info("email sent");
		} catch (MessagingException m) {
			log.error(m.toString());
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
