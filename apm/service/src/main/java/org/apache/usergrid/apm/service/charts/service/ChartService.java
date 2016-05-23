package org.apache.usergrid.apm.service.charts.service;

import java.util.List;

import org.apache.usergrid.apm.model.LogChartCriteria;
import org.apache.usergrid.apm.model.MetricsChartCriteria;
import org.apache.usergrid.apm.model.SessionChartCriteria;


public interface ChartService {
	
   /** 
    * chart graph data
    * @param criteria
    * @return
    */
	List<NetworkMetricsChartDTO> getNetworkMetricsChartData (MetricsChartCriteria criteria); 
	List<SessionMetricsChartDTO> getSessionMetricsChartData(SessionChartCriteria criteria);	
   List<LogChartDTO> getLogChartData(LogChartCriteria criteria);

   
   
   //chart raw data
   //even though whole chart criteria is coming up, for M2, we are only going to filter based on
   //appId and time range.Regardless of chart criteria, raw data will remain same for now;
   
   //List<kjflsajf> getRawNetworkData (MetricsChartCriteria critera);

}
