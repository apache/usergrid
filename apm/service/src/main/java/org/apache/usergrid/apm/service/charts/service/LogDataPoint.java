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
