package com.apigee.apm.rest;

import java.util.Date;
import java.util.UUID;

/**
 * 
 * @author prabhat
 *
 */

public class AppDetailsForAPM {
	
	UUID orgUUID;
	String orgName;
	UUID appUUID;
	String appName;
	Date createdDate;
	String appAdminEmail;
	
	public UUID getOrgUUID() {
		return orgUUID;
	}
	public void setOrgUUID(UUID orgUUID) {
		this.orgUUID = orgUUID;
	}
	public String getOrgName() {
		return orgName;
	}
	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}
	public UUID getAppUUID() {
		return appUUID;
	}
	public void setAppUUID(UUID appUUID) {
		this.appUUID = appUUID;
	}
	public String getAppName() {
		return appName;
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	public String getAppAdminEmail() {
		return appAdminEmail;
	}
	public void setAppAdminEmail(String appAdminEmail) {
		this.appAdminEmail = appAdminEmail;
	}
	
	


}
