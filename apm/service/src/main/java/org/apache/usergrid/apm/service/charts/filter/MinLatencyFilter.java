package org.apache.usergrid.apm.service.charts.filter;

public class MinLatencyFilter extends BiggerThanFilter {
	
	public MinLatencyFilter() {
		propertyName = "minLatency";
		propertyValue = 0L;
	}
	
	public MinLatencyFilter (Object value) {
		propertyName = "minLatency";
		propertyValue = value;
	}

}
