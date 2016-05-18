package org.apache.usergrid.apm.service.charts.service;

import com.ideawheel.portal.model.LogChartCriteria;

public class CrashRawCriteria extends RawCriteria<LogChartCriteria>
{



	String crashSummary;
	
	String crashMetaData;
	
	public CrashRawCriteria(LogChartCriteria cq)
	{
		super(cq);
		// TODO Auto-generated constructor stub
	}

	public String getCrashSummary() {
		return crashSummary;
	}

	public void setCrashSummary(String crashSummary) {
		this.crashSummary = crashSummary;
	}

	public String getCrashMetaData() {
		return crashMetaData;
	}

	public void setCrashMetaData(String crashMetaData) {
		this.crashMetaData = crashMetaData;
	}
	
	
	
	
		



}
