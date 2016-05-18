package org.apache.usergrid.apm.service.charts.filter;

public class HasErrorFilter extends BiggerThanFilter {
	
	public HasErrorFilter () {
		propertyName = "numErrors";
		propertyValue = 0L;
	}

}
