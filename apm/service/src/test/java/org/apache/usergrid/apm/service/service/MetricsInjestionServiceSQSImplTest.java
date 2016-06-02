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

import junit.framework.TestCase;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.service.*;
;

public class MetricsInjestionServiceSQSImplTest extends TestCase {

	//protected static String BASE_SQS_PATH = "https://queue.amazonaws.com/366243268945/wm_metrics_";
	
	App applicationConfigurationModel;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		//NetworkTestData.populateTestDataForUnitTests();
		applicationConfigurationModel = NetworkTestData.populateDataForMetricsInjestionTest();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testInjestMetics() {
		
		MetricsInjestionService injestionService = ServiceFactory.getMetricsInjestionServiceInstance();
		
		injestionService.injestMetrics(applicationConfigurationModel.getInstaOpsApplicationId(), applicationConfigurationModel.getFullAppName());
		
		assertTrue ("Compact session metrics row count is not 0", ServiceFactory.getSessionDBService().getCompactSessionMetricsRowCount() != 0);
		assertTrue ("Compact session metrics row count is not 0", ServiceFactory.getLogDBService().getCompactLogRowCount() != 0);
		
				
	}

	public void testInjestAllMetics() {
		
		MetricsInjestionService injestionService = ServiceFactory.getMetricsInjestionServiceInstance();
		
		injestionService.injestAllMetrics();
		
		
	}
	

	public void testInjestMetricsMultipleTimes() throws InterruptedException {
		
		//Wait 30 seconds before injesting metrics
		Thread.sleep(30000);
		
		MetricsInjestionService injestionService = ServiceFactory.getMetricsInjestionServiceInstance();
		
		injestionService.injestAllMetrics();
		
		// Wait 60 seconds before injesting metrics again.
		Thread.sleep(60000);
				
		NetworkTestData.populateSQSWithTestData(1, Calendar.getInstance(), 5, applicationConfigurationModel);
		
		injestionService.injestAllMetrics();
		
		
	}
	
	
	
	public void injestMetrics(Long applicationId, String name) throws InterruptedException {
		
		
		MetricsInjestionService injestionService = ServiceFactory.getMetricsInjestionServiceInstance();
		
		MetricsInjestionServiceSQSImpl castedInjestionService = (MetricsInjestionServiceSQSImpl)injestionService;
		
		AmazonSQS sqsClient = castedInjestionService.getSqsClient();
		
		//Determine how many metrics there.
		
		GetQueueAttributesRequest queueAttributesRequest = new GetQueueAttributesRequest();

		ArrayList<String> attributeNames = new ArrayList<String>(2);

		attributeNames.add("ApproximateNumberOfMessages");
		attributeNames.add("LastModifiedTimestamp");

		queueAttributesRequest.setAttributeNames(attributeNames);
		queueAttributesRequest.setQueueUrl(AWSUtil.formFullQueueUrl(name));

		GetQueueAttributesResult queueAttributeResult = sqsClient.getQueueAttributes(queueAttributesRequest);

		int numMessages = Integer.parseInt(queueAttributeResult.getAttributes().get("ApproximateNumberOfMessages"));
		
		System.out.println("Num Messages to consume :" + numMessages);
		
		if(numMessages > 0)
		{
			injestionService.injestMetrics(applicationId, name);
			
			Thread.sleep(30000);
			
			queueAttributeResult = sqsClient.getQueueAttributes(queueAttributesRequest);
	
			int finalNumMessages = Integer.parseInt(queueAttributeResult.getAttributes().get("ApproximateNumberOfMessages"));
	

			System.out.println("Num Messages left to consume :" + finalNumMessages);
			
			//assertEquals(true, finalNumMessages < numMessages);
		}
		
	}	
	
	public void testInjestNonExistantQueue() throws InterruptedException {
		
		Long applicationId = 75205458656549557L;
		
		MetricsInjestionService injestionService = ServiceFactory.getMetricsInjestionServiceInstance();
		
		injestionService.injestMetrics(applicationId,"blah");
		
	}

	/** since we don't have csv anymore
	public void testMessageParsingOnlyWithHeader() {
		String header = TestUtil.generateSampleMessage(0);
		MetricsInjestionServiceSQSImpl injestionService = (MetricsInjestionServiceSQSImpl) ServiceFactory.getMetricsInjestionServiceInstance();
		List list = injestionService.marshallCSVMessage(header);
		assertTrue("number of metrics bean is zero", list.size() ==0);
		
	}
	
	public void testMessageParsingWithHeaderAnd1RowData() {
		String messages = TestUtil.generateSampleMessage(1);
		MetricsInjestionServiceSQSImpl injestionService = (MetricsInjestionServiceSQSImpl) ServiceFactory.getMetricsInjestionServiceInstance();
		List list = injestionService.marshallCSVMessage(messages);
		assertTrue("number of metrics bean is 1", list.size() ==1);
		
	}
	
	public void testMessageParsingWithHeaderAndMultpleRowsOfData() {
		String messages = TestUtil.generateSampleMessage(3);
		MetricsInjestionServiceSQSImpl injestionService = (MetricsInjestionServiceSQSImpl) ServiceFactory.getMetricsInjestionServiceInstance();
		List list = injestionService.marshallCSVMessage(messages);
		assertTrue("number of metrics bean is 3", list.size() ==3);
		
	}
	
	
	public void testMessgaeParsingAndPersistence() {
		String messages = TestUtil.generateSampleMessage(3);
		MetricsInjestionServiceSQSImpl injestionService = (MetricsInjestionServiceSQSImpl) ServiceFactory.getMetricsInjestionServiceInstance();
		List<ClientNetworkMetrics> list = injestionService.marshallCSVMessage(messages);
		assertTrue("number of metrics bean is 3", list.size() ==3);
		NetworkMetricsDBServiceImpl hibService =  (NetworkMetricsDBServiceImpl) ServiceFactory.getWsHibernateServiceInstance();
		hibService.saveWebServicesRecords(list);
		List<ClientNetworkMetrics> list1 = hibService.getWebServicesRecordsForApp(new Long (999));
		assertEquals("number of persisted metrics bean is 3", 3,  list.size());
		
		
	}
*/	
	
}
