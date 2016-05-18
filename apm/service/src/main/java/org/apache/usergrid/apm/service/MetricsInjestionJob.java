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
