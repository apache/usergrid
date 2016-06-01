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
import java.util.List;

import junit.framework.TestCase;

import org.apache.usergrid.apm.service.charts.service.NetworkMetricsChartDTO;
import org.apache.usergrid.apm.model.MetricsChartCriteria;
import org.apache.usergrid.apm.service.NetworkMetricsDBService;
import org.apache.usergrid.apm.service.NetworkTestData;
import org.apache.usergrid.apm.service.ServiceFactory;
import org.apache.usergrid.apm.service.service.TestUtil;

public class HourlyNetworkMetricsChartServiceTest extends TestCase
{

	protected void setUp() throws Exception {      
		ServiceFactory.invalidateHibernateSession();
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGetHourlyChartDataNetworkOverview() {

		//Save a chart criteria first
		ServiceFactory.getNetworkMetricsChartCriteriaService().saveChartCriteria(TestUtil.getChartCriteriaForHourly());

		MetricsChartCriteria cq = ServiceFactory.getNetworkMetricsChartCriteriaService().getChartCriteria(1l);

		assertTrue (cq.getId().equals(1L));

		Calendar startTime = Calendar.getInstance();
		startTime.add(Calendar.HOUR_OF_DAY, -1);

		NetworkTestData.populateCompactNetworkMetricsForMinutes(4, startTime,1, 1l);


		NetworkMetricsDBService dbService = ServiceFactory.getMetricsDBServiceInstance();      


		List<NetworkMetricsChartDTO> chartData = ServiceFactory.getChartService().getNetworkMetricsChartData(cq);
		assertTrue ("number of chart subgroups is 1 ", chartData.size() ==1);

		//assume that user does the refresh after 10 minutes
		startTime.add(Calendar.MINUTE, 10);

		NetworkTestData.populateCompactNetworkMetricsForMinutes(3, startTime, 1, 1L);


		chartData = ServiceFactory.getChartService().getNetworkMetricsChartData(cq);
		assertTrue ("number of chart subgroups is 1 ", chartData.size() ==1);
		assertTrue ("number of datapoints is 7", chartData.get(0).getDatapoints().size() == 7);
	}

	/*
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

	 */

	public void testGetHourlyChartDataForNetworkProvider() {

		//Until we have predictable way to populate test DB i.e no random value it will be difficult to exactly test this test case

		//Save a chart critieria first
		ServiceFactory.getNetworkMetricsChartCriteriaService().saveChartCriteria(TestUtil.getChartCriteriaForHourlyNetworkProvider());
		MetricsChartCriteria cq = ServiceFactory.getNetworkMetricsChartCriteriaService().getChartCriteria(1l);

		Calendar startTime = Calendar.getInstance();
		startTime.add(Calendar.HOUR_OF_DAY, -1);

		NetworkTestData.populateCompactNetworkMetricsForMinutes(4, startTime,1, 1l);

		NetworkMetricsDBService dbService = ServiceFactory.getMetricsDBServiceInstance();


		List<NetworkMetricsChartDTO> chartData = ServiceFactory.getChartService().getNetworkMetricsChartData(cq);
		assertTrue ("number of chart subgroups is more than 1 ", chartData.size() > 1);
		//assertTrue ("Num of chart data points is 3 " , chartData.get(0).getDatapoints().size() == 3);

		//assume that user does the refresh after 10 minutes
		startTime.add(Calendar.MINUTE, 12);

		NetworkTestData.populateCompactNetworkMetricsForMinutes(3, startTime, 1, 1L);

		chartData = ServiceFactory.getChartService().getNetworkMetricsChartData(cq);
		assertTrue ("number of chart subgroups is more than 1 ", chartData.size() > 1);
	}
}
