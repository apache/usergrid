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


@Entity
@Table(name = "CLIENT_LOG")
@org.hibernate.annotations.Table(appliesTo="CLIENT_LOG",
indexes = {
		@Index(name="appIdEndMinute", columnNames={"appId","endMinute"} )        
} )
public class ClientLog implements Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * This is the primary key for a log record
	 */
	private Long id;

	private String sessionId;

	/**
	 * This relates to applicaitonId from {@link org.apache.usergrid.apm.model.App}
	 */
	private Long appId;

	private String fullAppName; //orgName_appName

	private String applicationVersion;

	String appConfigType = ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT;


	String networkType;
	String networkCountry;
	String networkCarrier;
	Boolean isNetworkRoaming;

	//DeviceParameters
	String deviceId;
	String deviceType;
	String deviceModel;
	String devicePlatform;
	String deviceOSVersion;
	String deviceOperatingSystem; //devicePlatform + deviceOSVersion



	Float bearing;
	Double latitude;
	Double longitude;


	public static final int ASSERT = 7;
	public static final int ERROR = 6;
	public static final int WARN = 5;
	public static final int INFO = 4;
	public static final int DEBUG = 3;
	public static final int VERBOSE = 2;

	/**
	 * Time when device initiated the webservices call
	 */
	private Date timeStamp;

	private Long endMinute;

	private Long endHour;

	private Long endDay;

	private Long endWeek;

	private Long endMonth;

	private Date correctedTimestamp;

	private String logLevel = ApigeeMobileAPMConstants.logLevelsString[ApigeeMobileAPMConstants.LOG_DEBUG];

	private String logMessage;

	public String tag;

	@Transient
	public String logLevelString;

	@Transient
	public String crashLogUrl;


	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}


	public Long getAppId() {
		return appId;
	}

	public void setAppId(Long appId) {
		this.appId = appId;
	}

	public String getFullAppName() {
		return fullAppName;
	}

	public void setFullAppName(String fullAppName) {
		this.fullAppName = fullAppName;
	}


	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}



	public String getNetworkType() {
		return networkType;
	}

	public void setNetworkType(String networkType) {
		this.networkType = networkType;
	}


	public Date getTimeStamp() {
		return timeStamp;
	}



	public void setTimeStamp(Date timeStamp) {
		 this.timeStamp = timeStamp;

	      long time = timeStamp.getTime();
	      this.endMinute = time / 1000/60;
	      this.endHour = this.endMinute / 60;
	      this.endDay = this.endHour / 24;
	      this.endWeek = this.endDay / 7;
	      this.endMonth = this.endDay/30;


	}

	public Date getCorrectedTimestamp() {
		return correctedTimestamp;
	}

	public void setCorrectedTimestamp(Date correctedTimestamp) {
		this.correctedTimestamp = correctedTimestamp;
		if(correctedTimestamp != null)
		{
			long time = correctedTimestamp.getTime();
			this.setEndMinute(time / 1000/60);
			this.setEndHour(time / 1000/ 60 / 60);
			this.setEndDay(time / 1000/ 60 / 60 / 24);
			this.setEndWeek(time / 1000/ 60 / 60 / 24 / 7);
			this.setEndMonth(time / 1000/ 60 / 60 / 24 / 7 / 12);
		}
	}


	public Long getEndMinute() {
		return endMinute;
	}

	public void setEndMinute(Long endMinute) {
		this.endMinute = endMinute;
	}


	public String getLogMessage() {
		return logMessage;
	}

	public void setLogMessage(String logMessage) {
		this.logMessage = logMessage;
	}


	public String getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}


	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	@Override
	public String toString() {
		return "[" + fullAppName + " timeStamp " + timeStamp.toString() + "][ Tag : " + tag + "]"
				+ "[ Device Platform : " + devicePlatform + "]"
				+ "[ Platform Version : " + deviceOperatingSystem + "]"
				+ "[ Device Model : " + deviceModel + "]"
				+ "[ Devicd ID :" + deviceId + "]"
				+   logMessage  ;
	}


	@Column(name="CONFIG_TYPE",nullable=false)
	public String getAppConfigType()
	{
		if (this.appConfigType == null)
			this.appConfigType = ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT;
		return appConfigType;
	}

	public void setAppConfigType(String appConfigType)
	{

		this.appConfigType = appConfigType;
	}

	public String getApplicationVersion()
	{
		return applicationVersion;
	}

	public void setApplicationVersion(String applicationVersion)
	{
		this.applicationVersion = applicationVersion;
	}

	public String getNetworkCountry()
	{
		return networkCountry;
	}

	public void setNetworkCountry(String networkCountry)
	{
		this.networkCountry = networkCountry;
	}

	public String getNetworkCarrier()
	{
		return networkCarrier;
	}

	public void setNetworkCarrier(String networkCarrier)
	{
		this.networkCarrier = networkCarrier;
	}

	public Boolean getIsNetworkRoaming()
	{
		return isNetworkRoaming;
	}

	public void setIsNetworkRoaming(Boolean isNetworkRoaming)
	{
		this.isNetworkRoaming = isNetworkRoaming;
	}


	public String getDeviceType()
	{
		return deviceType;
	}

	public void setDeviceType(String deviceType)
	{
		this.deviceType = deviceType;
	}

	public String getDeviceModel()
	{
		return deviceModel;
	}

	public void setDeviceModel(String deviceModel)
	{
		this.deviceModel = deviceModel;
	}

	public String getDevicePlatform()
	{
		return devicePlatform;
	}

	public void setDevicePlatform(String devicePlatform)
	{
		this.devicePlatform = devicePlatform;
	}

	public String getDeviceOperatingSystem()
	{
		return deviceOperatingSystem;
	}

	public void setDeviceOperatingSystem(String deviceOperatingSystem)
	{
		this.deviceOperatingSystem = deviceOperatingSystem;
	}	

	public String getDeviceOSVersion() {
		return deviceOSVersion;
	}

	public void setDeviceOSVersion(String deviceOSVersion) {
		this.deviceOSVersion = deviceOSVersion;
	}

	public Long getEndHour()
	{
		return endHour;
	}

	public void setEndHour(Long endHour)
	{
		this.endHour = endHour;
	}

	public Long getEndDay()
	{
		return endDay;
	}

	public void setEndDay(Long endDay)
	{
		this.endDay = endDay;
	}

	public Long getEndWeek()
	{
		return endWeek;
	}

	public void setEndWeek(Long endWeek)
	{
		this.endWeek = endWeek;
	}

	public Long getEndMonth()
	{
		return endMonth;
	}

	public void setEndMonth(Long endMonth)
	{
		this.endMonth = endMonth;
	}

	public Float getBearing()
	{
		return bearing;
	}

	public void setBearing(Float bearing)
	{
		this.bearing = bearing;
	}

	public Double getLatitude()
	{
		return latitude;
	}

	public void setLatitude(Double latitude)
	{
		this.latitude = latitude;
	}

	public Double getLongitude()
	{
		return longitude;
	}

	public void setLongitude(Double longitude)
	{
		this.longitude = longitude;
	}  


	public String getSessionId()
	{
		return sessionId;
	}

	public void setSessionId(String sessionId)
	{
		this.sessionId = sessionId;
	}



	@Transient
	public String getCrashLogUrl() {
		return crashLogUrl;
	}


	public void setCrashLogUrl(String crashLogUrl) {
		this.crashLogUrl = crashLogUrl;
	}

	





}
