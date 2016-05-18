package org.apache.usergrid.apm.service;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;

import com.ideawheel.common.model.App;


/**
 * Scheduler that periodically checks if new app has been added. If so, it adds a new injestor for 
 * that app using MetricsInjestionJob
 * @author prabhat
 *
 */

public class NewAppAddedChecker implements Job {

	private static final Log log = LogFactory.getLog(NewAppAddedChecker.class);
	private ApplicationService appService = ServiceFactory.getApplicationService();

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		ApplicationService applicationService = ServiceFactory.getApplicationService();
		List<App> list = null;
		Date prevFireTime = context.getPreviousFireTime();
		if (prevFireTime != null) { //it will be null when webapp is started 
			log.info("going to check for newly added app since "  + prevFireTime);  
			list = applicationService.getAppsAddedSince(prevFireTime);
		}
		if (list != null) {
			log.info("Total number of newly added app " + list.size());

			for(App app : list)
			{
				String appId = app.getInstaOpsApplicationId().toString();
				String fullAppName = app.getFullAppName();
				JobDetail detail;
				try {
					detail = context.getScheduler().getJobDetail(appId, "QUEUE_INJESTION");

					if (detail == null && !app.getMonitoringDisabled())
					{
						log.info("Scheduling injestor for app with id " + appId);
						JobDetail job = new JobDetail(appId.toString(),"QUEUE_INJESTION",MetricsInjestionJob.class);
						job.getJobDataMap().put("instaOpsApplicatonId", appId);
						job.getJobDataMap().put("fullAppName", fullAppName);

						Trigger trigger = TriggerUtils.makeSecondlyTrigger(DeploymentConfig.geDeploymentConfig().getMetricsInjestorInterval());
						trigger.setName(appId.toString());
						trigger.setGroup("QUEUE_INJESTION");

						//Adds job to scheduler
						context.getScheduler().scheduleJob(job, trigger);
					} 
				} catch (SchedulerException e) {
					log.error("problem with scheduler for newly added app");
					log.error(e);
				}
			}
		}


	}

}
