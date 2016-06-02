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
package org.apache.usergrid.apm.rest;

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
