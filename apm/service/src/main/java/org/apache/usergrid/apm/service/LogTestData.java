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

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;

import org.apache.usergrid.apm.model.ClientLog;
import org.apache.usergrid.apm.model.ApigeeMobileAPMConstants;
import org.apache.usergrid.apm.model.ChartCriteria.SamplePeriod;
import org.apache.usergrid.apm.model.CompactClientLog;
import org.apache.usergrid.apm.model.LogChartCriteria;

public class LogTestData
{

	static String[] networkCarriers = {"att", "verizon", "sprint", "tmobile"};
	static Long[] appId = {1l, 456l, 3l};
	static String[] networkType = {"2g", "3g", "wifi"};
	static long baseLatency = 50L;
	static String[] uids = {"alan", "prabhat", "ideawheel", "cloud", "amazon", "google"};
	static String[] deviceIds = {"apple-iphone", "motorola-droid", "apple-ipad", "cloud-pc", "amazon-kindle", "google-nexus"};
	static String[] telephoneDeviceId = {"alan-iphone", "prabhat-droid", "ideawheel-ipad", "cloud-pc", "amazon-kindle", "google-nexus"};
	static String[] deviceModel = {"apple-iphone4g","apple-ipad","samsung-galaxytab","htc-thunder","google-nexusone"};
	//static String[] deviceModels = {"apple","samsung","htc","google"};
	static String[] devicePlatform = {"android","ios","blackberry"};   

	static String[] configs = {ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT ,
		ApigeeMobileAPMConstants.CONFIG_TYPE_DEVICE_LEVEL, ApigeeMobileAPMConstants.CONFIG_TYPE_DEVICE_TYPE,
		ApigeeMobileAPMConstants.CONFIG_TYPE_AB};

	static String[] tags = {"UI","NETWORK","ANIMATION"};


	public static final int ASSERT = 7;
	public static final int ERROR = 6;
	public static final int WARN = 5;
	public static final int INFO = 4;
	public static final int DEBUG = 3;
	public static final int VERBOSE = 2;


	/*public static String[] assertMessages = {"assert1", "assert2", "another assert 3", "something goofy", " good that it's happening"};
   public static String[] warnMessages = {"warning1", "warning2", "another warning 3", "something warning", " app hit weird case"};
   public static String[] errorMessages = {"error1", "error2", "another error 3", "mysterious error", "could not find the key"};
   public static String[] infoMessages = {"important info" , "OK, look at me", "Really, Look at me", "I'm warning you", "Dah dah dah", "WTF, What a terribe fault"};
   public static String[] debugMessages = {"currency is dollar" , "called method1", "method2 has 2 params", "oh .may need to implement it", "Doh Doh"};
   public static String[] verboseMessages = {"verbose1" , "verbose2 most", "verbose3 much", "verbose4 worse", "Doh Doh verbose good"};*/

	public static String[][] messages = {
		{"verbose1" , "verbose2 most", "verbose3 much", "verbose4 worse", "Doh Doh verbose good"}, //2
		{"currency is dollar" , "called method1", "method2 has 2 params", "oh .may need to implement it", "Doh Doh"}, //3
		{"important info" , "OK, look at me", "Really, Look at me", "I'm warning you", "Dah dah dah", "WTF, What a terribe fault"}, //4
		{"warning1", "warning2", "another warning 3", "something warning", " app hit weird case"}, //5
		{"error1", "error2", "another error 3", "mysterious error", "could not find the key"}, //6
		{"assert1", "assert2", "another assert 3", "something goofy", " good that it's happening"} //7      
	};



	static Random generator = new Random();

	public static void populateLogForMinutes(int numMin, Calendar startTime, int numDevices, Long givenAppId ) {

		Vector<Device> v = new Vector<Device> ();
		Device d = new Device ();
		d.setUniqueDeviceId (UUID.randomUUID().toString());
		d.setModel (deviceModel[generator.nextInt(deviceModel.length)]);
		d.setPlatform (devicePlatform[generator.nextInt(devicePlatform.length)]);		
		d.setNetworkCarrier (networkCarriers[generator.nextInt(networkCarriers.length)]);
		d.setNetworkType ( networkType[generator.nextInt(networkType.length)]);
		v.add(d);
		LogTestData.populateLogForMinutes(numMin, startTime, numDevices, givenAppId, v);

	}


	public static void populateLogForMinutes(int numMin, Calendar startTime, int numDevices, Long givenAppId, Vector<Device> devices )
	{


		LogDBService service = ServiceFactory.getLogDBService();

		List<LogChartCriteria> cqs = ServiceFactory.getLogChartCriteriaService().getDefaultChartCriteriaForApp(givenAppId);


		int cqSize = cqs.size();

		for (int j = 0; j < numMin ; j++)
		{
			startTime.add(Calendar.MINUTE, 1);
			for (int l = 0; l < numDevices; l++) {
				startTime.set(Calendar.SECOND, generator.nextInt(60));   

				Calendar endTime =Calendar.getInstance();
				endTime.setTime(startTime.getTime());
				endTime.add(Calendar.SECOND, generator.nextInt(60));

				Date start = startTime.getTime();
				Date end = endTime.getTime();

				CompactClientLog logRecord = new CompactClientLog();
				int index = generator.nextInt(devices.size());           
				Device d = devices.get(index);

				logRecord.setAppId(givenAppId);

				logRecord.setChartCriteriaId(cqs.get(generator.nextInt(cqSize)).getId());
				logRecord.setAppConfigType(configs[generator.nextInt(4)]);
				logRecord.setApplicationVersion(new Integer(generator.nextInt(2) + 1).toString());
				logRecord.setDeviceId(d.getUniqueDeviceId());
				logRecord.setNetworkCarrier(d.getNetworkCarrier());
				logRecord.setNetworkType(d.getNetworkType());
				logRecord.setDeviceModel(d.getModel());
				logRecord.setDevicePlatform(d.getPlatform());

				logRecord.setSamplePeriod(SamplePeriod.MINUTE);

				logRecord.setTimeStamp(start);            
				logRecord.setAssertCount(new Long (generator.nextInt(5)));
				logRecord.setDebugCount(new Long (generator.nextInt(5)));
				logRecord.setErrorAndAboveCount(new Long (generator.nextInt(5)));
				logRecord.setErrorCount(Math.max(logRecord.getErrorAndAboveCount() -1,1));
				logRecord.setWarnCount(new Long (generator.nextInt(5)));
				logRecord.setInfoCount(new Long (generator.nextInt(5)));          

				service.saveCompactLog(logRecord);
			}
		}
	}

	public static void populateRawLogs(int numMin, Calendar startTime, int numDevices, Long givenAppId)  {
		Vector<Device> v = new Vector<Device> ();
		Device d = new Device ();
		d.setUniqueDeviceId (UUID.randomUUID().toString());
		d.setModel (deviceModel[generator.nextInt(deviceModel.length)]);
		d.setPlatform (devicePlatform[generator.nextInt(devicePlatform.length)]);		
		d.setNetworkCarrier (networkCarriers[generator.nextInt(networkCarriers.length)]);
		d.setNetworkType ( networkType[generator.nextInt(networkType.length)]);
		v.add(d);
		LogTestData.populateRawLogs(numMin, startTime, numDevices, givenAppId, v);
		
	}

	public static void populateRawLogs(int numMin, Calendar startTime, int numDevices, Long givenAppId, Vector <Device> devices ) {

		LogDBService service = ServiceFactory.getLogDBService();
		for (int j = 0; j < numMin ; j++)
		{
			startTime.add(Calendar.MINUTE, 1);
			for (int l = 0; l < numDevices; l++) {//put 10 random logRecords
				startTime.set(Calendar.SECOND, generator.nextInt(60));   

			Calendar endTime =Calendar.getInstance();
			endTime.setTime(startTime.getTime());
			endTime.add(Calendar.SECOND, generator.nextInt(60));

			Date start = startTime.getTime();
			Date end = endTime.getTime();

			ClientLog logRecord = new ClientLog();
			int index = generator.nextInt(devices.size());           
			Device d = devices.get(index);
			logRecord.setAppId(givenAppId);

			int rand = generator.nextInt(5);

			logRecord.setAppConfigType(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);
			logRecord.setApplicationVersion(new Integer(generator.nextInt(2) + 1).toString());
			logRecord.setDeviceId(d.getUniqueDeviceId());
			logRecord.setNetworkCarrier(d.getNetworkCarrier());
			logRecord.setNetworkType(d.getNetworkType());
			logRecord.setDeviceModel(d.getModel());
			logRecord.setDevicePlatform(d.getPlatform());
			logRecord.setTimeStamp(start);          
			int level = generator.nextInt(5);
			logRecord.setLogLevel(ApigeeMobileAPMConstants.logLevelsString[level + 2]);
			logRecord.setLogMessage(messages[level][generator.nextInt(5)]);
			logRecord.setTag(tags[generator.nextInt(3)]);

			service.saveLog(logRecord);
			}
		}
	}



	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// TODO Auto-generated method stub

	}

}
