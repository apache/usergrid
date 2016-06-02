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

import org.apache.usergrid.apm.model.ChartCriteria.SamplePeriod;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;


@Entity
@Table(name = "COMPACT_NETWORK_METRICS")
@org.hibernate.annotations.Table(appliesTo="COMPACT_NETWORK_METRICS",
		indexes = {
		@Index(name="NetworkMetricsChartMinute", columnNames={"appId","chartCriteriaId","endMinute"} ),
		@Index(name="NetworkMetricsChartHour", columnNames={"appId","chartCriteriaId","endHour"} ) 
} )
public class CompactNetworkMetrics implements Serializable {

	private static final long serialVersionUID = 1L;	

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private Long id;

	private Long appId;
	
	private String fullAppName;
	
	private String chartCriteria;
	
	private String domain;

	private Long minLatency = 0L;

	private Long maxLatency = 0L;

	private Long sumLatency = 0L;

	private Long numSamples = 0L;

	private Long numErrors = 0L;	
	
	
	private Long minServerLatency = 0L;

	private Long maxServerLatency = 0L;

	private Long sumServerLatency = 0L;	

	private Long chartCriteriaId;	
	
	


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



	private Date timeStamp;

	private Long endMinute;
	private Long endHour;
	private Long endDay;
	private Long endWeek;
	private Long endMonth;
	private Long endYear;


	//add all groupyby filters here

	boolean groupedByApp;
	boolean groupedByNetworkType;
	boolean groupedByNeworkProvider;

	//Software Versioning Parameters
	String applicationVersion;

	 
	String appConfigType;

	@Enumerated(EnumType.STRING)
	@Column(name="SAMPLE_PERIOD",nullable=false) 
	SamplePeriod samplePeriod;

	@Transient
	private String appConfigTypeString; //this is for work around of problem with addScalar and enum at NetworkMetricsDBService


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getAppId() {
		return appId;
	}

	public void setAppId(Long applicationId) {
		this.appId = applicationId;
	}

	public String getAppConfigType()
	{
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

	public Long getMinLatency() {
		return minLatency;
	}

	public void setMinLatency(Long minLatency) {
		this.minLatency = minLatency;
	}

	public Long getMaxLatency() {
		return maxLatency;
	}

	public void setMaxLatency(Long maxLatency) {
		this.maxLatency = maxLatency;
	}

	public Long getSumLatency() {
		return sumLatency;
	}

	public void setSumLatency(Long sumLatency) {
		this.sumLatency = sumLatency;
	}

	public Long getNumSamples() {
		return numSamples;
	}

	public void setNumSamples(Long numSamples) {
		this.numSamples = numSamples;
	}	

	public Long getMinServerLatency() {
		return minServerLatency;
	}

	public void setMinServerLatency(Long minServerLatency) {
		this.minServerLatency = minServerLatency;
	}

	public Long getMaxServerLatency() {
		return maxServerLatency;
	}

	public void setMaxServerLatency(Long maxServerLatency) {
		this.maxServerLatency = maxServerLatency;
	}

	public Long getSumServerLatency() {
		return sumServerLatency;
	}

	public void setSumServerLatency(Long sumServerLatency) {
		this.sumServerLatency = sumServerLatency;
	}

	public String getNetworkCarrier() {
		return networkCarrier;
	}

	public void setNetworkCarrier(String networkCarrier) {
		this.networkCarrier = networkCarrier;
	}

	public String getNetworkType() {
		return networkType;
	}

	public void setNetworkType(String networkType) {
		this.networkType = networkType;
	}

	public Long getNumErrors() {
		return numErrors;
	}

	public void setNumErrors(Long numErrors) {
		this.numErrors = numErrors;
	}

	public Long getChartCriteriaId() {
		return chartCriteriaId;
	}

	public void setChartCriteriaId(Long chartCriteriaId) {
		this.chartCriteriaId = chartCriteriaId;
	}

	public boolean isGroupedByApp() {
		return groupedByApp;
	}

	public void setGroupedByApp(boolean groupedByApp) {
		this.groupedByApp = groupedByApp;
	}


	public boolean isGroupedByNetworkType() {
		return groupedByNetworkType;
	}

	public void setGroupedByNetworkType(boolean groupedByNetworkType) {
		this.groupedByNetworkType = groupedByNetworkType;
	}


	public boolean isGroupedByNeworkProvider() {
		return groupedByNeworkProvider;
	}

	public void setGroupedByNeworkProvider(boolean groupedByNeworkProvider) {
		this.groupedByNeworkProvider = groupedByNeworkProvider;
	}


	public String getNetworkCountry()
	{
		return networkCountry;
	}

	public void setNetworkCountry(String networkCountry)
	{
		this.networkCountry = networkCountry;
	}

	public Boolean getIsNetworkRoaming()
	{
		return isNetworkRoaming;
	}

	public void setIsNetworkRoaming(Boolean isNetworkRoaming)
	{
		this.isNetworkRoaming = isNetworkRoaming;
	}

	public String getDeviceId()
	{
		return deviceId;
	}

	public void setDeviceId(String deviceId)
	{
		this.deviceId = deviceId;
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

	public Long getEndYear() {
		return endYear;
	}

	public void setEndYear(Long endYear) {
		this.endYear = endYear;
	}	

	public SamplePeriod getSamplePeriod() {
		return samplePeriod;
	}

	public void setSamplePeriod(SamplePeriod samplePeriod) {
		this.samplePeriod = samplePeriod;
	}
	
	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
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

	public String toString () {
		return appId + " CQ: " + chartCriteriaId + " carrier: " + networkCarrier + " type: " + networkType + " "
			+ " samples "	+ numSamples +  " latency " + sumLatency + " errors " + numErrors + " timeStamp " + timeStamp;
	}


}
