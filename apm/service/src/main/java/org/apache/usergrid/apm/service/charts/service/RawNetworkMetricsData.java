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

public class RawNetworkMetricsData extends RawData
{
   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private String url;
   
   private Long numRequests;
   
   private Long avgLatency;
   
   private Long errorCount;
   
   private String devicePlatform;
   
   private String networkCarrier;
   
   private String networkType;

   public String getUrl()
   {
      return url;
   }

   public void setUrl(String url)
   {
      this.url = url;
   }

   public Long getNumRequests()
   {
      return numRequests;
   }

   public void setNumRequests(Long numRequests)
   {
      this.numRequests = numRequests;
   }

   public Long getAvgLatency()
   {
      return avgLatency;
   }

   public void setAvgLatency(Long avgLatency)
   {
      this.avgLatency = avgLatency;
   }

   public Long getErrorCount()
   {
      return errorCount;
   }

   public void setErrorCount(Long errorCount)
   {
      this.errorCount = errorCount;
   }

   public String getDevicePlatform()
   {
      return devicePlatform;
   }

   public void setDevicePlatform(String devicePlatform)
   {
      this.devicePlatform = devicePlatform;
   }

   public String getNetworkCarrier()
   {
      return networkCarrier;
   }

   public void setNetworkCarrier(String networkCarrier)
   {
      this.networkCarrier = networkCarrier;
   }

   public String getNetworkType()
   {
      return networkType;
   }

   public void setNetworkType(String networkType)
   {
      this.networkType = networkType;
   }   

}
