package com.apigee.apm.rest;

import java.util.List;

import org.apache.usergrid.apm.service.charts.service.AggregatedNetworkData;
import org.apache.usergrid.apm.service.charts.service.AttributeValueChartData;
import org.apache.usergrid.apm.service.charts.service.NetworkRequestsErrorsChartData;

/**
 * 
 * @author ApigeeCorporation
 * This is different from Log and Session Chart data wrapped because for network, we show two different charts.
 * 
 */
public class NetworkChartDataWrapper {
	
	AggregatedNetworkData summaryData;
	List<AttributeValueChartData> responseTimes;
	List<NetworkRequestsErrorsChartData> requestErrorCounts;

	public AggregatedNetworkData getSummaryData() {
		return summaryData;
	}

	public void setSummaryData(AggregatedNetworkData summaryData) {
		this.summaryData = summaryData;
	}	

	public List<AttributeValueChartData> getResponseTimes() {
		return responseTimes;
	}

	public void setResponseTimes(List<AttributeValueChartData> responseTimes) {
		this.responseTimes = responseTimes;
	}

	public List<NetworkRequestsErrorsChartData> getRequestErrorCounts() {
		return requestErrorCounts;
	}

	public void setRequestErrorCounts(
			List<NetworkRequestsErrorsChartData> requestErrorCounts) {
		this.requestErrorCounts = requestErrorCounts;
	}



}
