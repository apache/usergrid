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

import java.util.List;

import junit.framework.TestCase;

import org.apache.usergrid.apm.service.charts.service.SessionMetricsChartUtil;
import org.apache.usergrid.apm.service.charts.service.SqlOrderGroupWhere;
import org.apache.usergrid.apm.model.ChartCriteria.LastX;
import org.apache.usergrid.apm.model.SessionChartCriteria;
import org.apache.usergrid.apm.service.ServiceFactory;


public class SessionChartCriteriaTest extends TestCase
{
   public void testSaveChartCriteria() {
      SessionChartCriteria cq = new SessionChartCriteria ();
      cq.setAppId(1l);
      cq.setChartName("my super session chart crit");      
      cq.setLastX(LastX.LAST_HOUR);
      cq.setGroupedByApp(true);
      Long id = ServiceFactory.getSessionChartCriteriaService().saveChartCriteria(cq);
      assertTrue("Chart Criteria was saved with id " + id, id != null);
      assertTrue ("Has grouped by app", ServiceFactory.getSessionChartCriteriaService().getChartCriteria(id).isGroupedByApp());
      
      
   }
   
   public void testGetDefaultChartsForAPP() {
      ServiceFactory.getSessionChartCriteriaService().saveDefaultChartCriteriaForApp(20L);
      List <SessionChartCriteria> list = ServiceFactory.getSessionChartCriteriaService().getDefaultChartCriteriaForApp(20L);
      assertEquals("no of default chart criteria for app", 6, list.size());
      
   } 
   
   public void testSessionChartCriteriaGroupingSimple () {
      SessionChartCriteria cq = new SessionChartCriteria ();
      cq.setAppId(1l);
      cq.setChartName("my super session chart crit");      
      cq.setLastX(LastX.LAST_DAY);      
      SqlOrderGroupWhere og = SessionMetricsChartUtil.getOrdersAndGroupings(cq);
      assertTrue("Correct grouping is there ", og.groupBy.equals("endHour"));
      assertTrue("Correct ordering is there ", og.orderBy.equals("endHour"));
   }
   
   public void testSessionChartCriteriaGroupingWithRealGrouping() {
      SessionChartCriteria cq = new SessionChartCriteria ();
      cq.setAppId(1l);
      cq.setChartName("my super session chart crit");      
      cq.setLastX(LastX.LAST_HOUR);
      cq.setGroupedByApp(true);
      SqlOrderGroupWhere og = SessionMetricsChartUtil.getOrdersAndGroupings(cq);
      assertTrue("Correct grouping is there ", og.groupBy.equals("appId,endMinute"));
      assertTrue("Correct ordering is there ", og.orderBy.equals("appId,endMinute"));
   }
   
   
   
}
