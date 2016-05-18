package org.apache.usergrid.apm.service.charts.service;

import java.util.List;

import com.ideawheel.portal.model.MetricsChartCriteria;

public interface ChartStrategy {
	
	public List<NetworkMetricsChartDTO> getChartData(MetricsChartCriteria cq);

}
