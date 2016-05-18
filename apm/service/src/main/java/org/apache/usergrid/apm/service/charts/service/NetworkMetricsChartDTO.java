package org.apache.usergrid.apm.service.charts.service;

import java.util.ArrayList;
import java.util.List;

public class NetworkMetricsChartDTO implements ChartDTO<NetworkMetricsChartDataPoint> {


	private String chartGroupName;

	private List<NetworkMetricsChartDataPoint> datapoints;

	public NetworkMetricsChartDTO () {
		datapoints = new ArrayList<NetworkMetricsChartDataPoint>();


	}

	public String getChartGroupName() {
		return chartGroupName;
	}

	public void setChartGroupName(String chartGroupName) {
		this.chartGroupName = chartGroupName;
	}

	public List<NetworkMetricsChartDataPoint> getDatapoints() {
		return datapoints;
	}

	public void setDatapoints(List<NetworkMetricsChartDataPoint> datapoints) {
		this.datapoints = datapoints;
	}

	public void addDataPoint (NetworkMetricsChartDataPoint dp)  {
		datapoints.add(dp);
	}
	
	public String toString() {
		String string = "ChartGroupName " + chartGroupName + "\n";
		String dp ="";
		for (int i = 0; i < datapoints.size(); i++)
			dp+= datapoints.get(i).toString()+"\n";
		return string+dp;
		
	}



}
