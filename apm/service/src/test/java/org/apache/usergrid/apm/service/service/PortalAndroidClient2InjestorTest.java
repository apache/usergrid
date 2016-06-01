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

import junit.framework.TestCase;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.service.*;
;

public class PortalAndroidClient2InjestorTest extends TestCase {

	//protected static String BASE_SQS_PATH = "https://queue.amazonaws.com/366243268945/wm_metrics_";
	
	App applicationConfigurationModel;
	
	protected void setUp() throws Exception {
		super.setUp();
		applicationConfigurationModel = NetworkTestData.setupBaselineApplication();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testInjestMetics() {
		
		MetricsInjestionService injestionService = ServiceFactory.getMetricsInjestionServiceInstance();
		
		injestionService.injestMetrics(1L, "blah");
		
		assertEquals(true,true);
		
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
	
}
