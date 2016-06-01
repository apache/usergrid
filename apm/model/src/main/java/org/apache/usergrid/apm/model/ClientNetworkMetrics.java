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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.usergrid.apm.model;

import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;


/**
 *
 * @author prabhat jha prabhat143@gmail.com
 */
@Entity
@Table(name = "CLIENT_NETWORK_METRICS")
@org.hibernate.annotations.Table(appliesTo="CLIENT_NETWORK_METRICS",
indexes = {
		@Index(name="appIdEndMinute", columnNames={"appId","endMinute"} )        
} )
public class ClientNetworkMetrics implements Serializable,Cloneable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	private Long id;

	private String sessionId;

	private Long appId;
	
	private String fullAppName;

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
	String deviceOSVersion;

	
	private Long latency = 0L;
	private Long numSamples = 0L; //it's going be 1 all the time but makes CEP thing easier.
	private Long numErrors = 0L;  //It's 0 or 1
	
	private String url;
	private String domain;
	private Long httpStatusCode;
	private Long responseDataSize;
	private Long serverProcessingTime = 0L;
	private Date serverReceiptTime;
	private Date serverResponseTime;
	private String serverId;

	private Double latitude = 0d;
	private Double longitude = 0d;


	/**
	 * Time when device initiated the webservices call
	 */
	private Date startTime;

	private Date endTime;

	private Date timeStamp;

	private Long endMinute;

	private Long endDay;

	private Long endHour;

	private Long endWeek;

	private Long endMonth;


	
	private String transactionDetails;

	String appConfigType = ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT;

	String applicationVersion = "UNKNOWN";




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




	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}


	public Long getEndMinute() {
		return endMinute;
	}


	public Long getEndHour() {
		return endHour;
	}


	public Long getEndDay() {
		return endDay;
	}


	public Long getEndWeek() {
		return endWeek;
	}


	public Long getEndMonth() {
		return endMonth;
	}

	public void setEndMinute(Long endMinute) {
		this.endMinute = endMinute;
	}

	public void setEndHour(Long endHour) {
		this.endHour = endHour;
	}

	public void setEndDay(Long endDay) {
		this.endDay = endDay;
	}

	public void setEndWeek(Long endWeek) {
		this.endWeek = endWeek;
	}

	public void setEndMonth(Long endMonth) {
		this.endMonth = endMonth;
	}


	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}		

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public Long getHttpStatusCode() {
		return httpStatusCode;
	}

	public void setHttpStatusCode(Long httpStatusCode) {
		this.httpStatusCode = httpStatusCode;
	}

	public Long getResponseDataSize() {
		return responseDataSize;
	}

	public void setResponseDataSize(Long responseDataSize) {
		this.responseDataSize = responseDataSize;
	}

	public Long getServerProcessingTime() {
		return serverProcessingTime;
	}

	public void setServerProcessingTime(Long serverProcessingTime) {
		this.serverProcessingTime = serverProcessingTime;
	}

	public Date getServerReceiptTime() {
		return serverReceiptTime;
	}

	public void setServerReceiptTime(Date serverReceiptTime) {
		this.serverReceiptTime = serverReceiptTime;
	}

	public Date getServerResponseTime() {
		return serverResponseTime;
	}

	public void setServerResponseTime(Date serverResponseTime) {
		this.serverResponseTime = serverResponseTime;
	}

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}


	
	public Long getLatency() {
		return latency;
	}

	public void setLatency(Long latency) {
		this.latency = latency;
	}


	public Long getNumSamples() {
		return numSamples;
	}

	public void setNumSamples(Long numSamples) {
		this.numSamples = numSamples;
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



	public String getTransactionDetails() {
		return transactionDetails;
	}

	public void setTransactionDetails(String transactionDetails) {
		this.transactionDetails = transactionDetails;
	}  



	public void setLatitude(Double lattitude) {
		this.latitude = lattitude;
	}

	public Double getLatitude() {
		return latitude;
	}


	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Double getLongitude() {
		return longitude;
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

	public Object clone()
	{
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public String getSessionId()
	{
		return sessionId;
	}

	public void setSessionId(String sessionId)
	{
		this.sessionId = sessionId;
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

	public String toString () {
		return fullAppName + " " + networkCarrier + " " + url + " " + 
				startTime.toString() + " " + numSamples +  " latency " + latency +  
				+ endMinute + " " + endHour + " " + endDay + " " + endWeek + " " + endMonth  + "\n";

	}
}
