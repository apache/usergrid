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
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.apache.usergrid.apm.model.DeviceModel;
import org.apache.usergrid.apm.model.DevicePlatform;
import org.apache.usergrid.apm.model.NetworkCarrier;
import org.apache.usergrid.apm.model.NetworkSpeed;

/**
 * Scheduler that periodically checks if new environment values such as network carrier, device models have been added in
 * SummarySessionMetrics.  If so, it adds the new values into appropriate tables. 
 * @author prabhat
 *
 */

public class EnvironmentOverrideValueUpdater implements Job {

	private static final Log log = LogFactory.getLog(EnvironmentOverrideValueUpdater.class);

	private ApplicationService appService = ServiceFactory.getApplicationService();
	private SessionDBService sessionService = ServiceFactory.getSessionDBService();
	private Hashtable<String,DeviceModel> deviceModelCache = null;
	private Hashtable<String,DevicePlatform> devicePlatformCache = null;
	private Hashtable<String,NetworkCarrier> networkCarrierCache = null;
	private Hashtable<String, NetworkSpeed> networkTypeCache = null;
	private boolean initialized = false;

	/**
	 * Find new env variables in last 10 min and insert them to DB
	 */
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		if (!initialized) {
			init ();
		}

		Calendar c = Calendar.getInstance();
		c.add (Calendar.MINUTE, -10);//looking for new data in last 10 min

		List<String> devMod = sessionService.getDistinctValuesFromSummarySessionMetrics("deviceModel", c.getTime());
		List<String> devPlat = sessionService.getDistinctValuesFromSummarySessionMetrics("devicePlatform", c.getTime());
		List<String> netCarr = sessionService.getDistinctValuesFromSummarySessionMetrics("networkCarrier", c.getTime());
		List<String> netType = sessionService.getDistinctValuesFromSummarySessionMetrics("networkType", c.getTime());

		for (String s : devMod) {
			if (s != null && deviceModelCache.get(s) == null) {
				log.info("found new device model in summary session");
				appService.saveDeviceModel(new DeviceModel(null,s));
				deviceModelCache.put(s, new DeviceModel(null,s));
			}
		}

		for (String s : devPlat) {
			if (s != null && devicePlatformCache.get(s) == null) {
				log.info("found new device platform in summary session");
				appService.saveDevicePlatform(new DevicePlatform(null,s));
				devicePlatformCache.put(s, new DevicePlatform(null,s));
			}
		}

		for (String s : netCarr) {
			if (s != null && networkCarrierCache.get(s) == null) {
				log.info("found new network carrier in summary session");
				appService.saveNetworkCarrier(new NetworkCarrier(null,s));
				networkCarrierCache.put(s, new NetworkCarrier(null,s));
			}
		}

		for (String s : netType) {
			if (s != null && networkTypeCache.get(s) == null) {
				log.info("found new networkType/speed in summary session");
				appService.saveNetworkSpeed(new NetworkSpeed(null,s));
				networkTypeCache.put(s, new NetworkSpeed(null,s));
			}
		}
	}


	/**
	 * Load all env specific values that are already in DB
	 */
	private void init () {
		log.info ("Initializing  environment parameters we already have in DB. This should happen only once after server start");
		if (deviceModelCache == null) {
			deviceModelCache = new Hashtable<String, DeviceModel>();
			List<DeviceModel> list = appService.getDeviceModels(null);
			for (DeviceModel m : list) {
				deviceModelCache.put(m.getDeviceModel(), m);
			}
		}

		if (devicePlatformCache == null) {
			devicePlatformCache = new Hashtable<String, DevicePlatform>();
			List<DevicePlatform> list = appService.getDevicePlatforms(null);
			for (DevicePlatform p : list) {
				devicePlatformCache.put(p.getDevicePlatform(), p);
			}
		}

		if (networkCarrierCache == null) {
			networkCarrierCache = new Hashtable<String, NetworkCarrier>();
			List<NetworkCarrier> list = appService.getNetworkCarriers(null);
			for (NetworkCarrier c : list) {
				networkCarrierCache.put(c.getNetworkCarrier(), c);
			}
		}

		if (networkTypeCache == null) {
			networkTypeCache = new Hashtable<String, NetworkSpeed>();
			List<NetworkSpeed> list = appService.getNetworkSpeeds(null);
			for (NetworkSpeed s : list) {
				networkTypeCache.put(s.getNetworkSpeed(), s);
			}
		}
		initialized = true;

	}

}
