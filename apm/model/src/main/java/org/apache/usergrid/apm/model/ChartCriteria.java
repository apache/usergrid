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

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;


@MappedSuperclass
public class ChartCriteria implements Serializable, Cloneable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected Long id;

	protected String chartName;

	protected String userName;

	protected String tags;


	protected String description;

	protected String fullAppName;
	protected Long appId;
	protected String appVersion;

	protected String appConfigType;

	protected String networkType;
	protected String networkCarrier;
	protected String networkCountry;

	protected String deviceId;
	protected String deviceType;
	protected String deviceModel;
	protected String devicePlatform;
	protected String deviceOS;   



	public enum PeriodType{LAST_X, SET_PERIOD};

	public enum LastX {LAST_HOUR, LAST_3HOUR, LAST_6HOUR, LAST_12HOUR, LAST_DAY, LAST_WEEK, LAST_MONTH, LAST_YEAR}

	/*
	 * Sample period for Weekly and Monthly data is in day but we need to differentiate them
	 */
	public enum SamplePeriod {MINUTE, HOUR, DAY_WEEK, DAY_MONTH, MONTH}

	@Enumerated(EnumType.STRING)
	@Column(name="PERIOD_TYPE")
	protected PeriodType periodType;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	
	
	public String getAppConfigType()
	{
		return appConfigType;
	}

	public void setAppConfigType(String configType)
	{
		this.appConfigType = configType;
	}

	protected LastX lastX;

	@Enumerated(EnumType.STRING)
	@Column(name="SAMPLE_PERIOD")  
	protected SamplePeriod samplePeriod;


	protected Date startDate;

	protected Date endDate;


	//Group by Parameters
	boolean groupedByApp;
	boolean groupedByAppVersion;
	boolean groupedByAppConfigType;

	boolean groupedByNetworkType;
	boolean groupedByNetworkCarrier;
	boolean groupedByNetworkCountry;

	boolean groupedByDeviceId;
	boolean groupedByDeviceType;
	boolean groupedByDeviceModel;
	boolean groupedbyDevicePlatform;
	boolean groupedByDeviceOS;   


	boolean defaultChart;
	boolean visible = true;

	public String getChartName()
	{
		return chartName;
	}

	public void setChartName(String chartName)
	{
		this.chartName = chartName;
	}

	public String getUserName()
	{
		return userName;
	}

	public void setUserName(String userName)
	{
		this.userName = userName;
	}

	public String getTags()
	{
		return tags;
	}

	public void setTags(String tags)
	{
		this.tags = tags;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public Long getAppId()
	{
		return appId;
	}

	public void setAppId(Long appId)
	{
		this.appId = appId;
	}

	public PeriodType getPeriodType()
	{
		return periodType;
	}

	public void setPeriodType(PeriodType periodType)
	{
		this.periodType = periodType;
	}

	public Date getStartDate()
	{
		return startDate;
	}

	public void setStartDate(Date startDate)
	{
		this.startDate = startDate;      
	}

	public Date getEndDate()
	{
		return endDate;

	}


	public String getNetworkType()
	{
		return networkType;
	}

	public void setNetworkType(String networkType)
	{
		this.networkType = networkType;
	}

	public String getNetworkCarrier()
	{
		return networkCarrier;
	}

	public void setNetworkCarrier(String networkCarrier)
	{
		this.networkCarrier = networkCarrier;
	}

	public void setEndDate(Date endDate)
	{
		this.endDate = endDate;     
	}

	public boolean isGroupedByApp()
	{
		return groupedByApp;
	}

	public void setGroupedByApp(boolean groupedByApp)
	{
		this.groupedByApp = groupedByApp;
	}

	public boolean isGroupedByNetworkType()
	{
		return groupedByNetworkType;
	}

	public void setGroupedByNetworkType(boolean groupedByNetworkType)
	{
		this.groupedByNetworkType = groupedByNetworkType;
	}

	public boolean isGroupedByNetworkCarrier()
	{
		return groupedByNetworkCarrier;
	}

	public void setGroupedByNeworkProvider(boolean groupedByNeworkCarrier)
	{
		this.groupedByNetworkCarrier = groupedByNeworkCarrier;
	}

	public boolean isDefaultChart()
	{
		return defaultChart;
	}

	public void setDefaultChart(boolean defaultChart)
	{
		this.defaultChart = defaultChart;
	} 


	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public LastX getLastX()
	{
		return lastX;
	}

	public void setSamplePeriod(SamplePeriod samplePeriod)
	{
		this.samplePeriod = samplePeriod;
	}

	public void setLastX(LastX lastX) {
		this.lastX = lastX;         
		this.periodType = PeriodType.LAST_X;

		//Note that SamplePeriod is not currently being used in query since CEP currently only does for MINUTE so all SamplePeriod reference can be removed
		//if CEP stays like that.
		if (lastX!=null) {
			switch(lastX) {
			case LAST_HOUR:
			case LAST_6HOUR:
			case LAST_3HOUR:
			case LAST_12HOUR:
				this.samplePeriod = SamplePeriod.MINUTE; 
				break;
			case LAST_DAY:
				this.samplePeriod = SamplePeriod.HOUR;
				break;
			case LAST_WEEK:
				this.samplePeriod = SamplePeriod.HOUR; //we will have 24*7 data points
				break;
			case LAST_MONTH:
				this.samplePeriod = SamplePeriod.DAY_MONTH;
				break;
			case LAST_YEAR:
				this.samplePeriod = SamplePeriod.MONTH;
				break;
			}
		}
	}

	public SamplePeriod getSamplePeriod() {
		if (samplePeriod != null)
			return samplePeriod;
		else {
			if (startDate != null && endDate != null) {
				long hourdiff = findDiffInHour();
				if (hourdiff <= 12)
					return SamplePeriod.MINUTE;
				else if (hourdiff <=24)
					return SamplePeriod.HOUR;
				else if (hourdiff/24 <=7)
					return SamplePeriod.HOUR; //for weekly view we want to show 24*7 data points otherwise there will be only 7 data points
				else if (hourdiff/24 <=30)
					return SamplePeriod.DAY_MONTH;
				else
					return SamplePeriod.MONTH;
			}
		}
		return null;
	}

	public boolean hasGrouping () {
		return groupedByApp || groupedByNetworkType || groupedByNetworkCarrier || groupedByAppConfigType 
				|| groupedByAppVersion || groupedByDeviceId || groupedByDeviceModel || groupedByDeviceOS || groupedByDeviceType
				|| groupedbyDevicePlatform;
	}

	public int calculateExpectedNumberOfDataPoint () {
		int rowCount = 0;

		if (periodType.equals(PeriodType.LAST_X)) {
			switch (lastX)  {
			case LAST_HOUR:
				rowCount = 60;
				break;
			case LAST_DAY:
				rowCount = 24;
				break;
			case LAST_WEEK:
				rowCount = 7;
				break;
			case LAST_MONTH:
				rowCount = 30;
				break;
			case LAST_YEAR:
				rowCount = 12;
				break;
			}
		}
		else {
			//TODO: some calculation
		}
		return rowCount;
	}
	protected long findDiffInHour()  {
		long ms1 = startDate.getTime();
		long ms2 = endDate.getTime();
		long diff = ms2 - ms1;
		return diff/(1000*60*60);

	}

	public String getAppVersion()
	{
		return appVersion;
	}

	public void setAppVersion(String appVersion)
	{
		this.appVersion = appVersion;
	}

	public String getNetworkCountry()
	{
		return networkCountry;
	}

	public void setNetworkCountry(String networkCountry)
	{
		this.networkCountry = networkCountry;
	}



	public void setGroupedByNetworkCarrier(boolean groupedByNeworkCarrier)
	{
		this.groupedByNetworkCarrier = groupedByNeworkCarrier;
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

	public String getDeviceOS()
	{
		return deviceOS;
	}

	public void setDeviceOS(String deviceOS)
	{
		this.deviceOS = deviceOS;
	}

	public boolean isGroupedByAppVersion()
	{
		return groupedByAppVersion;
	}

	public void setGroupedByAppVersion(boolean groupedByAppVersion)
	{
		this.groupedByAppVersion = groupedByAppVersion;
	}

	public boolean isGroupedByAppConfigType()
	{
		return groupedByAppConfigType;
	}

	public void setGroupedByAppConfigType(boolean groupedByAppConfigType)
	{
		this.groupedByAppConfigType = groupedByAppConfigType;
	}

	public boolean isGroupedByNetworkCountry()
	{
		return groupedByNetworkCountry;
	}

	public void setGroupedByNetworkCountry(boolean groupedByNetworkCountry)
	{
		this.groupedByNetworkCountry = groupedByNetworkCountry;
	}

	public boolean isGroupedByDeviceId()
	{
		return groupedByDeviceId;
	}

	public void setGroupedByDeviceId(boolean groupedByDeviceId)
	{
		this.groupedByDeviceId = groupedByDeviceId;
	}

	public boolean isGroupedByDeviceType()
	{
		return groupedByDeviceType;
	}

	public void setGroupedByDeviceType(boolean groupedByDeviceType)
	{
		this.groupedByDeviceType = groupedByDeviceType;
	}

	public boolean isGroupedByDeviceModel()
	{
		return groupedByDeviceModel;
	}

	public void setGroupedByDeviceModel(boolean groupdByDeviceModel)
	{
		this.groupedByDeviceModel = groupdByDeviceModel;
	}

	public boolean isGroupedbyDevicePlatform()
	{
		return groupedbyDevicePlatform;
	}

	public void setGroupedbyDevicePlatform(boolean groupedbyDevicePlatform)
	{
		this.groupedbyDevicePlatform = groupedbyDevicePlatform;
	}

	public boolean isGroupedByDeviceOS()
	{
		return groupedByDeviceOS;
	}

	public void setGroupedByDeviceOS(boolean groupedByDeviceOS)
	{
		this.groupedByDeviceOS = groupedByDeviceOS;
	}	

	public String getFullAppName() {
		return fullAppName;
	}

	public void setFullAppName(String fullAppName) {
		this.fullAppName = fullAppName;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
