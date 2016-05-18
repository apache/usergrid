package org.apache.usergrid.apm.service.charts.service;

import java.util.Date;

public class SessionDataPoint implements DataPoint
{
   
   Date timestamp;
   
   Long numSessions;
   
   Long numUsers;
   
   Long avgSessionTime;

   public Date getTimestamp()
   {
      return timestamp;
   }

   public void setTimestamp(Date timestamp)
   {
      this.timestamp = timestamp;
   }

   public Long getNumSessions()
   {
      return numSessions;
   }

   public void setNumSessions(Long numSessions)
   {
      this.numSessions = numSessions;
   }

   public Long getNumUsers()
   {
      return numUsers;
   }

   public void setNumUsers(Long numUsers)
   {
      this.numUsers = numUsers;
   }

   public Long getAvgSessionTime()
   {
      return avgSessionTime;
   }

   public void setAvgSessionTime(Long avgSessionTime)
   {
      this.avgSessionTime = avgSessionTime;
   }
}
