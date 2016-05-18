package com.apigee.apm.rest;


public class StatusBean {

	private Long numActiveApps;

	private Long numUsers;

	private Long clientSessionMetricsCount;

	private Long compactSessionMetricsCount;


	private Long queryTimeInMilliSeconds; //in milliseconds


	private String overallStatus;


	public String getOverallStatus() {
		return overallStatus;
	}
	public void setOverallStatus(String overallStatus) {
		this.overallStatus = overallStatus;
	}

	public Long getNumActiveApps() {
		if (null == numActiveApps)
			return 0L;
		return numActiveApps;
	}

	public void setNumActiveApps(Long numActiveApps) {
		this.numActiveApps = numActiveApps;
	}

	public Long getNumUsers() {
		if (null == numUsers)
			return 0L;
		return numUsers;
	}

	public void setNumUsers(Long numUsers) {
		this.numUsers = numUsers;
	}

	public Long getClientSessionMetricsCount() {
		if (null == clientSessionMetricsCount)
			return 0L;
		return clientSessionMetricsCount;
	}

	public void setClientSessionMetricsCount(Long clientSessionMetricsCount) {
		this.clientSessionMetricsCount = clientSessionMetricsCount;
	}

	public Long getCompactSessionMetricsCount() {
		if (compactSessionMetricsCount == null)
			return 0L;
		return compactSessionMetricsCount;
	}

	public void setCompactSessionMetricsCount(Long compactSessionMetricsCount) {
		this.compactSessionMetricsCount = compactSessionMetricsCount;
	}

	public Long getQueryTimeInMilliSeconds() {
		if (null == queryTimeInMilliSeconds)
			return 0L;
		return queryTimeInMilliSeconds;
	}
	public void setQueryTimeInMilliSeconds(Long queryTimeInMilliSeconds) {
		this.queryTimeInMilliSeconds = queryTimeInMilliSeconds;
	}



}
