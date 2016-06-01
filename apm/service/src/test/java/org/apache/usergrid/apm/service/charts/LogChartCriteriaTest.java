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

import org.apache.usergrid.apm.model.ChartCriteria.LastX;
import org.apache.usergrid.apm.model.LogChartCriteria;
import org.apache.usergrid.apm.service.ServiceFactory;


public class LogChartCriteriaTest extends TestCase
{
   public void testSaveChartCriteria() {
      LogChartCriteria cq = new LogChartCriteria();
      cq.setAppId(1l);
      cq.setChartName("my super app diagnostic chart crit");      
      cq.setLastX(LastX.LAST_HOUR);
      cq.setGroupedByAppConfigType(true);
      cq.setErrorAndAboveCount(1l);
      Long id = ServiceFactory.getLogChartCriteriaService().saveChartCriteria(cq);
      assertTrue("Chart Criteria was saved with id " + id, id != null);
      assertTrue ("Has grouped by app", ServiceFactory.getLogChartCriteriaService().getChartCriteria(id).isGroupedByAppConfigType());
      
      
   }
   
   public void testGetDefaultChartsForAPP() {
      ServiceFactory.getLogChartCriteriaService().saveDefaultChartCriteriaForApp(20L);
      List <LogChartCriteria> list = ServiceFactory.getLogChartCriteriaService().getDefaultChartCriteriaForApp(20L);
      assertEquals("no of default chart criteria for app", 5, list.size());
      
   }  
   
}

