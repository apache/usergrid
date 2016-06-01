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
