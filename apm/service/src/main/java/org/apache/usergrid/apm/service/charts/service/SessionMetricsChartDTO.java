package org.apache.usergrid.apm.service.charts.service;

import java.util.ArrayList;
import java.util.List;

public class SessionMetricsChartDTO implements ChartDTO<SessionDataPoint>
{

   private String chartGroupName;

   private List<SessionDataPoint> datapoints;

   public SessionMetricsChartDTO () {
      datapoints = new ArrayList<SessionDataPoint>();

   }

   public String getChartGroupName() {
      return chartGroupName;
   }

   public void setChartGroupName(String chartGroupName) {
      this.chartGroupName = chartGroupName;
   }

   public List<SessionDataPoint> getDatapoints() {
      return datapoints;
   }

   public void setDatapoints(List<SessionDataPoint> datapoints) {
      this.datapoints = datapoints;
   }

   public void addDataPoint (SessionDataPoint dp)  {
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
