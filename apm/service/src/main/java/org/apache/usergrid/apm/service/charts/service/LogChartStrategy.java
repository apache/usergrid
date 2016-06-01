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
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.usergrid.apm.service.charts.filter.SpecialTimeFilter;
import org.apache.usergrid.apm.model.CompactClientLog;
import org.apache.usergrid.apm.model.LogChartCriteria;
import org.apache.usergrid.apm.service.ServiceFactory;

public class LogChartStrategy
{

   private static final Log log = LogFactory.getLog(LogChartStrategy.class);

   public List<LogChartDTO> getChartData(LogChartCriteria cq) {

      log.info ("Getting app diagnostics chart data for " + cq.getChartName() + " " + cq.getAppId() );

      List<LogChartDTO> chartData = null;
      List<CompactClientLog> logs = null;

      SpecialTimeFilter tf = new SpecialTimeFilter(cq);
      Long from = tf.getFrom();
      Long to = tf.getTo();

      log.info("Looking for app diagnostics data for " + tf.getEndPropName() + " between " + from + " and " + to );

      //logs = ServiceFactory.getLogDBService().getCompactClientLogUsingNativeSqlQuery(cq);
      //following  is different from above call. Above assumes that there is only going to be one row per endMinute. This is 
      //does summation when multiple rows exist for a given end*
      logs = ServiceFactory.getLogDBService().getNewCompactClientLogUsingNativeSqlQuery(cq);
      log.info("number of total compact log " + logs.size());
      chartData = compileChartData ( logs, cq);

      
      return chartData;
   }


   protected List<LogChartDTO> compileChartData (List<CompactClientLog>  logs, LogChartCriteria cq ) {

      //the simple case
      if (!cq.hasGrouping()) {
         return compileChartDataWithNoGrouping(logs, cq);

      } else {
         return compileChartDataWithGrouping(logs, cq);

      }
   }

   protected List<LogChartDTO> compileChartDataWithNoGrouping (List<CompactClientLog>  logs, LogChartCriteria cq ) {

      List <LogChartDTO> charts = new ArrayList<LogChartDTO>();     
      LogChartDTO dto = new LogChartDTO ();
      dto.setChartGroupName("N/A"); //not applicable
      populateUIChartData( dto, logs, cq);
      charts.add(dto);
      return charts;

   }

   public List<LogChartDTO> compileChartDataWithGrouping (List<CompactClientLog>  logs, LogChartCriteria cq ) {
      String propName = new SpecialTimeFilter(cq).getEndPropName();
      List <LogChartDTO> charts = new ArrayList<LogChartDTO>();
      log.info("Populating log chart dto with group based on end* " + propName);
      String previousGroup="previous";     
      String currentGroup = "";
      LogChartDTO dto = null;
      LogDataPoint dp = null;
      for (CompactClientLog m: logs) {
         if (cq.isGroupedByApp())        
            currentGroup = m.getAppId().toString();
         else if (cq.isGroupedByNetworkType())
            currentGroup = m.getNetworkType();
         else if (cq.isGroupedByNetworkCarrier())        
            currentGroup = m.getNetworkCarrier();
         else if (cq.isGroupedByAppVersion()) 
            currentGroup = m.getApplicationVersion();
         else if (cq.isGroupedByAppConfigType()) 
            currentGroup = m.getAppConfigType().toString();
         else if (cq.isGroupedByDeviceModel())  
            currentGroup = m.getDeviceModel();
         else if (cq.isGroupedbyDevicePlatform())
            currentGroup = m.getDevicePlatform();
         else if (cq.isGroupedByDeviceOS()) //aka platform version
             currentGroup = m.getDeviceOperatingSystem();

         if (!currentGroup.equals(previousGroup)) {
            dto = new LogChartDTO();
            dto.setChartGroupName(currentGroup);           
            charts.add(dto);
            previousGroup = currentGroup;
         }

         dp = new LogDataPoint();
         dp.setAssertCount(m.getAssertCount());
         dp.setErrorCount(m.getErrorCount());
         dp.setDebugCount(m.getDebugCount());
         dp.setErrorAndAboveCount(m.getErrorAndAboveCount());
         dp.setInfoCount(m.getInfoCount());
         dp.setVerboseCount(m.getVerboseCount());
         dp.setWarnCount(m.getWarnCount());
         dp.setCrashCount(m.getCrashCount());
         if ("endMinute".equals(propName))
            dp.setTimestamp(new Date(m.getEndMinute()*60*1000));
         else if ("endHour".equals(propName))
            dp.setTimestamp(new Date(m.getEndHour()*60*60*1000));
         else if ("endDay".equals(propName))
            dp.setTimestamp(new Date(m.getEndDay()*24*60*60*1000)); 
         dto.addDataPoint(dp);
      }
      return charts;

   }



   protected void populateUIChartData(LogChartDTO dto, List<CompactClientLog> logs, LogChartCriteria cq) {     

      LogDataPoint dp;
      CompactClientLog tempLog;
      String propName = new SpecialTimeFilter(cq).getEndPropName();
      log.info("Populating log chart dto with no group based on end* " + propName);
      if (logs != null) {
         for (int i = 0; i < logs.size(); i++) {
            tempLog = logs.get(i);
            dp = new LogDataPoint();
            dp.setAssertCount(tempLog.getAssertCount());
            dp.setErrorCount(tempLog.getErrorCount());
            dp.setDebugCount(tempLog.getDebugCount());
            dp.setErrorAndAboveCount(tempLog.getErrorAndAboveCount());
            dp.setInfoCount(tempLog.getInfoCount());
            dp.setVerboseCount(tempLog.getVerboseCount());
            dp.setWarnCount(tempLog.getWarnCount());
            dp.setCrashCount(tempLog.getCrashCount());
            if ("endMinute".equals(propName))
               dp.setTimestamp(new Date(tempLog.getEndMinute()*60*1000));
            else if ("endHour".equals(propName))
               dp.setTimestamp(new Date(tempLog.getEndHour()*60*60*1000));
            else if ("endDay".equals(propName))
               dp.setTimestamp(new Date(tempLog.getEndDay()*24*60*60*1000));         

            dto.addDataPoint(dp);
         }
      }
   }

}
