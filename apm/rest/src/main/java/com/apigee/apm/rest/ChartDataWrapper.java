package com.apigee.apm.rest;

import java.util.List;

/**
 * 
 * @author ApigeeCorporation
 *
 * @param <S> Aggregated{Session, Log, NetworkMetrics}Data
 * @param <D> {Log, Session, NetworkMetrics}ChartDTO
 */
public class ChartDataWrapper<S,D> {
	S summaryData;
	List<D> chartData;
	
	public S getSummaryData() {
		return summaryData;
	}
	public void setSummaryData(S summaryData) {
		this.summaryData = summaryData;
	}
	public List<D> getChartData() {
		return chartData;
	}
	public void setChartData(List<D> chartData) {
		this.chartData = chartData;
	}
	
	
	

}
