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
package org.apache.usergrid.apm.util;

import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.service.NetworkTestData;

/**
 * Scheduler that periodically sends messages to SQS
 * @author prabhat
 *
 */

public class SendTestMetricsSchedulerJob implements Job {

	
	private static final Log log = LogFactory.getLog(SendTestMetricsSchedulerJob.class);


	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		log.info("Simulating Sending of 500 active devices");		
		
		NetworkTestData.populateSQSWithTestData(1, Calendar.getInstance(), 500, new App());
		
	}

}
