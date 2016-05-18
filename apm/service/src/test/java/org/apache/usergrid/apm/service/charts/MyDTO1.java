package org.apache.usergrid.apm.service.charts;

public class MyDTO1 {
	private Long minute;
	private Long totSamples;
	private String networkCarrier;
	
	public MyDTO1 () {
		
	}
	public Long getMinute() {
		return minute;
	}
	public void setMinute(Long minute) {
		this.minute = minute;
	}
	public Long getTotSamples() {
		return totSamples;
	}
	public void setTotSamples(Long totSamples) {
		this.totSamples = totSamples;
	}
	public String getNetworkCarrier() {
		return networkCarrier;
	}
	public void setNetworkCarrier(String networkCarrier) {
		this.networkCarrier = networkCarrier;
	}
	
	

}

