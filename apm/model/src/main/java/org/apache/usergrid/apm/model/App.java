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
package org.apache.usergrid.apm.model;

import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


/**
 * 
 * This class represented an enhanced version of the
 * ApplicationConfigurationModel to support 3 additional use cases:
 * 
 * 1. Enabling Device level configuration overrides 2. Enabling Device Type
 * configuration overrides 3. Enabling A/B Testing for configuration overrides
 * 
 * Device level configuration overrides enable the developer to override 1 or
 * more devices using a specific configuration. This is helpful when the user is
 * doing testing, or if there is a particular customer complaint.
 * 
 * Device Type Configuration Overrides enables the developer to override
 * multiple devices based on the type of phone it is, network it is on, etc.
 * This is helpful the developer identifies a large swath of devices that could
 * potentially have issues.
 * 
 * A/B Testing over-rides enable the developers to incrementally roll out
 * features to a percentage of the population. This essentially allows for
 * developers to "test" to see if a configuration performs better than another.
 * 
 * Logic for implementing overrides:
 * 
 * Configuration logic gets implemented using a "heirachy". It should look
 * something like this:
 * 
 * 1. If Device-Level Overrides is enabled && Phone matches device filter, then
 * use Device-level AppConfig 2. Else, If Device-Type Overrides is enabled &&
 * Phone matches device filter, then Device-type AppConfig 3. Else, If A/B Test
 * Overrides is enabled && Random % < "B Percentage", then use A/B Testing
 * AppConfig 4. Else, use default AppConfig
 * 
 * Tradeoffs:
 * 
 * The question might be - why only use 4 levels ? What if we want to mix and
 * match ? The reason is "real world use cases". Instead of having 100s of
 * combinations that will never get used or will never see the light of day, its
 * important to not over-complicate. You will also notice, there is no explcit
 * "versioning" either. This is more of a "tool" to get developers out of tough
 * spots and not a general all purpose configuration system.
 * 
 * @author alanho
 * @author prabhat
 * 
 */


@Entity
@Table(name = "APPLICATIONS", uniqueConstraints=@UniqueConstraint(columnNames="fullAppName"))
@org.hibernate.annotations.Table(appliesTo="APPLICATIONS",
		indexes = {
		@Index(name="OrgAppName", columnNames={"fullAppName"} ),
		@Index(name="Created_Date", columnNames={"createdDate"} )  
} )

public class App implements Serializable
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	Long instaOpsApplicationId; //this is legacy id from InstaOps days.
	
	String applicationUUID;
	
	String organizationUUID;
	
	String orgName;
	
	String appName;
	
	String fullAppName; //it's of form orgName_appName
	
	String appOwner;	
	
	Date createdDate = new Date();

	Date lastModifiedDate = new Date();

	Boolean monitoringDisabled = false;

	Boolean deleted = false;

	
	@Transient
	String googleId;
	
	@Transient
	String appleId;
	
	@Transient
	String description;

	String environment ;

	
	@Transient
	String customUploadUrl;
	
	@Transient
    ApplicationConfigurationModel defaultAppConfig;

	@Transient
	Set<AppConfigOverrideFilter> appConfigOverrideFilters = new HashSet<AppConfigOverrideFilter>();
	
	@Transient
    ApplicationConfigurationModel deviceLevelAppConfig;
	
	@Transient
	Boolean deviceLevelOverrideEnabled = false;

	@Transient
    ApplicationConfigurationModel deviceTypeAppConfig;

	@Transient
	Boolean deviceTypeOverrideEnabled = false;

	@Transient
    ApplicationConfigurationModel ABTestingAppConfig;

	@Transient
	Boolean ABTestingOverrideEnabled = false;
	
	@Transient
	Integer ABTestingPercentage = 0;	

	@Transient
	Set<AppConfigOverrideFilter> deviceNumberFilters = new HashSet<AppConfigOverrideFilter>();;

	@Transient
	Set<AppConfigOverrideFilter> deviceIdFilters = new HashSet<AppConfigOverrideFilter>();;

	@Transient
	Set<AppConfigOverrideFilter> deviceModelRegexFilters = new HashSet<AppConfigOverrideFilter>();;

	@Transient
	Set<AppConfigOverrideFilter> devicePlatformRegexFilters = new HashSet<AppConfigOverrideFilter>();;

	@Transient
	Set<AppConfigOverrideFilter> networkTypeRegexFilters = new HashSet<AppConfigOverrideFilter>();;

	@Transient
	Set<AppConfigOverrideFilter> networkOperatorRegexFilters = new HashSet<AppConfigOverrideFilter>();;


	public App()
	{
		this.defaultAppConfig = new ApplicationConfigurationModel(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);
		this.deviceLevelAppConfig = new ApplicationConfigurationModel(ApigeeMobileAPMConstants.CONFIG_TYPE_DEVICE_LEVEL);
		this.deviceTypeAppConfig = new ApplicationConfigurationModel(ApigeeMobileAPMConstants.CONFIG_TYPE_DEVICE_TYPE);
		this.ABTestingAppConfig = new ApplicationConfigurationModel(ApigeeMobileAPMConstants.CONFIG_TYPE_AB);
	}


	public Long getInstaOpsApplicationId() {
		return instaOpsApplicationId;
	}


	public void setInstaOpsApplicationId(Long instaOpsApplicationId) {
		this.instaOpsApplicationId = instaOpsApplicationId;
	}


	public String getApplicationUUID() {
		return applicationUUID;
	}


	public void setApplicationUUID(String applicationUUID) {
		this.applicationUUID = applicationUUID;
	}


	public String getOrganizationUUID() {
		return organizationUUID;
	}


	public void setOrganizationUUID(String organizationUUID) {
		this.organizationUUID = organizationUUID;
	}


	public String getOrgName() {
		return orgName;
	}


	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}


	public String getAppName() {
		return appName;
	}


	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getFullAppName() {
		if (this.fullAppName == null && orgName !=null && appName != null) {
			this.fullAppName = orgName + "_" + appName;
			this.fullAppName = fullAppName.replaceAll("[^a-zA-Z0-9_-]", "");//only allow alpha numeric, _ and -. Remove other characters
			this.fullAppName = fullAppName.replaceAll("[^a-zA-Z0-9_-]", ""); //only allow alpha numeric, _ and -. Remove other characters
			this.fullAppName = this.fullAppName.toLowerCase();
		}
		return fullAppName;
	}


	public void setFullAppName(String fullAppName) {
		if (fullAppName != null) {
			this.fullAppName = fullAppName.replaceAll("[^a-zA-Z0-9_-]", ""); //only allow alpha numeric, _ and -. Remove other characters
			this.fullAppName = this.fullAppName.toLowerCase();
		}
	}


	public String getAppOwner() {
		return appOwner;
	}


	public void setAppOwner(String appOwner) {
		this.appOwner = appOwner;
	}


	public String getGoogleId() {
		return googleId;
	}


	public void setGoogleId(String googleId) {
		this.googleId = googleId;
	}


	public String getAppleId() {
		return appleId;
	}


	public void setAppleId(String appleId) {
		this.appleId = appleId;
	}


	public String getDescription() {
		return description;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public String getEnvironment() {
		return environment;
	}


	public void setEnvironment(String environment) {
		this.environment = environment;
	}


	public Date getCreatedDate() {
		return createdDate;
	}


	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}


	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}


	public void setLastModifiedDate(Date lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}


	public Boolean getMonitoringDisabled() {
		return monitoringDisabled;
	}


	public void setMonitoringDisabled(Boolean monitoringDisabled) {
		this.monitoringDisabled = monitoringDisabled;
	}


	public Boolean getDeleted() {
		return deleted;
	}


	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}


	public String getCustomUploadUrl() {
		return customUploadUrl;
	}


	public void setCustomUploadUrl(String customUploadUrl) {
		this.customUploadUrl = customUploadUrl;
	}


	public ApplicationConfigurationModel getDefaultAppConfig() {
		return defaultAppConfig;
	}


	public void setDefaultAppConfig(ApplicationConfigurationModel defaultAppConfig) {
		this.defaultAppConfig = defaultAppConfig;
	}


	public Set<AppConfigOverrideFilter> getAppConfigOverrideFilters() {
		return appConfigOverrideFilters;
	}


	public void setAppConfigOverrideFilters(
			Set<AppConfigOverrideFilter> appConfigOverrideFilters) {
		this.appConfigOverrideFilters = appConfigOverrideFilters;
	}


	public ApplicationConfigurationModel getDeviceLevelAppConfig() {
		return deviceLevelAppConfig;
	}


	public void setDeviceLevelAppConfig(
			ApplicationConfigurationModel deviceLevelAppConfig) {
		this.deviceLevelAppConfig = deviceLevelAppConfig;
	}


	public Boolean getDeviceLevelOverrideEnabled() {
		return deviceLevelOverrideEnabled;
	}


	public void setDeviceLevelOverrideEnabled(Boolean deviceLevelOverrideEnabled) {
		this.deviceLevelOverrideEnabled = deviceLevelOverrideEnabled;
	}


	public ApplicationConfigurationModel getDeviceTypeAppConfig() {
		return deviceTypeAppConfig;
	}


	public void setDeviceTypeAppConfig(
			ApplicationConfigurationModel deviceTypeAppConfig) {
		this.deviceTypeAppConfig = deviceTypeAppConfig;
	}


	public Boolean getDeviceTypeOverrideEnabled() {
		return deviceTypeOverrideEnabled;
	}


	public void setDeviceTypeOverrideEnabled(Boolean deviceTypeOverrideEnabled) {
		this.deviceTypeOverrideEnabled = deviceTypeOverrideEnabled;
	}


	public ApplicationConfigurationModel getABTestingAppConfig() {
		return ABTestingAppConfig;
	}


	public void setABTestingAppConfig(
			ApplicationConfigurationModel aBTestingAppConfig) {
		ABTestingAppConfig = aBTestingAppConfig;
	}


	public Boolean getABTestingOverrideEnabled() {
		return ABTestingOverrideEnabled;
	}


	public void setABTestingOverrideEnabled(Boolean aBTestingOverrideEnabled) {
		ABTestingOverrideEnabled = aBTestingOverrideEnabled;
	}


	public Integer getABTestingPercentage() {
		return ABTestingPercentage;
	}


	public void setABTestingPercentage(Integer aBTestingPercentage) {
		ABTestingPercentage = aBTestingPercentage;
	}


	public Set<AppConfigOverrideFilter> getDeviceNumberFilters() {
		return deviceNumberFilters;
	}


	public void setDeviceNumberFilters(
			Set<AppConfigOverrideFilter> deviceNumberFilters) {
		this.deviceNumberFilters = deviceNumberFilters;
	}


	public Set<AppConfigOverrideFilter> getDeviceIdFilters() {
		return deviceIdFilters;
	}


	public void setDeviceIdFilters(Set<AppConfigOverrideFilter> deviceIdFilters) {
		this.deviceIdFilters = deviceIdFilters;
	}


	public Set<AppConfigOverrideFilter> getDeviceModelRegexFilters() {
		return deviceModelRegexFilters;
	}


	public void setDeviceModelRegexFilters(
			Set<AppConfigOverrideFilter> deviceModelRegexFilters) {
		this.deviceModelRegexFilters = deviceModelRegexFilters;
	}


	public Set<AppConfigOverrideFilter> getDevicePlatformRegexFilters() {
		return devicePlatformRegexFilters;
	}


	public void setDevicePlatformRegexFilters(
			Set<AppConfigOverrideFilter> devicePlatformRegexFilters) {
		this.devicePlatformRegexFilters = devicePlatformRegexFilters;
	}


	public Set<AppConfigOverrideFilter> getNetworkTypeRegexFilters() {
		return networkTypeRegexFilters;
	}


	public void setNetworkTypeRegexFilters(
			Set<AppConfigOverrideFilter> networkTypeRegexFilters) {
		this.networkTypeRegexFilters = networkTypeRegexFilters;
	}


	public Set<AppConfigOverrideFilter> getNetworkOperatorRegexFilters() {
		return networkOperatorRegexFilters;
	}


	public void setNetworkOperatorRegexFilters(
			Set<AppConfigOverrideFilter> networkOperatorRegexFilters) {
		this.networkOperatorRegexFilters = networkOperatorRegexFilters;
	}
	
	

	
}
