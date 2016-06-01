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
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.usergrid.apm.service.NetworkTestData;
import org.apache.usergrid.apm.service.StatelessComplexEventProcessingService;
import org.drools.runtime.conf.ClockTypeOption;

import org.apache.usergrid.apm.model.ClientMetricsEnvelope;
import org.apache.usergrid.apm.model.ClientSessionMetrics;
import org.apache.usergrid.apm.model.SummarySessionMetrics;

import junit.framework.TestCase;

public class SessionCalculationTest extends TestCase {

private static final Log log = LogFactory.getLog("SessionCalc");

	final static private long ONE_MIN = 1000 * 60;
	final static private String SESSION_A = "A";
	final static private String SESSION_B = "B";
	final static private String SESSION_C = "C";
	final static private String SESSION_D = "D";
	final static private String DEV_1 = "D1";
	final static private String DEV_2 = "D2";
	final static private String DEV_3 = "D3";

	ClientSessionMetrics csm1;
	ClientSessionMetrics csm2;
	ClientSessionMetrics csm3;
	ClientSessionMetrics csm4;
	ClientSessionMetrics csm5;
	ClientSessionMetrics csm6;
	ClientSessionMetrics csm7;
	ClientSessionMetrics csm8;
	ClientSessionMetrics csm9;
	
	StatelessComplexEventProcessingService cepService ;
	
	protected void setUp() throws Exception {
		super.setUp();

		NetworkTestData.setupBaselineApplication();
		
		cepService  = new StatelessComplexEventProcessingService(ClockTypeOption.get("pseudo"));
		
		csm1 = NetworkTestData.generateSessionMetrics(1, new Date(ONE_MIN), new Date(ONE_MIN),SESSION_A,DEV_1);
		csm2 = NetworkTestData.generateSessionMetrics(1, new Date(ONE_MIN), new Date(ONE_MIN * 4),SESSION_A,DEV_1);
		csm3 = NetworkTestData.generateSessionMetrics(1, new Date(ONE_MIN * 3), new Date(ONE_MIN * 3),SESSION_B,DEV_2);
		csm4 = NetworkTestData.generateSessionMetrics(1, new Date(ONE_MIN * 3), new Date(ONE_MIN * 7),SESSION_B,DEV_2);
		csm5 = NetworkTestData.generateSessionMetrics(1, new Date(ONE_MIN * 5), new Date(ONE_MIN * 5),SESSION_C,DEV_3);
		csm6 = NetworkTestData.generateSessionMetrics(1, new Date(ONE_MIN * 5), new Date(ONE_MIN * 8),SESSION_C,DEV_3);
		csm7 = NetworkTestData.generateSessionMetrics(1, new Date(ONE_MIN), new Date(ONE_MIN * 6),SESSION_A,DEV_1);
		csm8 = NetworkTestData.generateSessionMetrics(1, new Date(ONE_MIN * 8), new Date(ONE_MIN * 8),SESSION_D,DEV_1);
		csm9 = NetworkTestData.generateSessionMetrics(1, new Date(ONE_MIN * 8), new Date(ONE_MIN * 9),SESSION_D,DEV_1);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testSummarySessionMetricsCreation()
	{
		
		List<ClientSessionMetrics> clientSessionMetrics1 = new ArrayList<ClientSessionMetrics>();
		List<SummarySessionMetrics> dirtySummarySessionMetrics1 = new ArrayList<SummarySessionMetrics>();
		Set<Long> modifiedMinutes1 = new TreeSet<Long>();
		
		//cepService.calculateSummarySessionMetrics("1", clientSessionMetrics, dirtySummarySessionMetrics, modifiedMinutes);
		
		Long appId = 1L;
		
		clientSessionMetrics1.add(csm1);

		clientSessionMetrics1.add(csm2);

		clientSessionMetrics1.add(csm3);

		clientSessionMetrics1.add(csm4);
		
		
		cepService.calculateSummarySessionMetrics2(appId, null, clientSessionMetrics1, 
				dirtySummarySessionMetrics1, modifiedMinutes1);
		
		assertEquals(2, dirtySummarySessionMetrics1.size());
		
		//assertEquals(SESSION_A, dirtySummarySessionMetrics1.get(0).getSessionId());
		//assertEquals(SESSION_B, dirtySummarySessionMetrics1.get(1).getSessionId());
		
		SummarySessionMetrics ssm1;
		SummarySessionMetrics ssm2;
		SummarySessionMetrics ssm3;
		SummarySessionMetrics ssm4;
		
		
		for(SummarySessionMetrics ssm : dirtySummarySessionMetrics1 )
		{
			if (ssm.getDeviceId().equals(DEV_1))
			{
				ssm1 = ssm;
				assertEquals(3, ssm.getEndMinute() - ssm.getStartMinute());
			}
			
			if (ssm.getDeviceId().equals(DEV_2))
			{
				ssm2 = ssm;
				assertEquals(4, ssm.getEndMinute() - ssm.getStartMinute());
			}
			
		}
		
		
		
	}
	
	public void testCompactSessionMetricsCalculation()
	{
		
		List<ClientSessionMetrics> clientSessionMetrics1 = new ArrayList<ClientSessionMetrics>();
		List<SummarySessionMetrics> dirtySummarySessionMetrics1 = new ArrayList<SummarySessionMetrics>();
		Set<Long> modifiedMinutes1 = new TreeSet<Long>();
		
		//cepService.calculateSummarySessionMetrics("1", clientSessionMetrics, dirtySummarySessionMetrics, modifiedMinutes);
		
		Long appId = 1L;
		
		clientSessionMetrics1.add(csm1);

		clientSessionMetrics1.add(csm2);

		clientSessionMetrics1.add(csm3);

		clientSessionMetrics1.add(csm4);
		
		List<ClientMetricsEnvelope> messages = new ArrayList<ClientMetricsEnvelope>();
		
		ClientMetricsEnvelope e1 = new ClientMetricsEnvelope();
		e1.setSessionMetrics(csm1);
		messages.add(e1);
		ClientMetricsEnvelope e2 = new ClientMetricsEnvelope();
		e2.setSessionMetrics(csm2);
		messages.add(e2);
		ClientMetricsEnvelope e3 = new ClientMetricsEnvelope();
		e3.setSessionMetrics(csm3);
		messages.add(e3);
		ClientMetricsEnvelope e4 = new ClientMetricsEnvelope();
		e4.setSessionMetrics(csm4);
		messages.add(e4);
		
		cepService.processEvents(1L,"foobar", messages);
		
		List<ClientMetricsEnvelope> messages2 = new ArrayList<ClientMetricsEnvelope>();
		
		ClientMetricsEnvelope e5 = new ClientMetricsEnvelope();
		e5.setSessionMetrics(csm5);
		messages2.add(e5);
		ClientMetricsEnvelope e6 = new ClientMetricsEnvelope();
		e6.setSessionMetrics(csm6);
		messages2.add(e6);
		ClientMetricsEnvelope e7 = new ClientMetricsEnvelope();
		e7.setSessionMetrics(csm7);
		messages2.add(e7);
		

		cepService.processEvents(1L,"foobar", messages2);
		
		List<ClientMetricsEnvelope> messages3 = new ArrayList<ClientMetricsEnvelope>();

		ClientMetricsEnvelope e8 = new ClientMetricsEnvelope();
		e8.setSessionMetrics(csm8);
		messages3.add(e8);
		ClientMetricsEnvelope e9 = new ClientMetricsEnvelope();
		e9.setSessionMetrics(csm9);
		messages3.add(e9);
		
		cepService.processEvents(1L, "foobar", messages3);
		
		
	}
	
}
