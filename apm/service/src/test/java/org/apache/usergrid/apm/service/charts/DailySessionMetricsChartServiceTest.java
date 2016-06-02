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

import java.util.Arrays;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;

import junit.framework.TestCase;

import org.apache.usergrid.apm.service.charts.service.AggregatedSessionData;
import org.apache.usergrid.apm.service.charts.service.SessionMetricsChartDTO;
import org.apache.usergrid.apm.model.ChartCriteria.PeriodType;
import org.apache.usergrid.apm.model.CompactSessionMetrics;
import org.apache.usergrid.apm.model.SessionChartCriteria;
import org.apache.usergrid.apm.model.SummarySessionMetrics;
import org.apache.usergrid.apm.service.ServiceFactory;
import org.apache.usergrid.apm.service.SessionDBService;
import org.apache.usergrid.apm.service.SessionDBServiceImpl;
import org.apache.usergrid.apm.service.SessionTestData;
import org.apache.usergrid.apm.service.SummarySessionTestData;
import org.apache.usergrid.apm.service.service.TestUtil;

public class DailySessionMetricsChartServiceTest extends TestCase
{

	protected void setUp() throws Exception {      
		ServiceFactory.invalidateHibernateSession();
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGetDailyChartDataActiveSessions() {

		//Save a chart criteria first
		ServiceFactory.getSessionChartCriteriaService().saveChartCriteria(TestUtil.getActiveSessionChartCriteriaForDaily());

		SessionChartCriteria cq = ServiceFactory.getSessionChartCriteriaService().getChartCriteria(1l);

		assertTrue (cq.getId().equals(1L));

		Calendar startTime = Calendar.getInstance();
		startTime.add(Calendar.HOUR_OF_DAY, -23);
		
		SummarySessionTestData.populateSummarySessionDataForMinutes(61, startTime, 2, 1L);

		//SessionTestData.populateSessionDataForHours(26, startTime,1, 1l);

		SessionDBService dbService = ServiceFactory.getSessionDBService();

		//assertEquals("Num Rows after first insertion for 4 minutes", dbService.getCompactSessionMetricsRowCount(),4);


		List<SessionMetricsChartDTO> chartData = ServiceFactory.getChartService().getSessionMetricsChartData(cq);
		assertTrue ("number of chart subgroups is 1 ", chartData.size() ==1);
		assertTrue ("number of datapoints is not empty", chartData.get(0).getDatapoints().size() >= 1); //could be 25 data points if we end point of interval are inclusive
	}

	public void _testGetDailyChartDataByNetworkCarrier() {

		//Save a chart criteria first
		ServiceFactory.getSessionChartCriteriaService().saveChartCriteria(TestUtil.getGroupByNetworkCarrierSessionChartCriteriaForHourly());

		SessionChartCriteria cq = ServiceFactory.getSessionChartCriteriaService().getChartCriteria(1l);

		assertTrue (cq.getId().equals(1L));
		assertTrue(cq.isGroupedByNetworkCarrier());

		Calendar startTime = Calendar.getInstance();
		startTime.add(Calendar.HOUR_OF_DAY, -1);

		SessionTestData.populateSessionDataForMinutes(4, startTime,1, 1l);

		SessionDBService dbService = ServiceFactory.getSessionDBService();

		assertEquals("Num Rows after first insertion for 4 minutes", dbService.getCompactSessionMetricsRowCount().intValue(),4);


		List<SessionMetricsChartDTO> chartData = ServiceFactory.getChartService().getSessionMetricsChartData(cq);
		assertEquals("Number of chart groups", chartData.size(),getNumDifferentCarriers(1L));

		//assume that user does the refresh after 10 minutes
		startTime.add(Calendar.MINUTE, 10);

		SessionTestData.populateSessionDataForMinutes(3, startTime, 1, 1L);

		int newCount = dbService.getCompactSessionMetricsRowCount().intValue();
		assertEquals("no of rows in compact session metrics table", newCount, 7);

		chartData = ServiceFactory.getChartService().getSessionMetricsChartData(cq);
		assertEquals("Number of chart groups", chartData.size(),getNumDifferentCarriers(1L));      
	}

	public void _testGetAggregateSessionData() {

		//Save a chart criteria first
		ServiceFactory.getSessionChartCriteriaService().saveChartCriteria(TestUtil.getActiveSessionChartCriteriaForHourly());

		SessionChartCriteria cq = ServiceFactory.getSessionChartCriteriaService().getChartCriteria(1l);

		assertTrue (cq.getId().equals(1L));

		Calendar startTime = Calendar.getInstance();
		startTime.add(Calendar.HOUR_OF_DAY, -1);

		SummarySessionTestData.populateSummarySessionDataForMinutes(4, startTime,1, 1l);

		SessionDBService dbService = ServiceFactory.getSessionDBService();

		AggregatedSessionData data = dbService.getAggreateSessionData(cq);

		assertTrue ("got some aggregate session data ", data != null);
		assertTrue (data.getTotalSessions().longValue() == 4);
		assertTrue (data.getAvgSessionLength().longValue() > 0);

	}

	public void _testDailySessionMetricsForYesterday() {

		ServiceFactory.getSessionChartCriteriaService().saveChartCriteria(TestUtil.getActiveSessionChartCriteriaForHourly());

		SessionChartCriteria cq = ServiceFactory.getSessionChartCriteriaService().getChartCriteria(1l);

		assertTrue (cq.getId().equals(1L));

		Calendar startTime = Calendar.getInstance();
		startTime.add(Calendar.HOUR_OF_DAY, -1);

		SessionTestData.populateSessionDataForMinutes(4, startTime,1, 1l);

		SessionDBService dbService = ServiceFactory.getSessionDBService();

		assertEquals("Num Rows after first insertion for 4 minutes", dbService.getCompactSessionMetricsRowCount().intValue(),4);


		List<SessionMetricsChartDTO> chartData = ServiceFactory.getChartService().getSessionMetricsChartData(cq);
		assertTrue ("number of chart subgroups is 1 ", chartData.size() ==1);



		startTime = Calendar.getInstance();
		startTime.add(Calendar.DAY_OF_YEAR, -1); //go back 24 hour
		startTime.add(Calendar.HOUR_OF_DAY, -1); //then go back 1 hour

		//populate test data for yesterday
		SessionTestData.populateSessionDataForMinutes(4, (Calendar) startTime.clone(),1, 1l);     

		assertEquals("Num Rows after inserting data for yesterday", dbService.getCompactSessionMetricsRowCount().intValue(),8);

		//Modify chart criteria to get yesterday's data
		cq.setPeriodType(PeriodType.SET_PERIOD);
		cq.setStartDate(startTime.getTime());
		Calendar endTime = Calendar.getInstance();      
		endTime.add(Calendar.DAY_OF_YEAR, -1);
		cq.setEndDate(endTime.getTime());

		chartData = ServiceFactory.getChartService().getSessionMetricsChartData(cq);
		assertTrue ("number of chart subgroups is 1 ", chartData.size() ==1);    
		assertEquals("There are data points for yesterday", chartData.get(0).getDatapoints().size(), 4);


	}

	public void _testSummarySessionForDevices() {
		ServiceFactory.getSessionChartCriteriaService().saveChartCriteria(TestUtil.getActiveSessionChartCriteriaForHourly());
		Calendar time = Calendar.getInstance();
		time.add(Calendar.MINUTE, -15);
		SummarySessionTestData.populateSummarySessionDataForMinutes(10, time,5, 1L);

		String[] deviceIds = {"apple-iphone", "motorola-droid"};
		//String[] deviceIds = {};
		List<String> dl = Arrays.asList(deviceIds);
		Calendar t = Calendar.getInstance();
		t.add(Calendar.MINUTE, -15);
		List <SummarySessionMetrics> ms = ServiceFactory.getSessionDBService().getSummarySessionsForDevices(1L, dl, t.getTime());
		System.out.println("Size " + ms.size());
		assertTrue("there are some summmary session matching device id " , ms.size() > 0);
	}

		
	private int getNumDifferentCarriers(Long appId) {
		List<CompactSessionMetrics> ms =  new SessionDBServiceImpl().getCompactSessionMetricsForApp(appId); 
		Hashtable<String, Integer> table = new Hashtable<String, Integer>();    
		for(CompactSessionMetrics m :ms) {
			table.put(m.getNetworkCarrier(), 0);          
		}
		return table.size();
	}





}
