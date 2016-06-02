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

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


public class ApplicationConfigurationModel implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;  


	Date lastModifiedDate;

	Boolean networkMonitoringEnabled = true;
	
	Boolean sessionDataCaptureEnabled = true;
	
	Boolean enableLogMonitoring = true;  

	Boolean monitorAllUrls = true;

	 
	int logLevelToMonitor = ApigeeMobileAPMConstants.LOG_DEBUG;


	//Fine Grained session data capture control
	Boolean batteryStatusCaptureEnabled = true;
	Boolean IMEICaptureEnabled = true;
	Boolean obfuscateIMEI = true;
	Boolean deviceIdCaptureEnabled = true;
	Boolean obfuscateDeviceId = true;
	Boolean deviceModelCaptureEnabled = true;
	Boolean locationCaptureEnabled = false;
	Long locationCaptureResolution = 1L;
	Boolean networkCarrierCaptureEnabled = true;


	Boolean enableUploadWhenRoaming = false;
	Boolean enableUploadWhenMobile = true; //This means not on wifi
	//by default, the interval at which the agent uploads data to server. Currently 1 minute and stored in DB in milli seconds since everything in
	//DB is in milli seconds.
	Long agentUploadIntervalInSeconds = 60L;

	Long samplingRate = 100L;



	/**
	 * regex but then need to worry about XSS on UI..oh boy ;-)
	 * Examples: *cnn.com.*,.*maps.google.com.*locations=\d{5}
	 */

	private Set<AppConfigURLRegex> urlRegex = new HashSet<AppConfigURLRegex>();


	/**
	 * These are for network caching.
	 */
	Boolean cachingEnabled = false;

	private Set<AppConfigCustomParameter>  customConfigParameters = new HashSet<AppConfigCustomParameter> ();

	private String appConfigType = ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT;


	public ApplicationConfigurationModel () {

	}

	public ApplicationConfigurationModel (String confType) {
		this.appConfigType = confType;
	}

	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}

	public void setLastModifiedDate(Date lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}

	public Boolean getNetworkMonitoringEnabled() {
		return networkMonitoringEnabled;
	}

	public void setNetworkMonitoringEnabled(Boolean networkMonitoringEnabled) {
		this.networkMonitoringEnabled = networkMonitoringEnabled;
	}

	public Boolean getMonitorAllUrls() {
		return monitorAllUrls;
	}

	public void setMonitorAllUrls(Boolean monitorAllUrls) {
		this.monitorAllUrls = monitorAllUrls;
	}

	public Boolean getEnableLogMonitoring() {
		return enableLogMonitoring;
	}

	public void setEnableLogMonitoring(Boolean enableLogMonitoring) {
		this.enableLogMonitoring = enableLogMonitoring;
	}

	public int getLogLevelToMonitor() {
		return logLevelToMonitor;
	}

	public void setLogLevelToMonitor(int logLevelToMonitor) {
		this.logLevelToMonitor = logLevelToMonitor;
	}

	public Boolean getSessionDataCaptureEnabled() {
		return sessionDataCaptureEnabled;
	}

	public void setSessionDataCaptureEnabled(Boolean sessionDataCaptureEnabled) {
		this.sessionDataCaptureEnabled = sessionDataCaptureEnabled;
	}

	public Boolean getBatteryStatusCaptureEnabled() {
		return batteryStatusCaptureEnabled;
	}

	public void setBatteryStatusCaptureEnabled(Boolean batteryStatusCaptureEnabled) {
		this.batteryStatusCaptureEnabled = batteryStatusCaptureEnabled;
	}

	public Boolean getIMEICaptureEnabled() {
		return IMEICaptureEnabled;
	}

	public void setIMEICaptureEnabled(Boolean iMEICaptureEnabled) {
		IMEICaptureEnabled = iMEICaptureEnabled;
	}

	public Boolean getObfuscateIMEI() {
		return obfuscateIMEI;
	}

	public void setObfuscateIMEI(Boolean obfuscateIMEI) {
		this.obfuscateIMEI = obfuscateIMEI;
	}

	public Boolean getDeviceIdCaptureEnabled() {
		return deviceIdCaptureEnabled;
	}

	public void setDeviceIdCaptureEnabled(Boolean deviceIdCaptureEnabled) {
		this.deviceIdCaptureEnabled = deviceIdCaptureEnabled;
	}

	public Boolean getObfuscateDeviceId() {
		return obfuscateDeviceId;
	}

	public void setObfuscateDeviceId(Boolean obfuscateDeviceId) {
		this.obfuscateDeviceId = obfuscateDeviceId;
	}

	public Boolean getDeviceModelCaptureEnabled() {
		return deviceModelCaptureEnabled;
	}

	public void setDeviceModelCaptureEnabled(Boolean deviceModelCaptureEnabled) {
		this.deviceModelCaptureEnabled = deviceModelCaptureEnabled;
	}

	public Boolean getLocationCaptureEnabled() {
		return locationCaptureEnabled;
	}

	public void setLocationCaptureEnabled(Boolean locationCaptureEnabled) {
		this.locationCaptureEnabled = locationCaptureEnabled;
	}

	public Long getLocationCaptureResolution() {
		return locationCaptureResolution;
	}

	public void setLocationCaptureResolution(Long locationCaptureResolution) {
		this.locationCaptureResolution = locationCaptureResolution;
	}

	public Boolean getNetworkCarrierCaptureEnabled() {
		return networkCarrierCaptureEnabled;
	}

	public void setNetworkCarrierCaptureEnabled(Boolean networkCarrierCaptureEnabled) {
		this.networkCarrierCaptureEnabled = networkCarrierCaptureEnabled;
	}

	public Boolean getEnableUploadWhenRoaming() {
		return enableUploadWhenRoaming;
	}

	public void setEnableUploadWhenRoaming(Boolean enableUploadWhenRoaming) {
		this.enableUploadWhenRoaming = enableUploadWhenRoaming;
	}

	public Boolean getEnableUploadWhenMobile() {
		return enableUploadWhenMobile;
	}

	public void setEnableUploadWhenMobile(Boolean enableUploadWhenMobile) {
		this.enableUploadWhenMobile = enableUploadWhenMobile;
	}

	public Long getAgentUploadIntervalInSeconds() {
		return agentUploadIntervalInSeconds;
	}

	public void setAgentUploadIntervalInSeconds(Long agentUploadIntervalInSeconds) {
		this.agentUploadIntervalInSeconds = agentUploadIntervalInSeconds;
	}

	public Long getSamplingRate() {
		return samplingRate;
	}

	public void setSamplingRate(Long samplingRate) {
		this.samplingRate = samplingRate;
	}

	public Set<AppConfigURLRegex> getUrlRegex() {
		return urlRegex;
	}

	public void setUrlRegex(Set<AppConfigURLRegex> urlRegex) {
		this.urlRegex = urlRegex;
	}

	public Boolean getCachingEnabled() {
		return cachingEnabled;
	}

	public void setCachingEnabled(Boolean cachingEnabled) {
		this.cachingEnabled = cachingEnabled;
	}

	public Set<AppConfigCustomParameter> getCustomConfigParameters() {
		return customConfigParameters;
	}

	public void setCustomConfigParameters(
			Set<AppConfigCustomParameter> customConfigParameters) {
		this.customConfigParameters = customConfigParameters;
	}

	public String getAppConfigType() {
		return appConfigType;
	}

	public void setAppConfigType(String appConfigType) {
		this.appConfigType = appConfigType;
	}





}
