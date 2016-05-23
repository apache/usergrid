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
