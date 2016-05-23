package org.apache.usergrid.apm.service.charts.service;

import java.util.List;

import org.apache.usergrid.apm.model.MetricsChartCriteria;

public interface ChartStrategy {
	
	public List<NetworkMetricsChartDTO> getChartData(MetricsChartCriteria cq);

}
