package org.apache.usergrid.apm.service.charts.service;

import java.util.ArrayList;
import java.util.List;

public class LogChartDTO implements ChartDTO<LogDataPoint>
{
   private String chartGroupName;

   private List<LogDataPoint> datapoints;

   public LogChartDTO () {
      datapoints = new ArrayList<LogDataPoint>();

   }

   public String getChartGroupName() {
      return chartGroupName;
   }

   public void setChartGroupName(String chartGroupName) {
      this.chartGroupName = chartGroupName;
   }

   public List<LogDataPoint> getDatapoints() {
      return datapoints;
   }

   public void setDatapoints(List<LogDataPoint> datapoints) {
      this.datapoints = datapoints;
   }

   public void addDataPoint (LogDataPoint dp)  {
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

