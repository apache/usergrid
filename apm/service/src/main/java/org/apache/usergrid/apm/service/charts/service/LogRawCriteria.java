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

import org.apache.usergrid.apm.model.LogChartCriteria;

public class LogRawCriteria extends RawCriteria<LogChartCriteria>
{



	String logMessage;

	String logLevel;

	boolean excludeCrash;
	
	String tag;


	public LogRawCriteria(LogChartCriteria cq)
	{
		super(cq);
		// TODO Auto-generated constructor stub
	}

	public boolean isExcludeCrash() {
		return excludeCrash;
	}
	public void setExcludeCrash(boolean excludeCrash) {
		this.excludeCrash = excludeCrash;
	}




	public String getLogMessage()
	{
		return logMessage;
	}

	public void setLogMessage(String logMessage)
	{
		this.logMessage = logMessage;
	}

	public String getLogLevel()
	{
		return logLevel;
	}

	public void setLogLevel(String logLevel)
	{
		this.logLevel = logLevel;
	}


	public String getTag() {
		return tag;
	}


	public void setTag(String tag) {
		this.tag = tag;
	}
	
	



}
