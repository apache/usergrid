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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.usergrid.apm.model.CompactNetworkMetrics;
import org.apache.usergrid.apm.model.MetricsChartCriteria;


public abstract class CommonStrategy {


   private static final Log log = LogFactory.getLog(CommonStrategy.class);

   /**new ArrayList <NetworkMetricsChartDTO> ();
    * Algo:
    * Find if there is any grouping.
    * 	- If there is, split the metrics into different buckets
    *  - If not, return an array list containing one NetworkMetricsChartDTO 
    * @param cachedMetrics
    * @param freshMetrics
    * @param cq
    * @return
    */
   protected List<NetworkMetricsChartDTO> compileChartData (List<? extends CompactNetworkMetrics>  cachedMetrics, List<? extends CompactNetworkMetrics> freshMetrics, MetricsChartCriteria cq ) {

      //the simple case
      if (!cq.hasGrouping()) {
         return compileChartDataWithNoGrouping(cachedMetrics, freshMetrics, cq);

      } else {
         return compileChartDataWithGrouping(cachedMetrics, freshMetrics, cq);
      }
   }

   protected List<NetworkMetricsChartDTO> compileChartDataWithNoGrouping (List<? extends CompactNetworkMetrics>  cachedMetrics, List<? extends CompactNetworkMetrics> freshMetrics, MetricsChartCriteria cq ) {

      List <NetworkMetricsChartDTO> charts = new ArrayList<NetworkMetricsChartDTO>();		
      NetworkMetricsChartDTO dto = new NetworkMetricsChartDTO ();
      dto.setChartGroupName("N/A"); //not applicable
      populateUIChartData(dto, cachedMetrics, cq.getId());
      populateUIChartData(dto, freshMetrics, cq.getId());
      charts.add(dto);
      return charts;

   }

   public List<NetworkMetricsChartDTO> compileChartDataWithGrouping (List<? extends CompactNetworkMetrics>  cachedMetrics, List<? extends CompactNetworkMetrics> freshMetrics, MetricsChartCriteria cq ) {		
      Hashtable <String, Integer> groupIndex = getChartGroups (cachedMetrics, cq);		
      //We need to do this because there could be new group such as Sprint network while cached ones only has ATT and Verizon
      Hashtable <String, Integer> groupIndexFresh = getChartGroups (freshMetrics, cq);
      Enumeration <String> keys = groupIndexFresh.keys();
      while (keys.hasMoreElements()) {
         String temp = keys.nextElement();
         if(!groupIndex.containsKey(temp))
            groupIndex.put(temp, groupIndex.size());
      }

      log.info("Total number of chart groups " + groupIndex.size());	
      //TODO: There probably is a better way
      List <NetworkMetricsChartDTO> charts = new ArrayList<NetworkMetricsChartDTO>(groupIndex.size());
      for (int i = 0; i < groupIndex.size(); i++)  {
         charts.add(new NetworkMetricsChartDTO());
      }

      keys = groupIndex.keys();
      String key;
      NetworkMetricsChartDTO dto;
      while (keys.hasMoreElements()) {
         key = keys.nextElement();
         dto = charts.get(groupIndex.get(key));
         dto.setChartGroupName(key);			
      }

      populateUIChartDataWithGroup(charts, cachedMetrics, groupIndex, cq);
      populateUIChartDataWithGroup(charts, freshMetrics, groupIndex, cq);		
      return charts;

   }


   public Hashtable<String, Integer> getChartGroups(List<? extends CompactNetworkMetrics> metrics, MetricsChartCriteria cq) {

      //List<CompactNetworkMetrics> metrics = (List<CompactNetworkMetrics>) m;

      Hashtable <String, Integer> groupIndex = new Hashtable<String,Integer> ();
      String previousGroup = "";
      int index = 0;
      if (cq.isGroupedByApp()) {
         for (int i = 0; i < metrics.size(); i++)  {
            if (!metrics.get(i).getAppId().toString().equals(previousGroup)) {
               previousGroup = metrics.get(i).getAppId().toString(); 
               groupIndex.put(previousGroup, index);
               index++;
            }
         }

      }
      else if (cq.isGroupedByNetworkCarrier()) {
         for (int i = 0; i < metrics.size(); i++)  {
            if (!metrics.get(i).getNetworkCarrier().equals(previousGroup)) {
               previousGroup = metrics.get(i).getNetworkCarrier(); 
               groupIndex.put(previousGroup, index);
               index++;
            }
         }
      }
      else if (cq.isGroupedByNetworkType()) {
         for (int i = 0; i < metrics.size(); i++)  {
            if (!metrics.get(i).getNetworkType().equals(previousGroup)) {
               previousGroup = metrics.get(i).getNetworkType(); 
               groupIndex.put(previousGroup, index);
               index++;
            }
         }
      }
      else if (cq.isGroupedByAppVersion()) {
         for (int i = 0; i < metrics.size(); i++)  {
            if (!metrics.get(i).getApplicationVersion().equals(previousGroup)) {
               previousGroup = metrics.get(i).getApplicationVersion(); 
               groupIndex.put(previousGroup, index);
               index++;
            }
         }
      }
      else if (cq.isGroupedByAppConfigType()) {
         for (int i = 0; i < metrics.size(); i++)  {
            if (!metrics.get(i).getAppConfigType().toString().equals(previousGroup)) {
               previousGroup = metrics.get(i).getAppConfigType().toString(); 
               groupIndex.put(previousGroup, index);
               index++;
            }
         }
      }
    /*  else if (cq.isGroupedByDeviceModel()) {
         for (int i = 0; i < metrics.size(); i++)  {
            if (!metrics.get(i).getDeviceModel().equals(previousGroup)) {
               previousGroup = metrics.get(i).getDeviceModel(); 
               groupIndex.put(previousGroup, index);
               index++;
            }
         }
      }

      else if (cq.isGroupedbyDevicePlatform()) {
         for (int i = 0; i < metrics.size(); i++)  {
            if (!metrics.get(i).getDevicePlatform().equals(previousGroup)) {
               previousGroup = metrics.get(i).getDevicePlatform(); 
               groupIndex.put(previousGroup, index);
               index++;
            }
         }
      }*/
      return groupIndex;
   }


   //protected abstract Hashtable<String, Integer> getChartGroups (List<? extends CompactNetworkMetrics> metrics, ChartCriteria cq);
   protected abstract void populateUIChartData(NetworkMetricsChartDTO dto, List<? extends CompactNetworkMetrics> metrics, Long chartCriteriaId) ;
   protected abstract void populateUIChartDataWithGroup(List<NetworkMetricsChartDTO> dtos, List<? extends CompactNetworkMetrics> metrics, Hashtable <String, Integer> groupIndex, MetricsChartCriteria cq);
   protected abstract Long findTheLatestTime(List<? extends CompactNetworkMetrics> metrics);
}



