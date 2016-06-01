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

import java.util.Date;

public class LogDataPoint implements DataPoint
{
	Date timestamp;

	private Long assertCount;
	private Long errorCount;
	private Long warnCount;
	private Long infoCount;
	private Long debugCount;
	private Long verboseCount;
	private Long errorAndAboveCount;  
	private Long eventCount;
	private Long crashCount;

	public Date getTimestamp()
	{
		return timestamp;
	}


	public void setTimestamp(Date timestamp)
	{
		this.timestamp = timestamp;
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

	public Long getEventCount()
	{
		eventCount = valueOf(errorAndAboveCount) + valueOf(warnCount)+valueOf(infoCount)+valueOf(debugCount)+valueOf(verboseCount); 

		return eventCount;
	}

	public Long getCrashCount() {
		return crashCount;
	}


	public void setCrashCount(Long crashCount) {
		this.crashCount = crashCount;
	}


	public void setEventCount(Long eventCount)
	{
		this.eventCount = eventCount;
	}  

	private static final long valueOf(Long number) {
		return (number==null)?0:number.longValue();
	}
}
