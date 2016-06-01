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
package org.apache.usergrid.apm.service.charts.service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.usergrid.apm.model.LogChartCriteria;

public class RawLogData extends RawData 
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	int logLevel;

	String logMessage;

	Long count;

	Date timeStamp; 

	String logLevelString;  

	String deviceType;
	String deviceModel;
	String devicePlatform;
	String deviceOperatingSystem;
	
	String applicationVersion;


	public int getLogLevel()
	{
		return logLevel;
	}

	public void setLogLevel(int logLevel)
	{
		this.logLevel = logLevel;
	}

	public String getLogMessage()
	{
		return logMessage;
	}

	public void setLogMessage(String logMessage)
	{
		this.logMessage = logMessage;
	}

	public Long getCount()
	{
		return count;
	}

	public void setCount(Long count)
	{
		this.count = count;
	}

	public Date getTimeStamp()
	{
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp)
	{
		this.timeStamp = timeStamp;
	}



	public String getLogLevelString()
	{
		switch (logLevel) {
		case 2:  return LogChartCriteria.LOG_LEVEL_STRING.VERBOSE.toString();
		case 3:  return LogChartCriteria.LOG_LEVEL_STRING.DEBUG.toString();
		case 4:  return LogChartCriteria.LOG_LEVEL_STRING.INFO.toString();
		case 5:  return LogChartCriteria.LOG_LEVEL_STRING.WARN.toString();
		case 6:  return LogChartCriteria.LOG_LEVEL_STRING.ERROR.toString();
		case 7:  return LogChartCriteria.LOG_LEVEL_STRING.ASSERT.toString();
		}
		return null;
	}

	public void setLogLevelString(String logLevelString)
	{
		this.logLevelString = logLevelString;
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
	

	public String getApplicationVersion() {
		return applicationVersion;
	}

	public void setApplicationVersion(String applicationVersion) {
		this.applicationVersion = applicationVersion;
	}

	public String toString() {
		return "logLevel: " + logLevel + " ,logMessage : " + logMessage + " ,count :" + count + " time: " + timeStamp;

	}

	private static final List<String> LEVELS = Arrays.asList(
			LogChartCriteria.LOG_LEVEL_STRING.VERBOSE.toString(),   // 2
			LogChartCriteria.LOG_LEVEL_STRING.DEBUG.toString(),     // 3
			LogChartCriteria.LOG_LEVEL_STRING.INFO.toString(),  // 4
			LogChartCriteria.LOG_LEVEL_STRING.WARN.toString(),  // 5
			LogChartCriteria.LOG_LEVEL_STRING.ERROR.toString(), // 6
			LogChartCriteria.LOG_LEVEL_STRING.ASSERT.toString() // 7
			);

	public static final int getLevelByName(String name) {
		int index = -1;

		if (name!=null && name.trim().length()>0) {
			name = name.toUpperCase();

			int i=0;
			for(String level:LEVELS) {
				if (level.startsWith(name.substring(0, 1))) { //since each log level string has unique first character, we will only check if starting character match
					index = i+2;
					break;
				}
				i++;
			}
		}

		return index;
	}
}
