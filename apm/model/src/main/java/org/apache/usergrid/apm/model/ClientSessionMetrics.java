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
@Table (name="CLIENT_SESSION_METRICS")
@org.hibernate.annotations.Table(appliesTo="CLIENT_SESSION_METRICS",
		indexes = {
		@Index(name="appIdEndMinute", columnNames={"appId","endMinute"} )        
} )
public class ClientSessionMetrics implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	Long id;
	
	String sdkVersion;
	
	/**
	 * iOS or Android or Ruby or JS or .NET
	 */
	String sdkType;

	String sessionId;



	/*
	 * The next set of parameters are network info parameters that may or may not be populated. I think 
	 * it is best to populate these parameters in the WebserviceMetricsBean envelop to prevent the need
	 * do the denormalization of data on the client side (move that denomalization to the server-side). 
	 * 
	 * Parameters are broken into:
	 * 
	 * NetworkInfo Parameters
	 * Telephony Parameters
	 * Location Parameters
	 * OS Parameters
	 */

	//Location Parameters
	Float bearing;
	Double latitude;
	Double longitude;

	//telephony Parameters
	String telephonyDeviceId;
	String telephonyNetworkOperator;
	String telephonyNetworkOperatorName;
	String telephonyNetworkType;
	String telephonySignalStrength;
	String telephonyPhoneType;

	//Network Parameters
	String networkExtraInfo;
	String networkSubType;
	String networkType;
	String networkTypeName;
	String networkCountry;
	String networkCarrier;
	Boolean isNetworkRoaming;

	/**
	 * For same session, a user may go from wifi to 3g. In that case this should be set to true
	 */
	Boolean isNetworkChanged;

	//DeviceParameters
	String deviceId;
	String deviceType;
	String deviceModel;
	String devicePlatform;
	String deviceOSVersion;
	/**
	 * deviceOperatingSystem = devicePlatform + deviceOSVersion
	 */
	String deviceOperatingSystem; 
	
	String localLanguage;
	String localCountry;
	String deviceCountry;  

	/**
	 * battery life in terms of percentage
	 */
	Integer batteryLevel = 0;


	//Software Versioning Parameters
	String applicationVersion;

		
	String appConfigType = ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT;;

	Long appId;

	private String fullAppName; //orgName_appName

	Date timeStamp;

	private Date sessionStartTime;

	private Long endMinute;

	private Long endHour;

	private Long endDay;

	private Long endWeek;

	private Long endMonth;



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

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}


	public Date getTimeStamp()
	{
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp)
	{
		 this.timeStamp = timeStamp;
	      
	      long time = timeStamp.getTime();
	      this.endMinute = time / 1000/60;
	      this.endHour = this.endMinute / 60;
	      this.endDay = this.endHour / 24;
	      this.endWeek = this.endDay / 7;
	      this.endMonth = this.endDay/30;
	}

	public Long getEndMinute()
	{
		return endMinute;
	}

	public void setEndMinute(Long endMinute)
	{
		this.endMinute = endMinute;
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

	public String getTelephonyDeviceId()
	{
		return telephonyDeviceId;
	}

	public void setTelephonyDeviceId(String telephonyDeviceId)
	{
		this.telephonyDeviceId = telephonyDeviceId;
	}

	public String getTelephonyNetworkOperator()
	{
		return telephonyNetworkOperator;
	}

	public void setTelephonyNetworkOperator(String telephonyNetworkOperator)
	{
		this.telephonyNetworkOperator = telephonyNetworkOperator;
	}

	public String getTelephonyNetworkOperatorName()
	{
		return telephonyNetworkOperatorName;
	}

	public void setTelephonyNetworkOperatorName(String telephonyNetworkOperatorName)
	{
		this.telephonyNetworkOperatorName = telephonyNetworkOperatorName;
	}

	public String getTelephonyNetworkType()
	{
		return telephonyNetworkType;
	}

	public void setTelephonyNetworkType(String telephonyNetworkType)
	{
		this.telephonyNetworkType = telephonyNetworkType;
	}

	public String getTelephonySignalStrength()
	{
		return telephonySignalStrength;
	}

	public void setTelephonySignalStrength(String telephonySignalStrength)
	{
		this.telephonySignalStrength = telephonySignalStrength;
	}

	public String getTelephonyPhoneType()
	{
		return telephonyPhoneType;
	}

	public void setTelephonyPhoneType(String telephonyPhoneType)
	{
		this.telephonyPhoneType = telephonyPhoneType;
	}

	public String getNetworkExtraInfo()
	{
		return networkExtraInfo;
	}

	public void setNetworkExtraInfo(String networkExtraInfo)
	{
		this.networkExtraInfo = networkExtraInfo;
	}

	public String getNetworkSubType()
	{
		return networkSubType;
	}

	public void setNetworkSubType(String networkSubType)
	{
		this.networkSubType = networkSubType;
	}

	public String getNetworkType()
	{
		return networkType;
	}

	public void setNetworkType(String networkType)
	{
		this.networkType = networkType;
	}

	public String getNetworkTypeName()
	{
		return networkTypeName;
	}

	public void setNetworkTypeName(String networkTypeName)
	{
		this.networkTypeName = networkTypeName;
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

	public String getDeviceOSVersion() {
		return deviceOSVersion;
	}

	public void setDeviceOSVersion(String deviceOSVersion) {
		this.deviceOSVersion = deviceOSVersion;
	}

	public String getDeviceOperatingSystem()
	{
		return deviceOperatingSystem;
	}

	public void setDeviceOperatingSystem(String deviceOperatingSystem)
	{
		this.deviceOperatingSystem = deviceOperatingSystem;
	}

	public String getLocalLanguage()
	{
		return localLanguage;
	}

	public void setLocalLanguage(String localLanguage)
	{
		this.localLanguage = localLanguage;
	}

	public String getLocalCountry()
	{
		return localCountry;
	}

	public void setLocalCountry(String localCountry)
	{
		this.localCountry = localCountry;
	}

	public String getDeviceCountry()
	{
		return deviceCountry;
	}

	public void setDeviceCountry(String deviceCountry)
	{
		this.deviceCountry = deviceCountry;
	}

	public String getApplicationVersion()
	{
		return applicationVersion;
	}

	public void setApplicationVersion(String applicationVersion)
	{
		this.applicationVersion = applicationVersion;
	}


	public Long getAppId()
	{
		return appId;
	}

	public void setAppId(Long appId)
	{
		this.appId = appId;
	}	
	

	public String getFullAppName() {
		return fullAppName;
	}

	public void setFullAppName(String fullAppName) {
		this.fullAppName = fullAppName;
	}

	public Integer getBatteryLevel()
	{
		return batteryLevel;
	}

	public void setBatteryLevel(Integer batteryLevel)
	{
		this.batteryLevel = batteryLevel;
	}


	public Boolean getIsNetworkChanged()
	{
		return isNetworkChanged;
	}

	public void setIsNetworkChanged(Boolean isNetworkChanged)
	{
		this.isNetworkChanged = isNetworkChanged;
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

	public String getDeviceId()
	{
		return deviceId;
	}

	public void setDeviceId(String deviceId)
	{
		this.deviceId = deviceId;
	}

	public String getSessionId()
	{
		return sessionId;
	}

	public void setSessionId(String sessionId)
	{
		this.sessionId = sessionId;
	}

	public Date getSessionStartTime() {
		return sessionStartTime;
	}

	public void setSessionStartTime(Date sessionStartTime) {
		this.sessionStartTime = sessionStartTime;
	}  
		
	public String getSdkVersion() {
		return sdkVersion;
	}

	public void setSdkVersion(String sdkVersion) {
		this.sdkVersion = sdkVersion;
	}	

	public String getSdkType() {
		return sdkType;
	}

	public void setSdkType(String sdkType) {
		this.sdkType = sdkType;
	}

	public String toString () {
		return "app: " + fullAppName  +  "sdkVersion " + sdkVersion +  " sessionId: " + sessionId + " deviceModel: " + deviceModel  + 
				" devicePlatform: " + devicePlatform + " time : " + timeStamp;
	}





}
