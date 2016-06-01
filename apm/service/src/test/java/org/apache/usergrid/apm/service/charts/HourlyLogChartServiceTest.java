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
package org.apache.usergrid.apm.service.charts;

import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;

import junit.framework.TestCase;

import org.apache.usergrid.apm.model.ClientLog;
import org.apache.usergrid.apm.service.charts.service.LogChartDTO;
import org.apache.usergrid.apm.service.charts.service.LogRawCriteria;
import org.apache.usergrid.apm.model.CompactClientLog;
import org.apache.usergrid.apm.model.LogChartCriteria;
import org.apache.usergrid.apm.service.LogDBService;
import org.apache.usergrid.apm.service.LogTestData;
import org.apache.usergrid.apm.service.ServiceFactory;
import org.apache.usergrid.apm.service.service.TestUtil;

public class HourlyLogChartServiceTest extends TestCase
{

   protected void setUp() throws Exception {      
      ServiceFactory.invalidateHibernateSession();
      super.setUp();
   }

   protected void tearDown() throws Exception {
      super.tearDown();
   }

   public void testGetHourlyChartDataAppErrors() {

      //Save a chart criteria first
      ServiceFactory.getLogChartCriteriaService().saveChartCriteria(TestUtil.getLogChartCriteriaForHourly());

      LogChartCriteria cq = ServiceFactory.getLogChartCriteriaService().getChartCriteria(1l);

      assertTrue (cq.getId().equals(1L));

      Calendar startTime = Calendar.getInstance();
      startTime.add(Calendar.HOUR_OF_DAY, -1);

      LogTestData.populateLogForMinutes(4, startTime,1, 1l);

      LogDBService dbService = ServiceFactory.getLogDBService();

      assertEquals("Num Rows after first insertion for 4 minutes", dbService.getCompactLogRowCount(),4);


      List<LogChartDTO> chartData = ServiceFactory.getChartService().getLogChartData(cq);
      assertTrue ("number of chart subgroups is 1 ", chartData.size() ==1);

      //assume that user does the refresh after 10 minutes
      startTime.add(Calendar.MINUTE, 10);

      LogTestData.populateLogForMinutes(3, startTime, 1, 1L);

      int newCount = dbService.getCompactLogRowCount();
      assertEquals("no of rows in compact session metrics table", newCount, 7);

      chartData = ServiceFactory.getChartService().getLogChartData(cq);
      assertTrue ("number of chart subgroups is 1 ", chartData.size() ==1);
      assertTrue ("number of datapoints is 7", chartData.get(0).getDatapoints().size() == 7);
   }
   
   public void testGetHourlyChartDataAppErrorsForYesterday() {

      //Save a chart criteria first
      ServiceFactory.getLogChartCriteriaService().saveChartCriteria(TestUtil.getLogChartCriteriaForHourlyForYesterday());

      LogChartCriteria cq = ServiceFactory.getLogChartCriteriaService().getChartCriteria(1l);

      assertTrue (cq.getId().equals(1L));

      Calendar startTime = Calendar.getInstance();
      startTime.add(Calendar.DAY_OF_YEAR, -1); //go back 24 hour
      startTime.add(Calendar.HOUR_OF_DAY, -1); //then go back 1 hour

      LogTestData.populateLogForMinutes(4, startTime,1, 1l);

      LogDBService dbService = ServiceFactory.getLogDBService();
      assertEquals("Num Rows after first insertion for 4 minutes", dbService.getCompactLogRowCount(),4);

      List<LogChartDTO> chartData = ServiceFactory.getChartService().getLogChartData(cq);
      
      assertTrue ("number of chart subgroups is 1 ", chartData.size() ==1);
      assertEquals ("number of datapoints is 4", 4, chartData.get(0).getDatapoints().size());
   }

   
   public void testGetHourlyChartDataByNetworkCarrier() {

      ServiceFactory.getLogChartCriteriaService().saveChartCriteria(TestUtil.getGroupByNetworkCarrierLogChartCriteriaForHourly());

      LogChartCriteria cq = ServiceFactory.getLogChartCriteriaService().getChartCriteria(1l);

      assertTrue (cq.getId().equals(1L));

      Calendar startTime = Calendar.getInstance();
      startTime.add(Calendar.HOUR_OF_DAY, -1);

      LogTestData.populateLogForMinutes(4, startTime,1, 1l);

      LogDBService dbService = ServiceFactory.getLogDBService();

      assertEquals("Num Rows after first insertion for 4 minutes", dbService.getCompactLogRowCount(),4);


     List<LogChartDTO> chartData = ServiceFactory.getChartService().getLogChartData(cq);
      assertEquals("Number of chart groups", chartData.size(), getNumDifferentCarriers(1L));

      //assume that user does the refresh after 10 minutes
      startTime.add(Calendar.MINUTE, 10);

      LogTestData.populateLogForMinutes(3, startTime, 1, 1L);

      int newCount = dbService.getCompactLogRowCount();
      assertEquals("no of rows in compact session metrics table", newCount, 7);

      chartData = ServiceFactory.getChartService().getLogChartData(cq);
      assertEquals("Number of chart groups", chartData.size(),getNumDifferentCarriers(1L));      
   }
   
   public void testHourlyRawLogData() {
      LogChartCriteria cq = TestUtil.getLogChartCriteriaForHourly();
      LogRawCriteria rq = new LogRawCriteria(cq);
      Calendar startTime = Calendar.getInstance();
      startTime.add(Calendar.HOUR_OF_DAY, -1);

      LogTestData.populateRawLogs(20, startTime,1, 1l);

      LogDBService dbService = ServiceFactory.getLogDBService();
      List<ClientLog> raw = dbService.getRawLogData(rq);

      assertTrue("got some raw data", raw != null);
      for (int i = 0; i < raw.size(); i++) {
         System.out.println (raw.get(i));
      }      
   }
   
   public void testHourlyRawLogDataWithLogMessageFiltering() {
      LogChartCriteria cq = TestUtil.getLogChartCriteriaForHourly();
      LogRawCriteria rq = new LogRawCriteria(cq);
      rq.setLogMessage("error");
      Calendar startTime = Calendar.getInstance();
      startTime.add(Calendar.HOUR_OF_DAY, -1);

      LogTestData.populateRawLogs(20, startTime,1, 1l);

      LogDBService dbService = ServiceFactory.getLogDBService();
      List<ClientLog> raw = dbService.getRawLogData(rq);

      assertTrue("got some raw data", raw != null);
      for (int i = 0; i < raw.size(); i++) {
         System.out.println (raw.get(i));
         assertTrue ("only log with error in message ", raw.get(i).getLogMessage().contains("error"));
      }      
   }
   
   public void testRawLogDataCount () {
      LogChartCriteria cq = TestUtil.getLogChartCriteriaForHourly();
      LogRawCriteria rq = new LogRawCriteria(cq);
      rq.setLogMessage("error");
      Calendar startTime = Calendar.getInstance();
      startTime.add(Calendar.HOUR_OF_DAY, -1);

      LogTestData.populateRawLogs(20, startTime,1, 1l);

      LogDBService dbService = ServiceFactory.getLogDBService();
      int count = dbService.getRawLogDataCount(rq);

      assertTrue ("count of log with error in message ", count > 0);
           
   }
   
   private int getNumDifferentCarriers(Long appId) {
    List<CompactClientLog> ms =  new LogDBService().getCompactClientLogsForApp(appId); 
    Hashtable<String, Integer> table = new Hashtable<String, Integer>();    
    for(CompactClientLog m :ms) {
       table.put(m.getNetworkCarrier(), 0);          
    }
    return table.size();
   }





}
