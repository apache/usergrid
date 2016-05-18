package org.apache.usergrid.apm.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


public class ClientMetricsEnvelope implements Serializable {

	
	private static final long serialVersionUID = 1L;

	Long instaOpsApplicationId;
	
	String orgName;
	
	String appName;
	
	String fullAppName;
	
	Date timeStamp;

	ClientSessionMetrics sessionMetrics;
	List<ClientLog> logs;
	List<ClientNetworkMetrics> metrics;

	
		
	public Long getInstaOpsApplicationId() {
		return instaOpsApplicationId;
	}

	public void setInstaOpsApplicationId(Long instaOpsApplicationId) {
		this.instaOpsApplicationId = instaOpsApplicationId;
	}

	public String getOrgName() {
		return orgName;
	}

	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}	

	public String getFullAppName() {
		if (fullAppName == null)
			return orgName+"_"+appName;
		return fullAppName;
	}

	public void setFullAppName(String fullAppName) {
		this.fullAppName = fullAppName;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date endTime) {
		this.timeStamp = endTime;
	}

	public List<ClientNetworkMetrics> getMetrics() {
		return metrics;
	}

	public void setMetrics(List<ClientNetworkMetrics> metrics) {
		this.metrics = metrics;
	}

	public List<ClientLog> getLogs() {
		return logs;
	}

	public void setLogs(List<ClientLog> logs) {
		this.logs = logs;
	}	
	public ClientSessionMetrics getSessionMetrics() {
		return sessionMetrics;
	}

	public void setSessionMetrics(ClientSessionMetrics sessionMetrics) {
		this.sessionMetrics = sessionMetrics;
	}
}
