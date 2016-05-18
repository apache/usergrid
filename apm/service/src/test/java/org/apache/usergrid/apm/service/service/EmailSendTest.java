package org.apache.usergrid.apm.service.service;

import java.util.Date;

import org.apache.usergrid.apm.service.util.AsyncMailer;
import org.apache.usergrid.apm.service.util.Email;
import org.apache.usergrid.apm.service.util.Mailer;

import junit.framework.TestCase;

public class EmailSendTest extends TestCase {

	public void testSimpleEmailSending() {

		Mailer.send("prabhat@apigee.com", "junit test email", "testing hard core synch");

	}

	public void testAsyncEmailSending() {

		AsyncMailer.send(new Email("async tst email", "testign hard core asynch", "prabhat@apigee.com" ));
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println ("done");

	}
	
	public void testReturnCarriage() {

		String newLine = System.getProperty("line.separator");
		
		AsyncMailer.send(new Email("async tst email", "Test \t\n <br /> Return Carriage", "alan@apigee.com" ));
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println ("done");

	}
	
	public void testDate() {
		Date d = new Date();
		System.out.println("Date is " + d.toString());
		System.out.println("Date in millisecons " + d.getTime());
	}

}
