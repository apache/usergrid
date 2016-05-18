package org.apache.usergrid.apm.service;

import java.util.Collections;
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
 * Scheduler that periodically consumes messages from SQS using {@link MetricsInjestionServiceSQSImpl} 
 * and shoves into database/RDS . This gets started when webapp is loaded through QuartzServlet
 * @author prabhat
 *
 */

public class MetricsInjestionSchedulerJob implements Job {

	private static final Log log = LogFactory.getLog(MetricsInjestionSchedulerJob.class);
	private MetricsInjestionService metricsInjestor = ServiceFactory.getMetricsInjestionServiceInstance();

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		if(DeploymentConfig.geDeploymentConfig().isEnableInjestor())
		{

			log.info("App Server just started. Let's start metrics injestor for all active apps first");

			ServiceFactory.setSchedulerStartedThroughWebApp(context.getScheduler());

			ApplicationService applicationService = ServiceFactory.getApplicationService();

			List<App> list = applicationService.getAllApps();
			Collections.shuffle(list);

			for(App app : list)
			{
				String appId = app.getInstaOpsApplicationId().toString();
				String fullAppName = app.getFullAppName();
				JobDetail detail;
				try {
					detail = context.getScheduler().getJobDetail(appId.toString(), "QUEUE_INJESTION");

					if (detail == null && !app.getMonitoringDisabled())
					{

						log.info("Scheduling injestor duing server start for application " + appId.toString());

						JobDetail job = new JobDetail(appId.toString(),"QUEUE_INJESTION",MetricsInjestionJob.class);
						job.getJobDataMap().put("instaOpsApplicatonId", appId);
						job.getJobDataMap().put("fullAppName", fullAppName);

						Trigger trigger = TriggerUtils.makeSecondlyTrigger(DeploymentConfig.geDeploymentConfig().getMetricsInjestorInterval()); 
						trigger.setName(fullAppName);
						trigger.setGroup("QUEUE_INJESTION");
						//Adds job to scheduler
						context.getScheduler().scheduleJob(job, trigger);
					} 
				} catch (SchedulerException e) {
					// TODO Auto-generated catch block
					log.error(e);
				}
			}


			log.info("Going to add a new scheduler that checks for newly added app and then adds injestor for app");        
			//Adds job to scheduler
			try
			{
				if (context.getScheduler().getJobDetail("NewAppChecker", "NewAppCheckerGroup") == null) {
					JobDetail job = new JobDetail("NewAppChecker","NewAppCheckerGroup",NewAppAddedChecker.class);
					Trigger trigger = TriggerUtils.makeMinutelyTrigger(1);
					trigger.setName("NewAppCheckerPerMinuteTrigger");
					trigger.setGroup("NewAppCheckerGroup");

					context.getScheduler().scheduleJob(job, trigger);
				}
				
				//TODO: This should be moved to a separate service that does all cron jobs not related to metrics injestion 

				if (context.getScheduler().getJobDetail("EnvironmentOverrideUpdater", "EnvironmentUpdaterGroup") == null) {
					JobDetail job = new JobDetail("EnvironmentOverrideUpdater","EnvironmentUpdaterGroup",EnvironmentOverrideValueUpdater.class);
					Trigger trigger = TriggerUtils.makeMinutelyTrigger(10);
					trigger.setName("EnvironmentUpdatePer10MinuteTrigger");
					trigger.setGroup("EnvironmentUpdaterGroup");

					context.getScheduler().scheduleJob(job, trigger);
				}

			}
			catch (SchedulerException e)
			{ 
				log.error("Problem adding NewAppChecker Job or EnvironmentUpdater");
				e.printStackTrace();
			}



		} else
		{
			log.warn("############## Metrics Injestor is not enabled on this server.##########");
		}
	}

}
