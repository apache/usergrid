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
package org.apache.usergrid.apm.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Scheduler that periodically consumes messages from SQS using {@link MetricsInjestionServiceSQSImpl} 
 * and shoves into database/RDS
 * @author prabhat
 *
 */

public class MetricsInjestionJob implements Job {
	
	private static final Log log = LogFactory.getLog(MetricsInjestionJob.class);
	private MetricsInjestionService metricsInjestor = ServiceFactory.getMetricsInjestionServiceInstance();

	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		//context.getScheduler().getCurrentlyExecutingJobs();
		long startTime = System.currentTimeMillis();
		Long instaOpsApplicationId = new Long (context.getJobDetail().getJobDataMap().getString("instaOpsApplicatonId"));	
		String fullAppName = context.getJobDetail().getJobDataMap().getString("fullAppName"); 
		log.info("Start Injesting Metrics for app " + fullAppName + " with Thread " + Thread.currentThread().getName());		
		if (instaOpsApplicationId != null) 		
			metricsInjestor.injestMetrics(instaOpsApplicationId, fullAppName);
		
		long endTime = System.currentTimeMillis();
		log.info("Done Injesting Metrics for App " + fullAppName + " in " + (endTime - startTime) + " milliseconds");
	}

}
