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
@Table(name = "SUMMARY_SESSION_METRICS")
@org.hibernate.annotations.Table(appliesTo="SUMMARY_SESSION_METRICS",
indexes = {
		@Index(name="SummarySessionDevicesForCEP", columnNames={"appId", "sessionId"}), 
		@Index(name="SummarySessionForMinuteChart", columnNames={"appId", "endMinute"})
} )


//@Index(name="SummarySessionForHourChart", columnNames={"appId", "endHour"}) 
public class SummarySessionMetrics implements Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//In milliseconds
	public static final long SESSION_EXPIRY_DURATION = 1800000;

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	Long id;

	private String fullAppName;

	/**
	 * This can simply be "id", the primary key of this table but we may choose to implement some more descriptive
	 * sessionId so that we can easily refer what this session means. This is the field that will tie ClientLog,
	 * ClientSession and ClientNetworkMetrics together. However, tieing them together is not going to be easy.
	 */
	private String sessionId;

	/**
	 * Session data
	 * 
	 * During the course of a single session, the mobile agent may upload information
	 * to the portal multiple times. Hence there is a 1:many cardinality between {@link org.apache.usergrid.apm.model.SummarySessionMetrics}
	 * and {@link org.apache.usergrid.apm.model.ClientSessionMetrics}
	 */

	Date sessionStartTime; 
	Date sessionEndTime;
	Date sessionExpiryTime;

	Long startMinute;
	Long endMinute;
	Long startHour;
	Long endHour;
	Long startDay;
	Long endDay;
	Long startWeek;
	Long endWeek;
	Long startMonth;
	Long endMonth;

	@Transient
	Date prevSessionEndTime;
	@Transient
	Long prevEndMinute;
	@Transient
	Long prevEndHour;
	@Transient
	Long prevEndDay;
	@Transient
	Long prevEndWeek;
	@Transient
	Long prevEndMonth;


	/*
	 * in seconds
	 */
	long sessionLength;

	Long userActivityCount;

	Long userActivityLength;

	Boolean sessionExplicitlyEnded = false;

	Boolean isActiveSession = true;


	/**
	 * battery life in terms of percentage
	 */
	Integer startBatteryLevel;
	Integer endBatteryLevel;
	Integer batteryConsumption;

	String deviceId;
	String deviceType;
	String deviceModel;
	String devicePlatform;
	String deviceOperatingSystem;
	String localLanguage;
	String localCountry;
	String deviceCountry;  

	//Network Parameters
	String networkType;
	String networkCountry;
	String networkCarrier;

	//Software Versioning Parameters
	String applicationVersion;


	@Column(name="CONFIG_TYPE",nullable=false)
	String appConfigType = ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT;;

	Long appId;

	private Long chartCriteriaId;

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public Date getSessionStartTime()
	{
		return sessionStartTime;
	}

	public void setSessionStartTime(Date sessionStartTime)
	{
		this.sessionStartTime = sessionStartTime;
		long time = sessionStartTime.getTime();

		this.setStartMinute(time / 1000/60);
		this.setStartHour(startMinute/ 60);
		this.setStartDay(startHour / 24);
		this.setStartWeek(startDay/7);
		this.setStartMonth(startDay/30);
	}

	public Date getSessionEndTime()
	{
		return sessionEndTime;
	}


	public void setSessionEndTime(Date sessionEndTime)
	{
		this.sessionEndTime = sessionEndTime;
		if (sessionStartTime != null) {
			this.sessionLength = (sessionEndTime.getTime() - sessionStartTime.getTime())/1000;
		}

		long time = sessionEndTime.getTime();
		this.endMinute = time / 1000/60;
		this.endHour = this.endMinute / 60;
		this.endDay = this.endHour / 24;
		this.endWeek = this.endDay / 7;
		this.endMonth = this.endDay/30;

		this.setSessionExpiryTime(new Date(sessionEndTime.getTime() + SESSION_EXPIRY_DURATION));

	}  


	public Date getSessionExpiryTime() {
		return sessionExpiryTime;
	}

	public void setSessionExpiryTime(Date sessionExpiryTime) {
		this.sessionExpiryTime = sessionExpiryTime;
	}

	public Long getSessionLength()
	{
		return sessionLength;
	}

	public void setSessionLength(Long sessionLength)
	{
		this.sessionLength = sessionLength;
	}

	public Integer getStartBatteryLevel()
	{
		return startBatteryLevel;
	}

	public void setStartBatteryLevel(Integer startBatteryLevel)
	{
		this.startBatteryLevel = startBatteryLevel;
	}

	public Integer getEndBatteryLevel()
	{
		return endBatteryLevel;
	}

	public void setEndBatteryLevel(Integer endBatteryLevel)
	{
		this.endBatteryLevel = endBatteryLevel;
		//      if(this.endBatteryLevel != null && this.startBatteryLevel != null)
		//      {
		//    	  this.batteryConsumption = this.endBatteryLevel - this.startBatteryLevel;
		//      }
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

	public String getAppConfigType()
	{
		return appConfigType;
	}

	public void setAppConfigType(String appConfigType)
	{
		this.appConfigType = appConfigType;
	}

	public Long getAppId()
	{
		return appId;
	}

	public void setAppId(Long appId)
	{
		this.appId = appId;
	}

	public Integer getBatteryConsumption()
	{
		return batteryConsumption;
	}

	public void setBatteryConsumption(Integer batteryConsumption)
	{
		this.batteryConsumption = batteryConsumption;
	}

	public Long getUserActivityCount()
	{
		return userActivityCount;
	}

	public void setUserActivityCount(Long userActivityCount)
	{
		this.userActivityCount = userActivityCount;
	}

	public Long getUserActivityLength()
	{
		return userActivityLength;
	}

	public void setUserActivityLength(Long userActivityLength)
	{
		this.userActivityLength = userActivityLength;
	}

	public String getDeviceId()
	{
		return deviceId;
	}

	public void setDeviceId(String deviceId)
	{
		this.deviceId = deviceId;
	}

	public Boolean getSessionExplicitlyEnded() {
		return sessionExplicitlyEnded;
	}

	public void setSessionExplicitlyEnded(Boolean sessionExplicitlyEnded) {
		this.sessionExplicitlyEnded = sessionExplicitlyEnded;
	}

	public Boolean getIsActiveSession() {
		return isActiveSession;
	}

	public void setIsActiveSession(Boolean isActiveSession) {
		this.isActiveSession = isActiveSession;
	}

	public Long getStartMinute()
	{
		return startMinute;
	}

	public void setStartMinute(Long startMinute)
	{
		this.startMinute = startMinute;
	}

	public Long getEndMinute()
	{
		return endMinute;
	}

	public void setEndMinute(Long endMinute)
	{
		this.endMinute = endMinute;
	}

	public Long getStartHour()
	{
		return startHour;
	}

	public void setStartHour(Long startHour)
	{
		this.startHour = startHour;
	}

	public Long getEndHour()
	{
		return endHour;
	}

	public void setEndHour(Long endHour)
	{
		this.endHour = endHour;
	}

	public Long getStartDay()
	{
		return startDay;
	}

	public void setStartDay(Long startDay)
	{
		this.startDay = startDay;
	}

	public Long getEndDay()
	{
		return endDay;
	}

	public void setEndDay(Long endDay)
	{
		this.endDay = endDay;
	}

	public Long getStartWeek()
	{
		return startWeek;
	}

	public void setStartWeek(Long startWeek)
	{
		this.startWeek = startWeek;
	}

	public Long getEndWeek()
	{
		return endWeek;
	}

	public void setEndWeek(Long endWeek)
	{
		this.endWeek = endWeek;
	}

	public Long getStartMonth()
	{
		return startMonth;
	}

	public void setStartMonth(Long startMonth)
	{
		this.startMonth = startMonth;
	}

	public Long getEndMonth()
	{
		return endMonth;
	}

	public void setEndMonth(Long endMonth)
	{
		this.endMonth = endMonth;
	}  


	public String getNetworkType()
	{
		return networkType;
	}

	public void setNetworkType(String networkType)
	{
		this.networkType = networkType;
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

	public Long getChartCriteriaId()
	{
		return chartCriteriaId;
	}

	public void setChartCriteriaId(Long chartCriteriaId)
	{
		this.chartCriteriaId = chartCriteriaId;
	}

	public String getSessionId()
	{
		return sessionId;
	}

	public void setSessionId(String sessionId)
	{
		this.sessionId = sessionId;
	}

	public void setSessionLength(long sessionLength)
	{
		this.sessionLength = sessionLength;
	}

	public Long getPrevEndMinute()
	{
		return prevEndMinute;
	}

	public void setPrevEndMinute(Long prevEndMinute)
	{
		this.prevEndMinute = prevEndMinute;
	}

	public Long getPrevEndHour()
	{
		return prevEndHour;
	}

	public void setPrevEndHour(Long prevEndHour)
	{
		this.prevEndHour = prevEndHour;
	}

	public Long getPrevEndDay()
	{
		return prevEndDay;
	}

	public void setPrevEndDay(Long prevEndDay)
	{
		this.prevEndDay = prevEndDay;
	}

	public Long getPrevEndWeek()
	{
		return prevEndWeek;
	}

	public void setPrevEndWeek(Long prevEndWeek)
	{
		this.prevEndWeek = prevEndWeek;
	}

	public Long getPrevEndMonth()
	{
		return prevEndMonth;
	}

	public void setPrevEndMonth(Long prevEndMonth)
	{
		this.prevEndMonth = prevEndMonth;
	}

	public Date getPrevSessionEndTime()
	{
		return prevSessionEndTime;
	}

	public void setPrevSessionEndTime(Date prevSessionEndTime)
	{
		this.prevSessionEndTime = prevSessionEndTime;

		long time = prevSessionEndTime.getTime();
		this.setPrevEndMinute(time / 1000/60);
		this.setPrevEndHour(time / 1000/ 60 / 60);
		this.setPrevEndDay(time / 1000/ 60 / 60 / 24);
		this.setPrevEndWeek(time / 1000/ 60 / 60 / 24 / 7);
		this.setPrevEndMonth(this.getPrevEndDay()/30);
	} 

	public String getFullAppName() {
		return fullAppName;
	}

	public void setFullAppName(String fullAppName) {
		this.fullAppName = fullAppName;
	}

	public String toString () {
		return fullAppName + " " + " deviceModel " + deviceModel  + " devicePlatform " + devicePlatform + " session end time : " + sessionEndTime;
	}





}
