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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.drools.runtime.conf.ClockTypeOption;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.quartz.Scheduler;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.AmazonSQSClient;
import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.model.AppConfigCustomParameter;
import org.apache.usergrid.apm.model.AppConfigOverrideFilter;
import org.apache.usergrid.apm.model.AppConfigURLRegex;
import org.apache.usergrid.apm.model.ApplicationConfigurationModel;
import org.apache.usergrid.apm.model.ClientLog;
import org.apache.usergrid.apm.model.ClientMetricsEnvelope;
import org.apache.usergrid.apm.model.ClientNetworkMetrics;
import org.apache.usergrid.apm.model.ClientSessionMetrics;
import org.apache.usergrid.apm.service.charts.service.ChartService;
import org.apache.usergrid.apm.service.charts.service.ChartServiceImpl;
import org.apache.usergrid.apm.service.charts.service.LogChartCriteriaService;
import org.apache.usergrid.apm.service.charts.service.NetworkMetricsChartCriteriaService;
import org.apache.usergrid.apm.service.charts.service.SessionChartCriteriaService;
import com.thoughtworks.xstream.XStream;


/**
 * Factory class, which can be used to instantiate service classes. Service classes should be used for business-logic
 * operations
 *
 * @author prabhat jha
 */
public class ServiceFactory
{

	private static final Log log = LogFactory.getLog(ServiceFactory.class);

	private static NetworkMetricsDBService networkMetricsDBService = new NetworkMetricsDBServiceImpl();
	private static SessionDBService sessionDBService = new SessionDBServiceImpl();
	private static MetricsInjestionService metricsInjestionService;
	private static SchedulerService quartzScheduler = new QuartzScheduler();
	/**
	 * Note that this will only be set if AwsConfig.getAwsConfig().isEnableInjestor() true. However job will be added to 
	 * this scheduler dynamically only when both UI and injestor are running on this instance
	 */
	private static Scheduler schedulerStartedThroughWebApp = null;
	private static ApplicationService applicationService;

	private static NetworkMetricsChartCriteriaService networkMetricsChartCritService = new NetworkMetricsChartCriteriaService();
	private static SessionChartCriteriaService sessionChartCritService = new SessionChartCriteriaService();
	private static LogChartCriteriaService appDiagCritService = new LogChartCriteriaService();

	private static ChartService chartService = new ChartServiceImpl();

	private static LogDBService logService = new LogDBService();
	
	private static CrashLogDBService crashDBService = new CrashLogDBService();

	private static AlarmService alarmService; 


	private static final Object factoryMutex = new Object();
	public static int hibernateBatchSize;

	private static AmazonSQSClient sqsClient;
	private static AmazonSQSAsyncClient sqsAsyncClient;
	/** Hibernate Session factory */
	private static SessionFactory sessionFactory;
	private static SessionFactory analyticsSessionFactory;

	private static StatelessComplexEventProcessingService cepService = new StatelessComplexEventProcessingService(ClockTypeOption.get("realtime"));

	private static AWSUtil awsUtil;



	private static XStream metricsXStream = null;
	private static XStream appConfigXStream = null;

	public static XStream getMetricsXStream () {
		if (metricsXStream == null) {
			metricsXStream = HibernateXStream.getHibernateXStream();
			metricsXStream.alias("clientLog", ClientLog.class);
			metricsXStream.alias("clientNetworkMetrics", ClientNetworkMetrics.class);
			metricsXStream.alias("clientSessionMetrics", ClientSessionMetrics.class);
			metricsXStream.alias("clientMetricsEnvelope", ClientMetricsEnvelope.class);
		}

		return metricsXStream;
	}

	public static XStream getAppConfigXStream () {
		if (appConfigXStream == null) {
			appConfigXStream = HibernateXStream.getHibernateXStream();
			appConfigXStream.alias("App", App.class);
			appConfigXStream.alias("applicationConfigurationModel", ApplicationConfigurationModel.class);
			appConfigXStream.alias("appConfigURLRegex", AppConfigURLRegex.class);
			appConfigXStream.alias("appConfigOverrideFilter", AppConfigOverrideFilter.class);
			appConfigXStream.alias("appConfigCustomParameter", AppConfigCustomParameter.class);
		}

		return appConfigXStream;
	}



	public static AmazonSQSClient getSQSClient()
	{
		if (sqsClient == null)
		{
			DeploymentConfig config = DeploymentConfig.geDeploymentConfig();
			AWSCredentials awsCredentials = new BasicAWSCredentials(config.getAccessKey(), config.getSecretKey());
			sqsClient = new AmazonSQSClient(awsCredentials);
		}

		return sqsClient;
	}

	public static AmazonSQSAsyncClient getSQSAsyncClient()
	{
		if (sqsAsyncClient == null)
		{
			DeploymentConfig config = DeploymentConfig.geDeploymentConfig();
			AWSCredentials awsCredentials = new BasicAWSCredentials(config.getAccessKey(), config.getSecretKey());
			sqsAsyncClient = new AmazonSQSAsyncClient(awsCredentials);
		}

		return sqsAsyncClient;
	}



	public static Session getHibernateSession()
	{
		if (sessionFactory == null)
		{
			synchronized (factoryMutex)
			{
				AnnotationConfiguration configuration = new AnnotationConfiguration().configure("hibernate-app-management.cfg.xml");
				hibernateBatchSize = new Integer(configuration.getProperty("hibernate.jdbc.batch_size")).intValue();
				sessionFactory = configuration.buildSessionFactory();            
			}

		}

		return sessionFactory.getCurrentSession();
	}

	public static Session getAnalyticsHibernateSession()
	{
		if (analyticsSessionFactory == null)
		{
			synchronized (factoryMutex)
			{
				AnnotationConfiguration configuration = new AnnotationConfiguration().configure("hibernate-analytics.cfg.xml");
				hibernateBatchSize = new Integer(configuration.getProperty("hibernate.jdbc.batch_size")).intValue();
				analyticsSessionFactory = configuration.buildSessionFactory();            
			}
			//TODO: Need other way to start the Quartz scheduler..it's a hack. Since scheduler is supposed to do some DB manipulation,
			//any service that needs Scheduler would have first got hibernate session so it makes sure that scheduler is setup first.
			/*  log.info("################### going to start the scheduler ..just the log##########");
         log.info("param value is " + System.getProperty("PARAM1"));
         if (System.getProperty("PARAM1") != null && System.getProperty("PARAM1").equals("true")) {
            log.info("going to start the scheduler");
            ((QuartzScheduler)ServiceFactory.getSchedulerService()).getScheduler();
         }*/
			//pre-pouluate some data        

		}

		return analyticsSessionFactory.getCurrentSession();
	}

	public static void prepoulateDB() {
		String prePopulatedDataFileName = "conf/db-init.txt";
		Properties dbInitProps = new Properties();
		try
		{
			dbInitProps.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(prePopulatedDataFileName));
			PrePopulateDBUtil.prePopulateDeviceModel(dbInitProps.getProperty("device-model"));
			PrePopulateDBUtil.prePopulateDevicePlatform(dbInitProps.getProperty("device-platform"));
			PrePopulateDBUtil.prePopulateNetworkCarriers(dbInitProps.getProperty("network-carriers"));
			PrePopulateDBUtil.prePopulateNetworkSpeed(dbInitProps.getProperty("network-speed"));
			PrePopulateDBUtil.prePopulateAdminUser(dbInitProps.getProperty("root-user"),dbInitProps.getProperty("root-pwd"),
					dbInitProps.getProperty("root-email"));         
			log.debug("prepopulated DBs" );
		}catch (FileNotFoundException e)
		{
			log.fatal("Could not read the properties file " + prePopulatedDataFileName + " for prepoupulate db stuff");
			log.error(e);
		}
		catch (IOException e)
		{
			log.error(e);
		}     

	}
	/** Getting of PhoneCallHibernateService implementation for performing operations on phonecall data database. */
	public static NetworkMetricsDBService getMetricsDBServiceInstance()
	{
		return networkMetricsDBService;
	}

	public static MetricsInjestionService getMetricsInjestionServiceInstance()
	{
		if (metricsInjestionService == null)
		{
			//Initialize metricsInjestionService
			MetricsInjestionServiceSQSImpl injestionServiceImpl = new MetricsInjestionServiceSQSImpl();

			//Set DAO
			injestionServiceImpl.setHibernateService(getMetricsDBServiceInstance());

			injestionServiceImpl.setLogDBService(getLogDBService());
			injestionServiceImpl.setSessionDBService(getSessionDBService());

			injestionServiceImpl.setSqsClient(getSQSClient());
			injestionServiceImpl.setSqsAsyncClient(getSQSAsyncClient());
			injestionServiceImpl.setCepService(cepService);
		
			injestionServiceImpl.setXStream(ServiceFactory.getMetricsXStream());


			metricsInjestionService = injestionServiceImpl;
		}

		return metricsInjestionService;

	}

	public static SchedulerService getSchedulerService()
	{
		return quartzScheduler;
	}


	/** @return the applicationService */
	public static ApplicationService getApplicationService()

	{
		if (applicationService == null)
		{
			ApplicationServiceImpl applicationServiceImpl = new ApplicationServiceImpl();					

			String accessKey = DeploymentConfig.geDeploymentConfig().getAccessKey(); 
			String secretKey = DeploymentConfig.geDeploymentConfig().getSecretKey();				

			AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
			AmazonSQSClient sqsClient = getSQSClient();
			AmazonS3 s3Client = new AmazonS3Client(awsCredentials);
			AmazonIdentityManagementClient identityManagementClient = new AmazonIdentityManagementClient(awsCredentials);

			applicationServiceImpl.setAwsCredentials(awsCredentials);
			applicationServiceImpl.setSqsClient(sqsClient);
			applicationServiceImpl.setS3Client(s3Client);
			applicationServiceImpl.setXStream(ServiceFactory.getAppConfigXStream());
			applicationServiceImpl.setIdentityManagementClient(identityManagementClient);
			applicationService = applicationServiceImpl;
		}

		return applicationService;
	}

	public static AlarmService getAlarmService() {
		if (alarmService == null)
		{
			alarmService = new AlarmService();
			alarmService.setApplicationService(getApplicationService());
		}
		return alarmService;
	}

	public static void setAlarmService(AlarmService alarmService) {
		ServiceFactory.alarmService = alarmService;
	}

	public static NetworkMetricsChartCriteriaService getNetworkMetricsChartCriteriaService () {
		return networkMetricsChartCritService;
	}

	public static ChartService getChartService () {
		return chartService;
	}

	public static LogDBService getLogDBService () {
		return logService;

	}
	
	public static CrashLogDBService getCrashLogDBService () {
		return crashDBService;
	}

	public static void invalidateHibernateSession () {
		if (sessionFactory != null) {
			sessionFactory.getCurrentSession().close();
			sessionFactory.close();	
			sessionFactory = null;
		}

	}	

	public static SessionChartCriteriaService getSessionChartCriteriaService () {
		return sessionChartCritService;
	}

	public static LogChartCriteriaService getLogChartCriteriaService() {
		return appDiagCritService;
	}

	public static SessionDBService getSessionDBService () {
		return sessionDBService;
	}

	public static Scheduler getSchedulerStartedThroughWebApp()
	{
		return schedulerStartedThroughWebApp;
	}

	public static void setSchedulerStartedThroughWebApp(Scheduler schedulerStartedThroughWebApp)
	{
		log.info("schedulerStartedThroughWebApp got set in ServiceFactory");
		ServiceFactory.schedulerStartedThroughWebApp = schedulerStartedThroughWebApp;
	}



}
