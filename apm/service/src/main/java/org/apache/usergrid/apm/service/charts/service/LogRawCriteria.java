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
