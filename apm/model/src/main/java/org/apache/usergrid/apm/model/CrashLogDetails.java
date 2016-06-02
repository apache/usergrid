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

import org.apache.usergrid.apm.model.ApigeeMobileAPMConstants;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;


@Entity
@Table(name = "CRASH_LOG_DETAILS")
@org.hibernate.annotations.Table(appliesTo="CRASH_LOG_DETAILS",
indexes = {
		@Index(name="appIdEndMinute", columnNames={"appId","endMinute"} )        
} )
public class CrashLogDetails implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private Long id;

	private Long appId;
	
	private String fullAppName;

	private String applicationVersion;

	String appConfigType = ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT;


	String networkType;
	String networkCountry;
	String networkCarrier;
	Boolean isNetworkRoaming;  

	Float bearing;
	Double latitude;
	Double longitude;

	//DeviceParameters
	String deviceId;
	String deviceType;
	String deviceModel;
	String devicePlatform;
	String deviceOperatingSystem;

	private Date timeStamp;

	private Long endMinute;

	private Long endHour;

	private Long endDay;   

	private Long endWeek;

	private Long endMonth;

	private String customMetaData;
	
	private String crashSummary;
	
	//only applicable for native SDKs
	private String crashFileName;

	@Transient
	public String crashLogUrl;

	
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

	public String getApplicationVersion() {
		return applicationVersion;
	}

	public void setApplicationVersion(String applicationVersion) {
		this.applicationVersion = applicationVersion;
	}

	public String getAppConfigType() {
		return appConfigType;
	}

	public void setAppConfigType(String appConfigType) {
		this.appConfigType = appConfigType;
	}

	public String getNetworkType() {
		return networkType;
	}

	public void setNetworkType(String networkType) {
		this.networkType = networkType;
	}

	public String getNetworkCountry() {
		return networkCountry;
	}

	public void setNetworkCountry(String networkCountry) {
		this.networkCountry = networkCountry;
	}

	public String getNetworkCarrier() {
		return networkCarrier;
	}

	public void setNetworkCarrier(String networkCarrier) {
		this.networkCarrier = networkCarrier;
	}

	public Boolean getIsNetworkRoaming() {
		return isNetworkRoaming;
	}

	public void setIsNetworkRoaming(Boolean isNetworkRoaming) {
		this.isNetworkRoaming = isNetworkRoaming;
	}

	public Float getBearing() {
		return bearing;
	}

	public void setBearing(Float bearing) {
		this.bearing = bearing;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getDeviceModel() {
		return deviceModel;
	}

	public void setDeviceModel(String deviceModel) {
		this.deviceModel = deviceModel;
	}

	public String getDevicePlatform() {
		return devicePlatform;
	}

	public void setDevicePlatform(String devicePlatform) {
		this.devicePlatform = devicePlatform;
	}

	public String getDeviceOperatingSystem() {
		return deviceOperatingSystem;
	}

	public void setDeviceOperatingSystem(String deviceOperatingSystem) {
		this.deviceOperatingSystem = deviceOperatingSystem;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
		 this.timeStamp = timeStamp;
	      
	      long time = timeStamp.getTime();
	      this.endMinute = time / 1000/60;
	      this.endHour = this.endMinute / 60;
	      this.endDay = this.endHour / 24;
	      this.endWeek = this.endDay / 7;
	      this.endMonth = this.endDay/30;

	}

	public Long getEndMinute() {
		return endMinute;
	}

	public void setEndMinute(Long endMinute) {
		this.endMinute = endMinute;
	}

	public Long getEndHour() {
		return endHour;
	}

	public void setEndHour(Long endHour) {
		this.endHour = endHour;
	}

	public Long getEndDay() {
		return endDay;
	}

	public void setEndDay(Long endDay) {
		this.endDay = endDay;
	}

	public Long getEndWeek() {
		return endWeek;
	}

	public void setEndWeek(Long endWeek) {
		this.endWeek = endWeek;
	}

	public Long getEndMonth() {
		return endMonth;
	}

	public void setEndMonth(Long endMonth) {
		this.endMonth = endMonth;
	}

	public String getCustomMetaData() {
		return customMetaData;
	}

	public void setCustomMetaData(String customMetaData) {
		this.customMetaData = customMetaData;
	}

	public String getCrashSummary() {
		return crashSummary;
	}

	public void setCrashSummary(String crashSummary) {
		this.crashSummary = crashSummary;
	}

	
	public String getCrashLogUrl() {
		return crashLogUrl;
	}

	public void setCrashLogUrl(String crashLogUrl) {
		this.crashLogUrl = crashLogUrl;
	}	
	
	public String getCrashFileName() {
		return crashFileName;
	}

	public void setCrashFileName(String crashFileName) {
		this.crashFileName = crashFileName;
	}

	public String toString () {
		return "AppId: " + appId + " crash summary: " + crashSummary;
	}



}
