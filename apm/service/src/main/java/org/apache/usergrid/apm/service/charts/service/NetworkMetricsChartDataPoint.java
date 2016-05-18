package org.apache.usergrid.apm.service.charts.service;

import java.util.Date;

public class NetworkMetricsChartDataPoint implements DataPoint {
	
	private Long minLatency;
	private Long maxLatency;
	private Long avgLatency;
	
	private Long minServerLatency;
	private Long maxServerLatency;
	private Long avgServerLatency;
	
	private Long errors;
	private Long samples;
	private Date timestamp;

	
	public NetworkMetricsChartDataPoint () {
		
	}	
	

	public Long getMinLatency() {
		return minLatency;
	}

	public void setMinLatency(Long minLatency) {
		this.minLatency = minLatency;
	}

	public Long getMaxLatency() {
		return maxLatency;
	}

	public void setMaxLatency(Long maxLatency) {
		this.maxLatency = maxLatency;
	}

	public Long getAvgLatency() {
		return avgLatency;
	}

	public void setAvgLatency(Long avgLatency) {
		this.avgLatency = avgLatency;
	}

	public Long getErrors() {
		return errors;
	}

	public void setErrors(Long errors) {
		this.errors = errors;
	}

	public Long getSamples() {
		return samples;
	}

	public void setSamples(Long samples) {
		this.samples = samples;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
		
	}	
	
	public Long getMinServerLatency() {
		return minServerLatency;
	}


	public void setMinServerLatency(Long minServerLatency) {
		this.minServerLatency = minServerLatency;
	}


	public Long getMaxServerLatency() {
		return maxServerLatency;
	}


	public void setMaxServerLatency(Long maxServerLatency) {
		this.maxServerLatency = maxServerLatency;
	}


	public Long getAvgServerLatency() {
		return avgServerLatency;
	}


	public void setAvgServerLatency(Long avgServerLatency) {
		this.avgServerLatency = avgServerLatency;
	}


	public String toString() {
		return timestamp.toString() + " AvL: " + avgLatency + " NS: " + samples + " NE: " + errors;
	}
	
	

	
}
