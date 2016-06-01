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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.time.SessionPseudoClock;

import org.apache.usergrid.apm.model.ApigeeMobileAPMConstants;
import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.model.ApplicationConfigurationModel;
import org.apache.usergrid.apm.model.ClientLog;
import org.apache.usergrid.apm.model.ClientMetricsEnvelope;
import org.apache.usergrid.apm.model.ClientNetworkMetrics;
import org.apache.usergrid.apm.model.ClientSessionMetrics;
import org.apache.usergrid.apm.model.ChartCriteria.SamplePeriod;
import org.apache.usergrid.apm.model.CompactNetworkMetrics;
import org.apache.usergrid.apm.model.MetricsChartCriteria;
//import org.hsqldb.util.CSVWriter;

public class NetworkTestData
{

	static String[] networkCarriers = {"att", "verizon", "sprint", "tmobile"};
	static Long[] appId = {1l, 456l, 3l};
	static String[] networkType = {"2g", "3g", "wifi"};
	static long baseLatency = 50L;
	static String[] uids = {"alan", "prabhat", "ideawheel", "cloud", "amazon", "google"};
	static String[] deviceIds = {"apple-iphone", "motorola-droid", "apple-ipad", "cloud-pc", "amazon-kindle", "google-nexus"};
	static String[] telephoneDeviceId = {"alan-iphone", "prabhat-droid", "ideawheel-ipad", "cloud-pc", "amazon-kindle", "google-nexus"};
	static String[] wsUrls = {"http://eejot.org/students", "http://google.com/finance", "https://amazon.com/price",
		"http://ideawheel.com/metric", "http://jboss.org/rest", "https://twitter.com/tweet"};
	static String[] deviceModel = {"apple-iphone4g","apple-ipad","samsung-galaxytab","htc-thunder","google-nexusone"};
	static String[] deviceOperatingSystem = {"2.2","2.3","3.0"};
	static String[] devicePlatform = {"Android","iOS","WindowsMobile"};
	static String[] countries = {"USA","Canada","Nepal","Greece"};
	static String[] language = {"en-US","en-CA","fr-FR", "fr-AG", "np-NP"};
	static String[] version = {"1","2","3","4"};
	static String[] configs = {ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT,
		ApigeeMobileAPMConstants.CONFIG_TYPE_DEVICE_LEVEL,
		ApigeeMobileAPMConstants.CONFIG_TYPE_DEVICE_TYPE,
		ApigeeMobileAPMConstants.CONFIG_TYPE_AB};

	public static enum OutputType{DB,CSV};


	static Random generator = new Random();

	private static final Log log = LogFactory.getLog(NetworkTestData.class);


	
	public static App populateDataForMetricsInjestionTest()
	{

		App app = setupBaselineApplication();

		log.info("Populating Metrics for 1 app in SQS Queue");
		NetworkTestData.populateSQSWithTestData(1, Calendar.getInstance(), 5, app);

		return app;
	}


	public static App setupBaselineApplication()
	{
		  App app = new App ();
          
	      app.setAppName("myapp1");
	      app.setAppOwner("coolguy1");
	      app.setOrgName("eejot");
	      
	      
	      ApplicationConfigurationModel defaultModel = new ApplicationConfigurationModel();
	      
	      defaultModel.setAppConfigType(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);
	      
	      app.setDefaultAppConfig(defaultModel);  
	      
	      app = ServiceFactory.getApplicationService().createApplication(app);
	      return app;
	}


	public static void populateTestDataforLast1day (Long givenAppId) {
		int counter = 0;
		NetworkMetricsDBService service = ServiceFactory.getMetricsDBServiceInstance();
		String[] networkCarriers = {"att", "verizon", "sprint"};
		Long[] appId = {1l, 456l, 3l};
		String[] networkType = {"2g", "3g", "wifi"};
		String[] uids = {"alan", "prabhat", "ideawheel", "cloud", "amazon", "google"};
		String[] deviceIds = {"alan-iphone", "prabhat-droid", "ideawheel-ipad", "cloud-pc", "amazon-kindle", "google-nexus"};
		String[] wsUrls = {"http://eejot.org/students", "http://google.com/finance", "https://amazon.com/price",
				"http://ideawheel.com/metric", "http://jboss.org/rest", "https://twitter.com/tweet"};
		List<ClientNetworkMetrics> list = new ArrayList<ClientNetworkMetrics> ();
		Random generator = new Random();
		try {

			Calendar startTime = Calendar.getInstance();
			startTime.add(Calendar.DAY_OF_MONTH, -1);
			for (int k = 0; k < 24; k++) {//each hour in a day
				startTime.set(Calendar.HOUR_OF_DAY, k);
				for (int l = 0; l < 10; l++) {//put 10 random beans
					startTime.set(Calendar.MINUTE, generator.nextInt(60));
					startTime.set(Calendar.SECOND, generator.nextInt(60));	

					Calendar endTime =Calendar.getInstance();
					endTime.setTime(startTime.getTime());
					endTime.add(Calendar.SECOND, generator.nextInt(60));

					Date start = startTime.getTime();
					Date end = endTime.getTime();

					ClientNetworkMetrics bean = new ClientNetworkMetrics();

					int index = generator.nextInt(uids.length);

					if (givenAppId == 0)
					{
						bean.setAppId(appId[generator.nextInt(3)]);
					}
					else
					{
						bean.setAppId(givenAppId);
					}
					bean.setAppConfigType(configs[generator.nextInt(4)]);
					bean.setDeviceId(deviceIds[index]);
					bean.setDevicePlatform(devicePlatform[generator.nextInt(3)]);
					bean.setNetworkCarrier(networkCarriers[generator.nextInt(3)]);
					bean.setNetworkType(networkType[generator.nextInt(3)]);
					bean.setUrl(wsUrls[index]);
					bean.setStartTime(start);
					bean.setEndTime(end);
					bean.setLatency(new Long(generator.nextInt(10)));
					
					bean.setNumSamples(new Long(generator.nextInt(1)));
					bean.setNumErrors(new Long (generator.nextInt(2)));
					
					bean.setApplicationVersion(new Integer(generator.nextInt(2) + 1).toString());
					list.add (bean);				

					//For different start time otherwise too little gap
					//Thread.sleep(1);
					counter++;
					//            System.out.println("adding test bean " + bean.toString());
				}
			}
			service.saveNetworkMetricsInBatch(list);
		}
		catch (Exception ex)
		{
			System.out.println("Problem with default webservices records DB initialisation: " + ex.toString());
			return;
		}
		System.out.println ("Number of rows inserted " + counter);
		/*	Calendar tempTime = Calendar.getInstance();
		tempTime.add(Calendar.DAY_OF_MONTH, -2);
		System.out.println("Number of uniqute url " + MetricsDatabaseHelper.getUniqueUrlsFor(1l, tempTime.getTime(), null).size());*/
	}


	public static void populateDataForMinutes(int numMin, Calendar startTime, int numDevices, Long givenAppId) {
		Vector<Device> v = new Vector<Device> ();
		Device d = new Device ();
		d.setUniqueDeviceId (UUID.randomUUID().toString());
		d.setModel (deviceModel[generator.nextInt(deviceModel.length)]);
		d.setPlatform (devicePlatform[generator.nextInt(devicePlatform.length)]);		
		d.setNetworkCarrier (networkCarriers[generator.nextInt(networkCarriers.length)]);
		d.setNetworkType ( networkType[generator.nextInt(networkType.length)]);
		v.add(d);
		NetworkTestData.populateDataForMinutes(numMin, startTime, numDevices, givenAppId,v);
		
	}
	
	public static void populateDataForMinutes(int numMin, Calendar startTime, int numDevices, Long givenAppId, Vector<Device> devices )
	{


		NetworkMetricsDBService service = ServiceFactory.getMetricsDBServiceInstance();

		for (int j = 0; j < numMin ; j++)
		{
			startTime.add(Calendar.MINUTE, 1);
			for (int l = 0; l < numDevices; l++) {//put 10 random beans
				startTime.set(Calendar.SECOND, generator.nextInt(60));	

				Calendar endTime =Calendar.getInstance();
				endTime.setTime(startTime.getTime());
				endTime.add(Calendar.SECOND, generator.nextInt(60));

				Date start = startTime.getTime();
				Date end = endTime.getTime();

				ClientNetworkMetrics bean = new ClientNetworkMetrics();
				int index = generator.nextInt(devices.size());           
				Device d = devices.get(index);

				bean.setAppId(givenAppId);

				
				bean.setAppConfigType(configs[generator.nextInt(4)]);
				bean.setApplicationVersion(new Integer(generator.nextInt(2) + 1).toString());
				bean.setDeviceId(d.getUniqueDeviceId());
				bean.setNetworkCarrier(d.getNetworkCarrier());
				bean.setNetworkType(d.getNetworkType());
				bean.setDeviceModel(d.getModel());
				bean.setDevicePlatform(d.getPlatform());
			
				bean.setUrl(wsUrls[index]);
				bean.setStartTime(start);
				bean.setEndTime(end);
				bean.setTimeStamp(end);

				long latency = getSimulatedLatency(bean.getNetworkType(), bean.getNetworkCarrier(), bean.getUrl());
				double rate = getFailureRate(bean.getNetworkType(), bean.getNetworkCarrier(), bean.getUrl());

				bean.setLatency(latency*2);
				bean.setNumSamples(1L);

				Double numErrors = (rate * bean.getNumSamples());


				bean.setNumErrors( numErrors.longValue());
				
				service.saveNetworkMetrics(bean);
			}
		}
	}
	public static void populateCompactNetworkMetricsForMinutes(int numMin, Calendar startTime, int numDevices, Long givenAppId) {
		Vector<Device> v = new Vector<Device> ();
		Device d = new Device ();
		d.setUniqueDeviceId (UUID.randomUUID().toString());
		d.setModel (deviceModel[generator.nextInt(deviceModel.length)]);
		d.setPlatform (devicePlatform[generator.nextInt(devicePlatform.length)]);		
		d.setNetworkCarrier (networkCarriers[generator.nextInt(networkCarriers.length)]);
		d.setNetworkType ( networkType[generator.nextInt(networkType.length)]);
		v.add(d);
		NetworkTestData.populateCompactNetworkMetricsForMinutes(numMin, startTime, numDevices, givenAppId, v);
	}

	public static void populateCompactNetworkMetricsForMinutes(int numMin, Calendar startTime, int numDevices, Long givenAppId, Vector<Device> devices )
	{


		NetworkMetricsDBService service = ServiceFactory.getMetricsDBServiceInstance();
		List<MetricsChartCriteria> cqs = ServiceFactory.getNetworkMetricsChartCriteriaService().getDefaultChartCriteriaForApp(givenAppId);


		int cqSize = cqs.size();

		for (int j = 0; j < numMin ; j++)
		{
			startTime.add(Calendar.MINUTE, 1);
			for (int l = 0; l < numDevices; l++) {//put 10 random beans
				startTime.set(Calendar.SECOND, generator.nextInt(60));	

				Calendar endTime =Calendar.getInstance();
				endTime.setTime(startTime.getTime());
				endTime.add( Calendar.SECOND,  generator.nextInt(60));

				Date start = startTime.getTime();
				Date end = endTime.getTime();

				CompactNetworkMetrics bean = new CompactNetworkMetrics();

				int index = generator.nextInt(devices.size());           
				Device d = devices.get(index);

				bean.setAppId(givenAppId);


				bean.setChartCriteriaId(cqs.get(generator.nextInt(cqSize)).getId());
				bean.setAppConfigType(configs[generator.nextInt(4)]);
				bean.setApplicationVersion(new Integer(generator.nextInt(2) + 1).toString());
				bean.setDeviceId(d.getUniqueDeviceId());
				bean.setNetworkCarrier(d.getNetworkCarrier());
				bean.setNetworkType(d.getNetworkType());
				bean.setDeviceModel(d.getModel());
				bean.setDevicePlatform(d.getPlatform());
				bean.setTimeStamp(start);
				bean.setSamplePeriod(SamplePeriod.MINUTE);

				long latency = getSimulatedLatency(bean.getNetworkType(), bean.getNetworkCarrier(), null);
				double rate = getFailureRate(bean.getNetworkType(), bean.getNetworkCarrier(), null);

				bean.setMaxLatency(latency*2);
				bean.setMinLatency(latency/2);
				bean.setNumSamples(new Long(generator.nextInt(5)));

				Double numErrors = (rate * bean.getNumSamples());
				
				bean.setNumErrors( numErrors.longValue());
				bean.setSumLatency(bean.getNumSamples() * latency);
				int serverLatencyWeight = generator.nextInt(50) +10;					
				bean.setSumServerLatency((latency*serverLatencyWeight)/100);				
				service.saveCompactNetworkMetrics(bean);
				
			}
		}
	}


	public static void populateSessionDataForMinutes(int numMin, Calendar startTime, int numDevices, Long givenAppId )
	{


		NetworkMetricsDBService service = ServiceFactory.getMetricsDBServiceInstance();

		for (int j = 0; j < numMin ; j++)
		{
			startTime.add(Calendar.MINUTE, 1);
			for (int l = 0; l < numDevices; l++) {//put 10 random beans
				startTime.set(Calendar.SECOND, generator.nextInt(60));   

				Calendar endTime =Calendar.getInstance();
				endTime.setTime(startTime.getTime());
				endTime.add(Calendar.SECOND, generator.nextInt(60));

				Date start = startTime.getTime();
				Date end = endTime.getTime();

				ClientNetworkMetrics bean = new ClientNetworkMetrics();

				int index = generator.nextInt(uids.length);

				if (givenAppId == 0)
				{
					bean.setAppId(appId[generator.nextInt(3)]);
				}
				else
				{
					bean.setAppId(givenAppId);
				}
				bean.setAppConfigType(configs[generator.nextInt(4)]);
				bean.setApplicationVersion(new Integer(generator.nextInt(2) + 1).toString());
				bean.setDeviceId(deviceIds[index]);
				bean.setNetworkCarrier(networkCarriers[generator.nextInt(3)]);
				bean.setNetworkType(networkType[generator.nextInt(3)]);
				bean.setUrl(wsUrls[index]);
				bean.setStartTime(start);
				bean.setEndTime(end);

				long latency = getSimulatedLatency(bean.getNetworkType(), bean.getNetworkCarrier(), bean.getUrl());
				double rate = getFailureRate(bean.getNetworkType(), bean.getNetworkCarrier(), bean.getUrl());

				
				bean.setLatency(latency/2);
				bean.setNumSamples(new Long(generator.nextInt(5)));

				Double numErrors = (rate * bean.getNumSamples());


				bean.setNumErrors( numErrors.longValue());
				
				service.saveNetworkMetrics(bean);
			}
		}
	}



	public static double getFailureRate(String networkType, String networkCarrier, String url)
	{
		

		double multiplier = 0D;

		if(networkCarrier.equalsIgnoreCase("AT&T"))
		{
			multiplier = Math.abs(generator.nextGaussian())/5;
			multiplier += 1;
		}

		if(networkCarrier.equalsIgnoreCase("verizon"))
		{
			multiplier = Math.abs(generator.nextGaussian())/4;
		}

		if(networkCarrier.equalsIgnoreCase("sprint"))
		{
			multiplier = Math.abs(generator.nextGaussian())/3;
		}

		if(networkCarrier.equalsIgnoreCase("tmobile"))
		{
			multiplier = Math.abs(generator.nextGaussian())/2;
		}

		return multiplier;
	}


	/**
	 *  
	 * 
	 */
	public static long getSimulatedLatency(String networkType, String networkCarrier, String url)
	{


		long latency;

		latency = baseLatency + Math.abs(new Long(generator.nextInt(10)));

		Math.pow(1, 1);

		double multiplier = 1;


		if(networkType.equals("2g"))
		{
			multiplier += (1 + Math.abs(generator.nextGaussian()))*2;
			latency += 300;
		}

		if(networkType.equals("3g"))
		{
			multiplier += 1+Math.abs(generator.nextGaussian());
			latency += 100;
		}

		if(!networkType.equals("wifi"))
		{
			if(networkCarrier.equals("att"))
			{
				multiplier += (1+Math.abs(generator.nextGaussian()))/5;
			}

			if(networkCarrier.equals("verizon"))
			{
				multiplier += (1+Math.abs(generator.nextGaussian()))/4;
			}

			if(networkCarrier.equals("sprint"))
			{
				multiplier += (1+Math.abs(generator.nextGaussian()))/3;
			}

			if(networkCarrier.equals("tmobile"))
			{
				multiplier += (1+Math.abs(generator.nextGaussian()))/2;
				latency += 200;
			}
		}

		latency *= multiplier;

		return latency;
	}

	public static void populateSQSWithTestData(int numMin, Calendar startTime, int numDevices, App app )
	{
		log.info("Populating SQS with Test Data. Start Time : " + startTime.toString() +  
				" Duration : " + numMin +  "mins. AppId: " + app.getFullAppName() );
		MetricsInjestionServiceSQSImpl sqsImpl = (MetricsInjestionServiceSQSImpl)ServiceFactory.getMetricsInjestionServiceInstance();

		for (int j = 0; j < numMin ; j++)
		{
			startTime.add(Calendar.MINUTE, 1);
			for (int l = 0; l < numDevices; l++) {//put 10 random beans
				startTime.set(Calendar.SECOND, generator.nextInt(60));	

				Calendar endTime =Calendar.getInstance();
				endTime.setTime(startTime.getTime());
				endTime.add(Calendar.SECOND, generator.nextInt(60));

				Date start = startTime.getTime();
				Date end = endTime.getTime();

				ClientMetricsEnvelope envelope = generateWebServiceMetricsBeanMessageEnvelope(app.getInstaOpsApplicationId(),start, end);

				sqsImpl.sendData(app.getFullAppName(), envelope);
			}
		}
	}	

	public static void populateDataForComplexEventService(int numMin, Calendar startTime, int numDevices, Long givenAppId, ComplexEventProcessingService service )
	{

		long total = 0;

		SessionPseudoClock clock = service.getSession().getSessionClock();
		long clockCurrentTime = clock.getCurrentTime();

		long delta = startTime.getTimeInMillis() - clockCurrentTime;
		clock.advanceTime(delta, TimeUnit.MILLISECONDS);

		for (int j = 0; j < numMin ; j++)
		{



			List<ClientMetricsEnvelope> messages = new ArrayList<ClientMetricsEnvelope>();
			for (int l = 0; l < numDevices; l++) {//put 10 random beans
				startTime.set(Calendar.SECOND, generator.nextInt(60));	

				Calendar endTime =Calendar.getInstance();
				endTime.setTime(startTime.getTime());
				endTime.add(Calendar.SECOND, generator.nextInt(60));

				Date start = startTime.getTime();
				Date end = endTime.getTime();

				ClientMetricsEnvelope envelope = generateDenormalizedWebServiceMetricsBeanMessageEnvelope(givenAppId,start, end);
				messages.add(envelope);
				total++;
			}


			log.info("Inserting facts for min " + j + " total messages sent : " + total);

			service.processEvents(messages);

			startTime.add(Calendar.MINUTE, 1);
			clock.getCurrentTime();
			clock.advanceTime(1, TimeUnit.MINUTES);
		}
	}

	public static void populateDataForStatelessComplexEventService(int numMin, Calendar startTime, int numDevices, Long givenAppId, StatelessComplexEventProcessingService service )
	{

		long total = 0;


		for (int j = 0; j < numMin ; j++)
		{

			StatefulKnowledgeSession ks = service.createSession();

			SessionPseudoClock clock = ks.getSessionClock();
			long clockCurrentTime = clock.getCurrentTime();

			long delta = startTime.getTimeInMillis() - clockCurrentTime;
			clock.advanceTime(delta, TimeUnit.MILLISECONDS);


			List<ClientMetricsEnvelope> messages = new ArrayList<ClientMetricsEnvelope>();
			for (int l = 0; l < numDevices; l++) {//put 10 random beans
				startTime.set(Calendar.SECOND, generator.nextInt(60));	

				Calendar endTime =Calendar.getInstance();
				endTime.setTime(startTime.getTime());
				endTime.add(Calendar.SECOND, generator.nextInt(60));

				Date start = startTime.getTime();
				Date end = endTime.getTime();

				ClientMetricsEnvelope envelope = generateDenormalizedWebServiceMetricsBeanMessageEnvelope(givenAppId,start, end);
				ServiceFactory.getSessionDBService().saveSessionMetrics(envelope.getSessionMetrics());
				ServiceFactory.getLogDBService().saveLogs(envelope.getLogs());
				ServiceFactory.getMetricsDBServiceInstance().saveNetworkMetricsInBatch(envelope.getMetrics());
				messages.add(envelope);
				total++;
			}


			log.info("Inserting facts for min " + j + " total messages sent : " + total);

			service.processEvents(givenAppId,"foobar", messages,ks);

			startTime.add(Calendar.MINUTE, 1);
			clock.getCurrentTime();
			clock.advanceTime(1, TimeUnit.MINUTES);
		}
	}

	public static void populateDataForComplexEventService2(int numMin, Calendar startTime, int numDevices, Long givenAppId, ComplexEventProcessingService service )
	{

		long total = 0;

		SessionPseudoClock clock = service.getSession().getSessionClock();
		long clockCurrentTime = clock.getCurrentTime();

		long delta = startTime.getTimeInMillis() - clockCurrentTime;
		clock.advanceTime(delta, TimeUnit.MILLISECONDS);

		for (int j = 0; j < numMin ; j++)
		{


			startTime.add(Calendar.MINUTE, 1);

			List<ClientMetricsEnvelope> messages = new ArrayList<ClientMetricsEnvelope>();
			for (int l = 0; l < numDevices; l++) {//put 10 random beans
				startTime.set(Calendar.SECOND, 0);	

				Calendar endTime =Calendar.getInstance();
				endTime.setTime(startTime.getTime());
				endTime.add(Calendar.SECOND, 59);

				Date start = startTime.getTime();
				Date end = endTime.getTime();

				ClientMetricsEnvelope envelope = generateDenormalizedWebServiceMetricsBeanMessageEnvelope(givenAppId,start, end);
				messages.add(envelope);
				total++;
			}


			log.info("Inserting facts for min " + j + " total messages sent : " + total);

			service.processEvents(messages);


			clock.getCurrentTime();
			clock.advanceTime(1, TimeUnit.MINUTES);



		}
	}

	public static void populateDataForComplexEventServiceWithEventsInPast(int numMin, Calendar startTime, int numDevices, Long givenAppId, ComplexEventProcessingService service )
	{

		long total = 0;

		SessionPseudoClock clock = service.getSession().getSessionClock();
		long clockCurrentTime = clock.getCurrentTime();

		long delta = startTime.getTimeInMillis() - clockCurrentTime;
		clock.advanceTime(delta, TimeUnit.MILLISECONDS);

		for (int j = 0; j < numMin ; j++)
		{


			startTime.add(Calendar.MINUTE, 1);

			List<ClientMetricsEnvelope> messages = new ArrayList<ClientMetricsEnvelope>();
			for (int l = 0; l < numDevices; l++) {//put 10 random beans
				startTime.set(Calendar.SECOND, 0);	

				Calendar endTime =Calendar.getInstance();
				endTime.setTime(startTime.getTime());
				endTime.add(Calendar.SECOND, 59);

				Date start = startTime.getTime();
				Date end = endTime.getTime();

				ClientMetricsEnvelope envelope = generateDenormalizedWebServiceMetricsBeanMessageEnvelope(givenAppId,start, end);
				messages.add(envelope);



				total++;
			}


			log.info("Inserting facts for min " + j + " total messages sent : " + total);

			service.processEvents(messages);


			clock.getCurrentTime();
			clock.advanceTime(1, TimeUnit.MINUTES);



		}
	}

	public static ClientMetricsEnvelope generateWebServiceMetricsBeanMessageEnvelope(long givenAppId, Date start,Date end)
	{
		ClientMetricsEnvelope envelope = constructWebServiceMetricsBeanMessageEnvelop( givenAppId, start, end);

		List<ClientNetworkMetrics> beans = new ArrayList<ClientNetworkMetrics>();
		List<ClientLog> logs = new ArrayList<ClientLog>();



		for(int i=0; i < 10; i++ )
		{

			ClientNetworkMetrics bean = getGeneratedMetricBean(givenAppId,start,end);
			beans.add(bean);
		}
		//  ServiceFactory.getMetricsDBServiceInstance().saveNetworkMetricsInBatch(beans);

		for(int i=0; i < 10; i++ )
		{
			int logLevel = getSimulateLogLevel(envelope.getSessionMetrics().getDeviceModel(), envelope.getSessionMetrics().getDevicePlatform(), envelope.getSessionMetrics().getAppConfigType()); 

			ClientLog logRecord = generateLog(givenAppId,start,end, logLevel);
			logs.add(logRecord);
		}
		//ServiceFactory.getLogDBService().saveLogs(logs);

		//MetricsInjestionServiceSQSImpl.denormalizeClientSessionMetrics(envelope.getSessionMetrics(), beans, logs);


		envelope.setLogs(logs);
		envelope.setMetrics(beans);

		return envelope;
	}


	public static ClientMetricsEnvelope generateDenormalizedWebServiceMetricsBeanMessageEnvelope(long givenAppId, Date start,Date end)
	{
		ClientMetricsEnvelope envelope = generateWebServiceMetricsBeanMessageEnvelope(givenAppId,start,end);

		MetricsInjestionServiceSQSImpl.denormalizeClientMetrics(envelope.getSessionMetrics(), envelope.getMetrics(), envelope.getLogs());

		return envelope;
	}



	public static ClientNetworkMetrics getGeneratedMetricBean(long givenAppId, Date start,Date end)
	{
		ClientNetworkMetrics bean = new ClientNetworkMetrics();
		bean.setAppConfigType(configs[generator.nextInt(4)]);

		int index = generator.nextInt(uids.length);

		if (givenAppId == 0)
		{
			bean.setAppId(appId[generator.nextInt(3)]);
		}
		else
		{
			bean.setAppId(givenAppId);
		}
		bean.setDeviceId(deviceIds[index]);
		bean.setNetworkCarrier(networkCarriers[generator.nextInt(3)]);
		bean.setNetworkType(networkType[generator.nextInt(3)]);
		bean.setUrl(wsUrls[index]);

		bean.setStartTime((Date) start.clone());
		bean.setEndTime((Date) end.clone());
		bean.setTimeStamp((Date)end.clone());

		long latency = getSimulatedLatency(bean.getNetworkType(), bean.getNetworkCarrier(), bean.getUrl());
		double rate = getFailureRate(bean.getNetworkType(), bean.getNetworkCarrier(), bean.getUrl());

		
		bean.setLatency(latency/2);
		bean.setNumSamples(new Long(generator.nextInt(5) + 1));

		Double numErrors = (rate * bean.getNumSamples());


		bean.setNumErrors( numErrors.longValue());
		
		return bean;
	}


	public static ClientMetricsEnvelope constructWebServiceMetricsBeanMessageEnvelop(long givenAppId, Date start,Date end)
	{
		ClientMetricsEnvelope envelope = new ClientMetricsEnvelope();
		envelope.setInstaOpsApplicationId(givenAppId);	

		ClientSessionMetrics sessionMetrics = generateSessionMetrics(givenAppId, start, end);		

		envelope.setSessionMetrics(sessionMetrics);

		return envelope;

	}


	public static String[] logMessages = {"startup" , "pause", "resume", "end", "Dah dah dah", "WTF, What a terribe fault"};

	public static ClientLog generateLog(long givenAppId, Date start,Date end, int logLevel)
	{
		ClientLog logRecord = new ClientLog(); 

		int rand = generator.nextInt(5);
		logRecord.setAppId(givenAppId);
		logRecord.setLogLevel(ApigeeMobileAPMConstants.logLevelsString[logLevel]);
		logRecord.setLogMessage(logMessages[rand]);
		logRecord.setTimeStamp((Date) end.clone());
		logRecord.setDeviceId(Integer.toString(generator.nextInt(20)));

		//this is a hack !!!


		return logRecord;
	}

	public static int getSimulateLogLevel(String deviceModel, String devicePlatform, String configType)
	{
		//Seeds with random level
		int rand = generator.nextInt(8);

		if(deviceModel.equals("htc-thunder"))
		{
			rand += 1;
		}

		if(devicePlatform.equals("Android"))
		{
			rand += 1;
		}

		if(configType.equals(ApigeeMobileAPMConstants.CONFIG_TYPE_AB))
		{
			rand += 1;
		}

		if(rand > 7)
		{
			rand = 7;
		}

		return 7;
	}


	public static ClientSessionMetrics generateSessionMetrics(long givenAppId, Date start,Date end, String sessionId, String deviceId)
	{
		ClientSessionMetrics sessionMetrics = new ClientSessionMetrics(); 

		int rand = generator.nextInt(5);
		sessionMetrics.setDeviceId(deviceId);
		sessionMetrics.setSessionId(sessionId);
		sessionMetrics.setAppId(givenAppId);
		sessionMetrics.setApplicationVersion(version[generator.nextInt(version.length)]);
		sessionMetrics.setAppConfigType(configs[generator.nextInt(configs.length)]);
		sessionMetrics.setBearing(0F);
		sessionMetrics.setDeviceCountry(countries[generator.nextInt(countries.length)]);
		sessionMetrics.setDeviceModel(deviceModel[generator.nextInt(deviceModel.length)]);
		sessionMetrics.setDeviceOperatingSystem(deviceOperatingSystem[generator.nextInt(deviceOperatingSystem.length)]);
		sessionMetrics.setDevicePlatform(devicePlatform[generator.nextInt(devicePlatform.length)]);
		sessionMetrics.setBatteryLevel(generator.nextInt(10));
		sessionMetrics.setIsNetworkRoaming(generator.nextBoolean());
		sessionMetrics.setIsNetworkChanged(false);
		sessionMetrics.setLocalCountry(sessionMetrics.getNetworkCountry());
		sessionMetrics.setLocalLanguage(language[generator.nextInt(language.length)]);
		sessionMetrics.setLatitude(10D);
		sessionMetrics.setLongitude(20D);
		sessionMetrics.setNetworkCarrier(networkCarriers[generator.nextInt(networkCarriers.length)]);
		sessionMetrics.setNetworkCountry(sessionMetrics.getNetworkCountry());
		sessionMetrics.setNetworkType(networkType[generator.nextInt(networkType.length)]);
		sessionMetrics.setSessionStartTime(start);
		sessionMetrics.setTimeStamp(end);
		//sessionMetrics.set;


		return sessionMetrics;
	}

	public static ClientSessionMetrics generateSessionMetrics(long givenAppId, Date start,Date end)
	{
		int rand = generator.nextInt(5);
		return generateSessionMetrics(givenAppId, start, end, UUID.randomUUID().toString(),deviceIds[generator.nextInt(deviceIds.length)]);
	}

	Timer timer;
	TimerTask task;

	public static void sendContinousMetrics(String appID)
	{

	}


	public static void main(String[] args)
	{
		ClientNetworkMetrics m = new ClientNetworkMetrics();
		m.setStartTime(new Date());
		ServiceFactory.getMetricsDBServiceInstance().saveNetworkMetrics(m);
	}


}
