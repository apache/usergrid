package org.apache.usergrid.apm.util;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import org.apache.usergrid.apm.service.Device;

public class MobileSession {
	String sessionId;
	
	Device device;
	
	Date startTime;
	
	Date endTime;
	
	public MobileSession () {
		sessionId = UUID.randomUUID().toString();
		Calendar cal = Calendar.getInstance(/*TimeZone.getTimeZone("GMT")*/); //EC2 is GMT by default.
		int random = new Random ().nextInt(60);
		cal.add(Calendar.SECOND, random * -1);
		startTime = cal.getTime();
		endTime = ((Calendar) cal.clone()).getTime();
	}	

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	
	
	
	

}
