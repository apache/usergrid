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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.apache.usergrid.apm.model.ApigeeMobileAPMConstants;
import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.model.ClientLog;
import org.apache.usergrid.apm.model.ClientMetricsEnvelope;
import org.apache.usergrid.apm.model.ClientNetworkMetrics;
import org.apache.usergrid.apm.model.ClientSessionMetrics;
import org.apache.usergrid.apm.service.DeploymentConfig;
import org.apache.usergrid.apm.service.Device;
import org.apache.usergrid.apm.service.MetricsInjestionServiceSQSImpl;
import org.apache.usergrid.apm.service.ServiceFactory;



public class GenerateSimulatedMobileData
{

	private static final Log log = LogFactory.getLog(GenerateSimulatedMobileData.class);

	static String[] networkCarriers = {"AT&T", "VERIZON", "SPRINT", "TMOBILE"};	
	static String[] networkType = {"MOBILE", "WIFI"};
	static String[] networkSubType = {"GSM","CDMA","SIP","GPRS","EDGE","UMTS","EVDO_0","EVDO_A","RTT","HSDPA","HSUPA","HSPA","IDEN","EVDO_B","LTE","EHRPD","HSPAP"};
	static Long[] httpStatusCodes = {401L,403L,408L,410L,500L,501L,502L,504L};
	static long baseLatency = 1500L;	
	static String[] wsUrls = {"https://api.twitter.com/1.1/statuses/mentions_timeline.json",
		"https://api.twitter.com/1.1/statuses/update.json?status=Posting%20from%20%40apigee's%20API%20test%20console.%20It's%20like%20a%20command",
		"https://graph.facebook.com/search?q=apigee&type=post",
		"https://graph.facebook.com/2439131959/accounts", 
		"https://www.eventbrite.com/json/event_search", 
		"https://api.foursquare.com/v2/users/leaderboard?v=20120321",
		"https://api.foursquare.com/v2/venues/add?v=20120321",
		"https://api.foursquare.com/v2/venues/suggestcompletion?v=20120321",
		"https://api.foursquare.com/v2/venues/trending?v=20120321",
		"https://www.googleapis.com/adsense/v1.1/accounts"};
	static String[] deviceModel = {"iPhone 4S","iPhone 4", "iPhone 3GS","iPad 2", "iPad-3G (4G)", "GT-P3113","Desire HD","Galaxy Nexus","Nexus 7"};
	static String[] deviceOperatingSystem = {"6.0","5.1.1","4.0.4","5.0","6.0.1","3.2","2.3.5","4.1.2","4.1.1"};
	static String[] devicePlatform = {"iPhone OS","iPhone OS", "iPhone OS","iPhone OS","iPhone OS","Android", "Android", "Android", "Android"};
	static String[] countries = {"USA","Canada","Nepal","Greece"};
	static String[] language = {"en-US","en-CA","fr-FR", "fr-AG", "np-NP"};
	static String[] version = {"1.2.7","2.0.1","3.0.5","4.3.0"};
	static String[] configs = {ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT,
		ApigeeMobileAPMConstants.CONFIG_TYPE_DEVICE_LEVEL,
		ApigeeMobileAPMConstants.CONFIG_TYPE_DEVICE_TYPE,
		ApigeeMobileAPMConstants.CONFIG_TYPE_AB};
		

	static Random generator = new Random();
	
	public static void sendCrashData(App app, Vector<MobileSession> sessions)  {
		System.out.println("Sending crash data for: " + app.getFullAppName()) ;
		int sessionIndex = generator.nextInt(sessions.size());
		MobileSession s = sessions.get(sessionIndex);
		ClientMetricsEnvelope envelope = constructMessageEnvelop( app, s);
		ClientLog logRecord = generateCrashLog(app,s, ClientLog.ASSERT);
		List<ClientLog> logs = new ArrayList<ClientLog> ();
		logs.add(logRecord);
		envelope.setLogs(logs);
		//first upload the crash file
		uploadSimulatedCrashLogsToS3(app, logRecord.getLogMessage());
		s.setEndTime(Calendar.getInstance().getTime());
		
		MetricsInjestionServiceSQSImpl sqsImpl = (MetricsInjestionServiceSQSImpl)ServiceFactory.getMetricsInjestionServiceInstance();
		sqsImpl.sendData(app.getFullAppName(), envelope);
		
	}
	
	public static boolean uploadSimulatedCrashLogsToS3 (App app, String fileName) {
		DeploymentConfig config = DeploymentConfig.geDeploymentConfig();
		AWSCredentials credentials = new BasicAWSCredentials( config.getAccessKey(), config.getSecretKey() );
		AmazonS3Client s3Client = new AmazonS3Client( credentials );
		PutObjectRequest putObjectRequest;


		String s3FullFileName = config.getEnvironment()+"/"+ config.getS3CrashLogFolder() + "/"+ app.getFullAppName()+"/"+fileName;
		String sampleCrashFileName = null;
		
		if (fileName.endsWith(".crash")) //it's an iOS crash file
			sampleCrashFileName = "ios-crash-log-example.txt";
		else if (fileName.endsWith(".stacktrace"))  
				sampleCrashFileName = "android-crash-log-example.txt";
		try {

			ObjectMetadata metaData = new ObjectMetadata();

			metaData.setHeader(Headers.S3_CANNED_ACL, CannedAccessControlList.AuthenticatedRead);
			
			
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(sampleCrashFileName);

			putObjectRequest = new PutObjectRequest(config.getS3LogBucket(), 
					s3FullFileName,is, null); 
					//new ByteArrayInputStream(    //fileName.getBytes("UTF-8")),null);
			PutObjectResult result = s3Client.putObject(putObjectRequest);
			return true;


		} catch (Exception e) {
			e.printStackTrace();	         
		}	   
		return false;
	}

	public static void populateSQSWithTestData(int numSessions, App app, 
			Vector<MobileSession> sessions, Vector<Device> devices)
	{
		System.out.println("Populating SQS with Test Data for AppId: " + app.getFullAppName() + " with total number of " + numSessions + " sessions" );
		MetricsInjestionServiceSQSImpl sqsImpl = (MetricsInjestionServiceSQSImpl)ServiceFactory.getMetricsInjestionServiceInstance();

		for (int j = 0; j < numSessions ; j++)
		{
			MobileSession s = sessions.get(j);
			ClientMetricsEnvelope envelope = generateMetrics(app, s);
			s.setEndTime(Calendar.getInstance().getTime());
			sqsImpl.sendData(app.getFullAppName(), envelope);				
			System.out.println("Sent data for session " + s.getSessionId());
			
			//clean payload that have already been sent
			envelope.setMetrics(null);
			envelope.setLogs(null);
			envelope.setSessionMetrics(null);
			envelope=null;
		}
	}	


	public static ClientMetricsEnvelope generateMetrics(App app, MobileSession s)
	{
		ClientMetricsEnvelope envelope = constructMessageEnvelop( app, s);

		List<ClientNetworkMetrics> networkMetrics = new ArrayList<ClientNetworkMetrics>();
		List<ClientLog> logs = new ArrayList<ClientLog>();
		int numNetworkEntry = 1;
		for(int i=0; i < numNetworkEntry; i++ )
		{
			ClientNetworkMetrics bean = getGeneratedMetricBean(app.getInstaOpsApplicationId(),s,envelope.getSessionMetrics());
			networkMetrics.add(bean);
		}

		int numLogEntry = 2;
		for(int i=0; i < numLogEntry; i++ )
		{
			int logLevel = getSimulateLogLevel(envelope.getSessionMetrics().getDeviceModel(), envelope.getSessionMetrics().getDevicePlatform(), envelope.getSessionMetrics().getAppConfigType()); 

			ClientLog logRecord = generateLog(app.getInstaOpsApplicationId(),s, logLevel);
			logs.add(logRecord);
		}

		envelope.setLogs(logs);
		envelope.setMetrics(networkMetrics);		

		return envelope;
	}	

	public static ClientMetricsEnvelope constructMessageEnvelop(App app, MobileSession s)
	{
		ClientMetricsEnvelope envelope = new ClientMetricsEnvelope();
		envelope.setInstaOpsApplicationId(app.getInstaOpsApplicationId());	
		envelope.setFullAppName(app.getFullAppName());
		envelope.setOrgName(app.getOrgName());
		envelope.setAppName(app.getAppName());
		ClientSessionMetrics sessionMetrics = generateSessionMetrics(app.getInstaOpsApplicationId(), s);
		envelope.setSessionMetrics(sessionMetrics);		
		return envelope;

	}

	public static ClientSessionMetrics generateSessionMetrics(long givenAppId, MobileSession s)
	{
		ClientSessionMetrics sessionMetrics = new ClientSessionMetrics();		
		sessionMetrics.setSessionId(s.getSessionId());
		Device d = s.getDevice();
		sessionMetrics.setDeviceId(d.getUniqueDeviceId());		
		sessionMetrics.setAppId(givenAppId);
		sessionMetrics.setApplicationVersion(version[generator.nextInt(version.length)]);
		sessionMetrics.setAppConfigType(configs[generator.nextInt(configs.length)]);
		sessionMetrics.setBearing(0F);
		sessionMetrics.setDeviceCountry(d.getCountry());
		
		sessionMetrics.setDeviceModel(d.getModel());
		sessionMetrics.setDevicePlatform(d.getPlatform());
		
		sessionMetrics.setDeviceOSVersion(d.getPlatformVersion());		
		sessionMetrics.setBatteryLevel(generator.nextInt(10));
		sessionMetrics.setIsNetworkRoaming(generator.nextBoolean());
		sessionMetrics.setIsNetworkChanged(false);
		sessionMetrics.setLocalCountry(sessionMetrics.getNetworkCountry());
		sessionMetrics.setLocalLanguage(language[generator.nextInt(language.length)]);
		sessionMetrics.setLatitude(10D);
		sessionMetrics.setLongitude(20D);		
		sessionMetrics.setNetworkCountry(sessionMetrics.getNetworkCountry());		
		sessionMetrics.setNetworkType(d.getNetworkType());
		sessionMetrics.setNetworkSubType(d.getNetworkSubType());
		sessionMetrics.setNetworkCarrier(d.getNetworkCarrier());
		sessionMetrics.setTelephonyNetworkOperatorName(d.getNetworkCarrier());		
			
		sessionMetrics.setSessionStartTime(s.getStartTime());
		
		Calendar cal = Calendar.getInstance();
		sessionMetrics.setTimeStamp(cal.getTime()); //this sets the sessionEndTime as well.		
		return sessionMetrics;
	}
	
	public static ClientLog generateCrashLog(App app, MobileSession s, int logLevel)
	{
				
		String fileName = UUID.randomUUID().toString(); 
		ClientLog log = new ClientLog();
		if (s.getDevice().getPlatform().equals("Android"))
			log.setLogMessage(fileName+".stacktrace");
		else if (s.getDevice().getPlatform().contains("iPhone"))
			log.setLogMessage(fileName+".crash");
		log.setLogLevel(ApigeeMobileAPMConstants.logLevelsString[ApigeeMobileAPMConstants.LOG_ASSERT]);
		log.setTag("CRASH");
		log.setTimeStamp(Calendar.getInstance().getTime());
		
		return log;
	}

	public static ClientLog generateLog(long givenAppId, MobileSession s, int logLevel)
	{
		ClientLog logRecord = new ClientLog();
		SimulatedLog l = SimulatedLog.getSimulatedLog(logLevel);
		
		logRecord.setLogLevel(ApigeeMobileAPMConstants.logLevelsString[l.logLevel]);
		logRecord.setLogMessage(l.logMessage);
		logRecord.setTag(l.tag);
		logRecord.setTimeStamp(Calendar.getInstance().getTime());
		return logRecord;
	}


	public static ClientNetworkMetrics getGeneratedMetricBean(Long givenAppId, MobileSession s, ClientSessionMetrics csm)
	{
		ClientNetworkMetrics bean = new ClientNetworkMetrics();
		//bean.setAppConfigType(configs[generator.nextInt(4)]);

		int index = generator.nextInt(wsUrls.length);

		bean.setUrl(wsUrls[index]);

		Calendar prevEnd = Calendar.getInstance();
		prevEnd.setTime(s.getEndTime());
		
		Calendar end  = Calendar.getInstance();
		
		int diff = (int) (end.getTimeInMillis() - prevEnd.getTimeInMillis())/1000; //diff in seconds
		
		Calendar startTime = (Calendar) prevEnd.clone();
		startTime.add(Calendar.SECOND, diff/2);
		bean.setStartTime(startTime.getTime());
		
		Calendar endTime = (Calendar) startTime.clone();
		endTime.add(Calendar.SECOND, diff/3);
		bean.setEndTime( endTime.getTime());
		bean.setTimeStamp(endTime.getTime());

		long latency = getSimulatedLatency(csm.getNetworkSubType(), csm.getTelephonyNetworkOperatorName(), bean.getUrl());
		int numError = getFailureRate(csm.getNetworkSubType(), csm.getTelephonyNetworkOperatorName(), bean.getUrl());
		
		Long statusCode;
		if (numError == 0)
			statusCode= 200L;
		else
			statusCode = httpStatusCodes[generator.nextInt(httpStatusCodes.length)];
		bean.setHttpStatusCode(statusCode);
		bean.setLatency(latency);		
		bean.setNumSamples(1L);		
		bean.setNumErrors( Long.valueOf(numError));
		//set server side latency
		int serverLatencyWeight = generator.nextInt(50) +10;
		bean.setServerProcessingTime((latency*serverLatencyWeight)/100);		

		return bean;
	}



	public synchronized static void expireSessions (Vector<MobileSession> sessions, int numSessionsToExpire) {
		int currentNumberOfSessions = sessions.size();
		System.out.println ("Expiring " + numSessionsToExpire + " old sessions");
		int index;
		for (int i = 0; i < numSessionsToExpire; i++) {
			index = generator.nextInt(currentNumberOfSessions-i);
			MobileSession s = sessions.get(index);
			Device d = s.getDevice();		
			d.setInUse(false);
			sessions.remove(index);
			s= null;
		}
		sessions.trimToSize();
	}

	public synchronized static void addNewSessions(Vector<MobileSession> sessions, Vector<Device> devices, int numSessionToAdd) {
		
		int numDevices = devices.size();
		int j = 0;
		System.out.println ("Adding " + numSessionToAdd + " new sessions");
		Device d;
		while (j < numSessionToAdd) {
			d = devices.get(generator.nextInt(numDevices));
			if (!d.isInUse()) {				
				d.setInUse(true);
				MobileSession s = new MobileSession();
				s.setDevice(d);
				sessions.add(s);
				j++;
			}
		}
		sessions.trimToSize();		 

	}


	public static int getSimulateLogLevel(String deviceModel, String devicePlatform, String configType)
	{
		//Seeds with random level
		int rand = generator.nextInt(5);
		rand = rand+3;

		if(deviceModel.equals("Desire HD"))
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
			rand = 6; //set back to error level
		}

		return rand;
	}	

	public static void generateDevices(Vector<Device> devices, int numDevices)  {
		
		Device d = null;
		int randomNumber = 0;
		for (int i = 0; i < numDevices; i++) {
			d = new Device ();
			d.setUniqueDeviceId (UUID.randomUUID().toString());
			randomNumber = generator.nextInt(deviceModel.length);
			d.setModel (deviceModel[randomNumber]);
			d.setPlatform (devicePlatform[randomNumber]);
			d.setPlatformVersion( deviceOperatingSystem[randomNumber]);
			d.setCountry ( countries[generator.nextInt(countries.length)]);
			
			d.setNetworkType ( networkType[generator.nextInt(networkType.length)]);
			if (!d.getNetworkType().equals("MOBILE")) {
				d.setNetworkCarrier("UNKNOWN");
				d.setNetworkSubType("UNKNOWN");
			}
			else {
				d.setNetworkCarrier (networkCarriers[generator.nextInt(networkCarriers.length)]);
				if (d.getPlatform().equalsIgnoreCase("ANDROID"))
				  d.setNetworkSubType(networkSubType[generator.nextInt(networkSubType.length)]);
				  else
					  d.setNetworkSubType("UNKNOWN");
			}
			devices.add(d);
		}
  
	}



	public static int getFailureRate(String nSubType, String nTelOperatorName, String url)
	{

		int r = generator.nextInt(10);
		if (nSubType.equals("WIFI")) //less error for wifi
			return (r < 2)?1: 0;

		if(nTelOperatorName.equals("AT&T"))
		{
			return (r < 3)?1: 0;
		}

		if(nTelOperatorName.equals("VERIZON"))
		{
			return (r < 4)?1: 0;
		}

		if(nTelOperatorName.equals("SPRINT"))
		{
			return (r < 5)?1: 0;
		}

		if(nTelOperatorName.equals("TMOBILE"))
		{
			return (r < 5)?1: 0;
		}

		return 0;
	}


	/**
	 *  
	 * 
	 */
	public static long getSimulatedLatency(String nSubType, String nTelOperatorName, String url)
	{


		long latency;

		latency = baseLatency + new Long(generator.nextInt(300));

		//Math.pow(1, 1);

		double multiplier = 1;


		if(nSubType.equals("EDGE"))
		{
			multiplier += (1 + Math.abs(generator.nextGaussian()))*2;
			latency += 300;
		}

		if(nSubType.equals("LTE"))
		{
			multiplier += 1+Math.abs(generator.nextGaussian());
			latency += 100;
		}

		if(!nSubType.equals("WIFI"))
		{
			if(nTelOperatorName.equals("AT&T"))
			{
				multiplier += (1+Math.abs(generator.nextGaussian()))/5;
			}

			if(nTelOperatorName.equals("VERIZON"))
			{
				multiplier += (1+Math.abs(generator.nextGaussian()))/4;
			}

			if(nTelOperatorName.equals("SPRINT"))
			{
				multiplier += (1+Math.abs(generator.nextGaussian()))/3;
			}

			if(nTelOperatorName.equals("TMOBILE"))
			{
				multiplier += (1+Math.abs(generator.nextGaussian()))/2;
				latency += 200;
			}
		}

		latency *= multiplier;

		return latency;
	} 


}
