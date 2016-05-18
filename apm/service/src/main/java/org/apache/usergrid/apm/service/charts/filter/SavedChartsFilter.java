package org.apache.usergrid.apm.service.charts.filter;

public class SavedChartsFilter extends EqualFilter {

	public SavedChartsFilter (Long cqId) {
		propertyName = "chartCriteriaId";
		propertyValue = cqId;
	}
}
