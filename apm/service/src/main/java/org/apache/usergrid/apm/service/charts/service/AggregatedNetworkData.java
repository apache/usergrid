package org.apache.usergrid.apm.service.charts.service;

public class AggregatedNetworkData implements AggregatedData
{
   /**
    * 
    */
   private static final long serialVersionUID = 1L;

 
   private Long avgLatency;
   
   private Long maxLatency;
   
   private Long totalRequests;
   
   private Long totalErrors;

 

   public Long getAvgLatency()
   {
      return avgLatency;
   }

   public void setAvgLatency(Long avgLatency)
   {
      this.avgLatency = avgLatency;
   }

   public Long getMaxLatency()
   {
      return maxLatency;
   }

   public void setMaxLatency(Long maxLatency)
   {
      this.maxLatency = maxLatency;
   }

   public Long getTotalRequests()
   {
      return totalRequests;
   }

   public void setTotalRequests(Long totalRequests)
   {
      this.totalRequests = totalRequests;
   }

   public Long getTotalErrors()
   {
      return totalErrors;
   }

   public void setTotalErrors(Long totalErrors)
   {
      this.totalErrors = totalErrors;
   }

@Override
public Long getTotalMetricsCount() {
	return totalRequests;
}
   
   
}
