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
import java.util.UUID;
import java.util.Vector;

import org.apache.usergrid.apm.model.ApigeeMobileAPMConstants;
import org.apache.usergrid.apm.model.ClientSessionMetrics;
import org.apache.usergrid.apm.model.ChartCriteria.SamplePeriod;
import org.apache.usergrid.apm.model.CompactSessionMetrics;
import org.apache.usergrid.apm.model.SessionChartCriteria;

public class SessionTestData
{



	static String[] networkCarriers = {"att", "verizon", "sprint", "tmobile"};
	static Long[] appId = {1l, 456l, 3l};
	static String[] networkType = {"2g", "3g", "wifi"};
	static long baseLatency = 50L;
	static String[] uids = {"alan", "prabhat", "ideawheel", "cloud", "amazon", "google"};
	static String[] deviceIds = {"apple-iphone", "motorola-droid", "apple-ipad", "cloud-pc", "amazon-kindle", "google-nexus"};
	static String[] telephoneDeviceId = {"alan-iphone", "prabhat-droid", "ideawheel-ipad", "cloud-pc", "amazon-kindle", "google-nexus"};
	static String[] deviceModel = {"apple-iphone4g","apple-ipad","samsung-galaxytab","htc-thunder","google-nexusone"};
	static String[] devicePlatform = {"android","ios","blackberry"};   
		
	static Long[] sessionLengths = {2000L,1000L, 15000L,20000L,25000L,2020L,1500L, 15700L,28900L,23400L};

	static String[] configs = {ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT,
		ApigeeMobileAPMConstants.CONFIG_TYPE_DEVICE_LEVEL,
		ApigeeMobileAPMConstants.CONFIG_TYPE_DEVICE_TYPE,
		ApigeeMobileAPMConstants.CONFIG_TYPE_AB};

	public static enum OutputType{DB,CSV};


	static Random generator = new Random();


	public static void populateSessionDataForMinutes(int numMin, Calendar startTime, int numDevices, Long givenAppId ) {
		Vector<Device> v = new Vector<Device> ();
		Device d = new Device ();
		d.setUniqueDeviceId (UUID.randomUUID().toString());
		d.setModel (deviceModel[generator.nextInt(deviceModel.length)]);
		d.setPlatform (devicePlatform[generator.nextInt(devicePlatform.length)]);		
		d.setNetworkCarrier (networkCarriers[generator.nextInt(networkCarriers.length)]);
		d.setNetworkType ( networkType[generator.nextInt(networkType.length)]);
		v.add(d);
		SessionTestData.populateSessionDataForMinutes(numMin, startTime, numDevices, givenAppId, v);
		
	}
	public static void populateSessionDataForMinutes(int numMin, Calendar startTime, int numDevices, Long givenAppId, Vector<Device> devices )
	{


		SessionDBService service = ServiceFactory.getSessionDBService();

		List<SessionChartCriteria> cqs = ServiceFactory.getSessionChartCriteriaService().getDefaultChartCriteriaForApp(givenAppId);


		int cqSize = cqs.size();

		List<CompactSessionMetrics> csms = null;
		for (int j = 0; j < numMin ; j++)
		{
			csms = new ArrayList<CompactSessionMetrics>();
			startTime.add(Calendar.MINUTE, 1);       
			for (int l = 0; l < numDevices; l++) {//put 10 random beans
				startTime.set(Calendar.SECOND, generator.nextInt(60));   

				Calendar endTime =Calendar.getInstance();
				endTime.setTime(startTime.getTime());
				endTime.add(Calendar.SECOND, generator.nextInt(60));

				Date start = startTime.getTime();
				Date end = endTime.getTime();

				CompactSessionMetrics bean = new CompactSessionMetrics();

				int index = generator.nextInt(devices.size());
				bean.setAppId(givenAppId);
				Device d = devices.get(index);


				bean.setChartCriteriaId(cqs.get(generator.nextInt(cqSize)).getId());           
				bean.setDeviceId(d.getUniqueDeviceId());
				bean.setNetworkCarrier(d.getNetworkCarrier());
				bean.setNetworkType(d.getNetworkType());
				bean.setDeviceModel(d.getModel());
				bean.setDevicePlatform(d.getPlatform());
				bean.setTimeStamp(start); 
				bean.setSumSessionLength(5 + sessionLengths[generator.nextInt(10)]);   
				bean.setSamplePeriod(SamplePeriod.MINUTE);
				bean.setSessionCount(new Long(generator.nextInt(10) +3));
				bean.setNumUniqueUsers(bean.getSessionCount() - Math.min(bean.getSessionCount(), generator.nextInt(2)));           
				bean.setApplicationVersion(new Integer(generator.nextInt(2) + 1).toString());
				bean.setAppConfigType(configs[generator.nextInt(4)]);
				csms.add(bean);            
			}
			service.saveCompactSessionMetrics(csms);
		}

	}

	public static void populateSessionDataForHours(int numHour, Calendar startTime, int numDevices, Long givenAppId )
	{


		SessionDBService service = ServiceFactory.getSessionDBService();

		List<SessionChartCriteria> cqs = ServiceFactory.getSessionChartCriteriaService().getDefaultChartCriteriaForApp(givenAppId);


		int cqSize = cqs.size();

		List<CompactSessionMetrics> csms = new ArrayList<CompactSessionMetrics> ();
		for (int j = 0; j < numHour ; j++)
		{
			startTime.add(Calendar.HOUR, 1);       
			for (int l = 0; l < numDevices; l++) {
				startTime.set(Calendar.MINUTE, generator.nextInt(60));   

				Calendar endTime =Calendar.getInstance();
				endTime.setTime(startTime.getTime());
				endTime.add(Calendar.MINUTE, generator.nextInt(60));

				Date start = startTime.getTime();
				Date end = endTime.getTime();

				CompactSessionMetrics bean = new CompactSessionMetrics();

				int index = generator.nextInt(uids.length);

				if (givenAppId == 0)
				{
					bean.setAppId(appId[generator.nextInt(3)]);
				}
				else
				{
					bean.setAppId(givenAppId);
				}

				bean.setChartCriteriaId(cqs.get(generator.nextInt(cqSize)).getId());           
				bean.setDeviceId(deviceIds[index]);
				bean.setNetworkCarrier(networkCarriers[generator.nextInt(4)]);
				bean.setNetworkType(networkType[generator.nextInt(3)]);
				bean.setDeviceModel(deviceModel[generator.nextInt(5)]);
				bean.setDevicePlatform(devicePlatform[generator.nextInt(3)]);
				bean.setTimeStamp(start); 
				bean.setSumSessionLength(5 + sessionLengths[generator.nextInt(10)]);   
				bean.setSamplePeriod(SamplePeriod.MINUTE);
				bean.setSessionCount(new Long(generator.nextInt(10) +3));
				bean.setNumUniqueUsers(bean.getSessionCount() - Math.min(bean.getSessionCount(), generator.nextInt(2)));           
				bean.setApplicationVersion(new Integer(generator.nextInt(2) + 1).toString());
				bean.setAppConfigType(configs[generator.nextInt(4)]);
				csms.add(bean);            
			}
		}
		service.saveCompactSessionMetrics(csms);
	}





	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		ClientSessionMetrics m = new ClientSessionMetrics();
		ServiceFactory.getSessionDBService().saveSessionMetrics(m);

	}

}
