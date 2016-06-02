/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
