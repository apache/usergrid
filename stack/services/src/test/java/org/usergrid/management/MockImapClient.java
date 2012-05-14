package org.usergrid.management;

import java.io.IOException;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockImapClient {

	private static final Logger logger = LoggerFactory
			.getLogger(MockImapClient.class);

	String host;
	String user;
	String password;

	public MockImapClient(String host, String user, String password) {
		this.host = host;
		this.user = user;
		this.password = password;
	}

	public void processMail() {
		try {
			Session session = getMailSession();
			Store store = connect(session);
			Folder folder = openMailFolder(store);
			findContent(folder);
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public Session getMailSession() {
		Properties props = System.getProperties();
		props.setProperty("mail.transport.protocol", "smtp");
		props.setProperty("mail.store.protocol", "imap");
		props.setProperty("mail.imap.partialfetch", "0");

		logger.info("Getting session");
		return Session.getDefaultInstance(props, null);

	}

	public Store connect(Session session) throws MessagingException {
		logger.info("getting the session for accessing email.");
		Store store = session.getStore("imap");

		store.connect(host, user, password);
		logger.info("Connection established with IMAP server.");
		return store;
	}

	public Folder openMailFolder(Store store) throws MessagingException {
		Folder folder = store.getDefaultFolder();
		folder = folder.getFolder("inbox");
		folder.open(Folder.READ_ONLY);
		return folder;
	}

	public void findContent(Folder folder) throws MessagingException,
			IOException {
		for (Message m : folder.getMessages()) {
			logger.info("Subject: " + m.getSubject());
		}
	}

}
