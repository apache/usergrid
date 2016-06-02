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
package org.apache.usergrid.apm.service.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.usergrid.apm.service.ComplexEventProcessingService;
import org.apache.usergrid.apm.service.NetworkTestData;
import org.apache.usergrid.apm.service.ServiceFactory;
import org.drools.runtime.conf.ClockTypeOption;
import org.drools.time.SessionPseudoClock;

import org.apache.usergrid.apm.model.ClientMetricsEnvelope;

import org.apache.usergrid.apm.service.charts.service.SessionChartCriteriaService;

public class ComplexEventProcessingServiceTest extends TestCase {

	private static final Log log = LogFactory.getLog(ComplexEventProcessingServiceTest.class);
	
	protected void setUp() throws Exception {
		super.setUp();
		
		NetworkTestData.setupBaselineApplication();
		
		
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void _testMultipleEvents()
	{
		ComplexEventProcessingService cepService = new ComplexEventProcessingService(ClockTypeOption.get("pseudo"));
		
		NetworkTestData.populateDataForComplexEventService(20, Calendar.getInstance(), 30, 0L, cepService);
		
	}
	
	public void _testSimpleMultipleEvents()
	{
		ComplexEventProcessingService cepService = new ComplexEventProcessingService(ClockTypeOption.get("pseudo"));
		cepService.setMetricsDBService(ServiceFactory.getMetricsDBServiceInstance());
		
		//NetworkTestData.populateDataForComplexEventService(3, Calendar.getInstance(), 3, 0L, cepService);
		NetworkTestData.populateDataForComplexEventService(20, Calendar.getInstance(), 50, 1L, cepService);
	}
	
	public void testSessionTestingEvents()
	{
		ComplexEventProcessingService cepService = new ComplexEventProcessingService(ClockTypeOption.get("pseudo"));
		cepService.setMetricsDBService(ServiceFactory.getMetricsDBServiceInstance());
		
		SessionChartCriteriaService sessionChartCriteriaService = ServiceFactory.getSessionChartCriteriaService();
		//SessionChartCriteria scc = sessionChartCriteriaService.getDefaultChartCriteriaByName( 1L,SessionChartCriteriaService.ACTIVE_USERS_SESSION);
		
		//Long id = scc.getId();
		
		int numEventPeriods = 2;
		
		Calendar start = Calendar.getInstance();
		start.add(Calendar.HOUR_OF_DAY, -1);
		
		NetworkTestData.populateDataForComplexEventService(numEventPeriods, start, 5, 1L, cepService);
		
		log.info("------------------------------------------------------------------------------------");
		
		if (cepService.getClientLogStream() != null)
		{
			log.info("ClientLog : " + cepService.getClientLogStream().getFactCount());
			//log.info("CompactClientLog : " + cepService.getCompactClientLogStream().getFactCount());
		}
		
		if (cepService.getSummarySessionMetricsStream() != null)
		{
			log.info("Num SummarySessions : " + cepService.getSummarySessionMetricsStream().getFactCount());
		}
				
		if (cepService.getCompactSessionMetricsStream() != null)
		{
			log.info("Num compactSession Metrics : " + cepService.getCompactSessionMetricsStream().getFactCount());
		}
		
		if (cepService.getCompactClientLogStream() != null)
		{
			//log.info("ClientLog : " + cepService.getClientLogStream().getFactCount());
			log.info("CompactClientLog : " + cepService.getCompactClientLogStream().getFactCount());
		}
	}
	
	public void _testProcessSingleEvent()
	{
		ComplexEventProcessingService cepService = new ComplexEventProcessingService(ClockTypeOption.get("pseudo"));
		
		Calendar now = Calendar.getInstance();
		
		Calendar endTime =Calendar.getInstance();
		endTime.setTime(now.getTime());
		endTime.add(Calendar.SECOND, 60);
		
		ClientMetricsEnvelope envelope1 = NetworkTestData.generateWebServiceMetricsBeanMessageEnvelope(0, now.getTime(), endTime.getTime());
		ClientMetricsEnvelope envelope2 = NetworkTestData.generateWebServiceMetricsBeanMessageEnvelope(1, now.getTime(), endTime.getTime());
		
		
		List<ClientMetricsEnvelope> messages = new ArrayList<ClientMetricsEnvelope>();
		messages.add(envelope1);
		messages.add(envelope2);
		
		SessionPseudoClock clock = cepService.getSession().getSessionClock();
		
		
		cepService.processEvents(messages);
		
		
		clock.advanceTime(120, TimeUnit.SECONDS);
		
		
		List<ClientMetricsEnvelope> messages2 = new ArrayList<ClientMetricsEnvelope>();
		cepService.processEvents(messages2);
		
		List list = new LinkedList();
	}
		
}
