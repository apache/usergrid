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
