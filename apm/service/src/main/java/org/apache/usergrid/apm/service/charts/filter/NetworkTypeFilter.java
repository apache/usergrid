package org.apache.usergrid.apm.service.charts.filter;


public class NetworkTypeFilter extends EqualFilter {
	
	public NetworkTypeFilter (String netType) {
		propertyName = "networkType";
		propertyValue = netType;
	}

}
