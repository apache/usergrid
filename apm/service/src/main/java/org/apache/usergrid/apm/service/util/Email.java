package org.apache.usergrid.apm.service.util;

public class Email {
	String recipient;
	String messageBody;
	String subject;
	
	public Email (String subject, String body, String recipient) {
		this.recipient = recipient;
		this.messageBody = body;
		this.subject = subject;
	}
	
	public String getRecipient() {
		return recipient;
	}
	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}
	public String getMessageBody() {
		return messageBody;
	}
	public void setMessageBody(String messageBody) {
		this.messageBody = messageBody;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}	
	
	

}
