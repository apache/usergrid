package org.apache.usergrid.apm.service.charts.filter;


public class AppsFilter extends EqualFilter {	
		
	public AppsFilter (Long appId) {
		propertyName = "appId";
		propertyValue = appId;
	}
	
	public static void main (String [] args)  {
		AppsFilter fil = new AppsFilter(10l);
		System.out.println (fil.getCriteria());
	}
}
