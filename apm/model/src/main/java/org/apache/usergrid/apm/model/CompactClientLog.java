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
import org.apache.usergrid.apm.model.ChartCriteria.SamplePeriod;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "COMPACT_CLIENT_LOG")
@org.hibernate.annotations.Table(appliesTo="COMPACT_CLIENT_LOG",
indexes = {
		@Index(name="LogChartMinute", columnNames={"appId","chartCriteriaId","endMinute"} ),
		@Index(name="LogChartHour", columnNames={"appId","chartCriteriaId","endHour"} )  
} )
public class CompactClientLog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * This is the primary key for a log record    * 
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private Long id;

	private Long chartCriteriaId;

	/**
	 * This relates to applicaitonId from {@link org.apache.usergrid.apm.model.App}
	 */
	private Long appId;

	private String fullAppName;

	private String chartCriteria;

	private String applicationVersion;

	String networkType;
	String networkCountry;
	String networkCarrier;
	Boolean isNetworkRoaming;  

	//DeviceParameters
	String deviceId;
	String deviceType;
	String deviceModel;
	String devicePlatform;
	String deviceOperatingSystem;

	Float bearing;
	Double latitude;
	Double longitude;


	private Date timeStamp;

	private Long endMinute;
	private Long endHour;
	private Long endDay;
	private Long endWeek;
	private Long endMonth;
	private Long endYear;



	private String logLevel;

	private Long assertCount;
	private Long errorCount;
	private Long warnCount;
	private Long infoCount;
	private Long debugCount;
	private Long verboseCount;
	private Long errorAndAboveCount;
	private Long crashCount;



	String appConfigType = ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT;



	@Enumerated(EnumType.STRING)
	@Column(name="SAMPLE_PERIOD",nullable=false) 
	SamplePeriod samplePeriod;


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


	public Long getEndMinute() {
		return endMinute;
	}

	public void setEndMinute(Long endMinute) {
		this.endMinute = endMinute;
	}


	@Column(length=1)
	public String getLogLevel() {
		return logLevel;
	}

	public void setLogLevel(String logLevel) {
		this.logLevel = logLevel;
	}

	@Override
	public String toString() {
		return "[" + timeStamp.toString() + "] [" + logLevel + "] " + " [" + deviceId + "] " ;
	}



	public String getAppConfigType()
	{
		if (this.appConfigType == null)
			this.appConfigType =ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT;
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

	public String getDevicePlatform()
	{
		return devicePlatform;
	}

	public void setDevicePlatform(String devicePlatform)
	{
		this.devicePlatform = devicePlatform;
	}

	public String getDeviceModel()
	{
		return deviceModel;
	}

	public void setDeviceModel(String deviceModel)
	{
		this.deviceModel = deviceModel;
	}

	public Long getAssertCount()
	{
		return assertCount;
	}

	public void setAssertCount(Long assertCount)
	{
		this.assertCount = assertCount;
	}

	public Long getErrorCount()
	{
		return errorCount;
	}

	public void setErrorCount(Long errorCount)
	{
		this.errorCount = errorCount;
	}

	public Long getWarnCount()
	{
		return warnCount;
	}

	public void setWarnCount(Long warnCount)
	{
		this.warnCount = warnCount;
	}

	public Long getInfoCount()
	{
		return infoCount;
	}

	public void setInfoCount(Long infoCount)
	{
		this.infoCount = infoCount;
	}

	public Long getDebugCount()
	{
		return debugCount;
	}

	public void setDebugCount(Long debugCount)
	{
		this.debugCount = debugCount;
	}

	public Long getVerboseCount()
	{
		return verboseCount;
	}

	public void setVerboseCount(Long verboseCount)
	{
		this.verboseCount = verboseCount;
	}

	public Long getErrorAndAboveCount()
	{
		return errorAndAboveCount;
	}

	public void setErrorAndAboveCount(Long errorAndAboveCount)
	{
		this.errorAndAboveCount = errorAndAboveCount;
	}

	public Long getCrashCount() {
		return crashCount;
	}

	public void setCrashCount(Long crashCount) {
		this.crashCount = crashCount;
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

	public String getDeviceOperatingSystem()
	{
		return deviceOperatingSystem;
	}

	public void setDeviceOperatingSystem(String deviceOperatingSystem)
	{
		this.deviceOperatingSystem = deviceOperatingSystem;
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

	public Long getEndYear()
	{
		return endYear;
	}

	public void setEndYear(Long endYear)
	{
		this.endYear = endYear;
	}

	public Long getChartCriteriaId()
	{
		return chartCriteriaId;
	}

	public void setChartCriteriaId(Long chartCriteriaId)
	{
		this.chartCriteriaId = chartCriteriaId;
	}

	public SamplePeriod getSamplePeriod()
	{
		return samplePeriod;
	}

	public void setSamplePeriod(SamplePeriod samplePeriod)
	{
		this.samplePeriod = samplePeriod;
	}	

	public String getFullAppName() {
		return fullAppName;
	}

	public void setFullAppName(String fullAppName) {
		this.fullAppName = fullAppName;
	}

	public String getChartCriteria() {
		return chartCriteria;
	}

	public void setChartCriteria(String chartCriteria) {
		this.chartCriteria = chartCriteria;
	}

}
