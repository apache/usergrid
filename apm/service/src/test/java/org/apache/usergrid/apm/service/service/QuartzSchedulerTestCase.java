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

import java.util.Date;

import junit.framework.TestCase;

import org.apache.usergrid.apm.service.ApplicationService;
import org.apache.usergrid.apm.service.QuartzScheduler;
import org.apache.usergrid.apm.service.ServiceFactory;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import org.apache.usergrid.apm.model.App;
;

/**
 * 
 * @author prabhat
 *
 */

public class QuartzSchedulerTestCase extends TestCase {

	ApplicationService service;
	
	App model;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		/*service = ServiceFactory.getApplicationService();
		
		model = new App();
		model.setDefaultAppConfig(new ApplicationConfigurationModel(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT));

		model.setDescription("HelloWorld");

		model.setAppName("WordPress");
		model.setAppOwner("Matt Mullen");
		//waiting for one minute so that we don't get error like
		//Status Code: 400, AWS Request ID: 5228b711-5914-43f9-a7f9-98db7adbfd55, AWS Error Code: 
		//AWS.SimpleQueueService.QueueDeletedRecently, AWS Error Message: You must wait 60 seconds 
		//after deleting a queue before you can create another with the same name.
		Thread.sleep(60000);
		//service.createApplication(model);
*/		
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		//ServiceFactory.getHibernateSession().close();
	}

	public void testScheduler() throws InterruptedException {		

		Scheduler scheduler = ((QuartzScheduler) ServiceFactory.getSchedulerService()).getScheduler();

		
		try {
			assertEquals("IdeaWheelSQS2RDSScheduler", scheduler.getMetaData().getSchedulerName());
			Date date = 	new Date (System.currentTimeMillis());
			//scheduler.start();
			System.out.println("Quartz Scheduler started");
			Thread.sleep(10000);
			//service.createApplication(model);
			Thread.sleep(10000);
			Trigger trigger = scheduler.getTrigger("TriggerEvery1Second", "Ideawheel");
			assertEquals("Sqs2RdsJob",trigger.getJobName());
			assertTrue("checking that trigger worked",trigger.getNextFireTime().after(date));			
			scheduler.shutdown();		

		} catch (SchedulerException se) {
			se.printStackTrace();
		}


	}


}
