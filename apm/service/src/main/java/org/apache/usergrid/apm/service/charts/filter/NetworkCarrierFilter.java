package org.apache.usergrid.apm.service.charts.filter;


public class NetworkCarrierFilter extends EqualFilter{
	
	public NetworkCarrierFilter (String networkCarrier) {
		propertyName = "networkCarrier";
		propertyValue = networkCarrier;		
	}
}
