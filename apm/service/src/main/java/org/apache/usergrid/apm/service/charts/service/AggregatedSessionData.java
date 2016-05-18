package org.apache.usergrid.apm.service.charts.service;


public class AggregatedSessionData implements AggregatedData
{
   
   /**
    * 
    */
   private static final long serialVersionUID = 1L;


   private Number totalSessions;
   
   private Number maxConcurrentSessions;   
   
   private Number totalUniqueUsers;
   
   private Number avgSessionLength;
   


   public Number getTotalSessions()
   {
      return totalSessions!=null?totalSessions.longValue():0;
   }


   public void setTotalSessions(Number totalSessions)
   {
      this.totalSessions = totalSessions;
   }


   public Number getMaxConcurrentSessions()
   {
      return maxConcurrentSessions!=null?maxConcurrentSessions.longValue():0;
   }


   public void setMaxConcurrentSessions(Number maxConcurrentSessions)
   {
      this.maxConcurrentSessions = maxConcurrentSessions;
   }


   public Number getTotalUniqueUsers()
   {
      return totalUniqueUsers!=null
             ?totalUniqueUsers.longValue():0;
   }


   public void setTotalUniqueUsers(Number totalUniqueUsers)
   {
      this.totalUniqueUsers = totalUniqueUsers;
   }


   public Number getAvgSessionLength()
   {
      return avgSessionLength!=null?avgSessionLength.longValue():0;
   }


   public void setAvgSessionLength(Number avgSessionLength)
   {
      this.avgSessionLength = avgSessionLength;
   }
   
   public String toString () {
      return "Aggregate Session Data " + 
       " totalSession " + totalSessions + " avg session length" + avgSessionLength
      + " uniqueUsers " + totalUniqueUsers;      
      
   }


@Override
public Long getTotalMetricsCount() {
	return totalSessions.longValue();
}
   
   
}
