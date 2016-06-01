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

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;



public class QuartzScheduler implements SchedulerService {

	private static final Log log = LogFactory.getLog(QuartzScheduler.class);

	/**
	 * 
	 */
	private static final Object factoryMutex = new Object();

	/**
	 * 
	 */
	private static Scheduler scheduler;


	public Scheduler getScheduler() {
		SchedulerFactory schedulerFactory = null;	
		if (scheduler == null) {
			synchronized (factoryMutex) {
				try {
					// Load the scheduler config and create a scheduler factory out out it 
					Properties quartzConfigs = new Properties();
					quartzConfigs.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("conf/quartz.properties"));
					schedulerFactory =  new StdSchedulerFactory(quartzConfigs);					
					scheduler = schedulerFactory.getScheduler();
					//TODO: Need to determine if scheduler should be started right away. I think so but not sure.
					scheduler.start();					
					log.info("Scheduler successfully obtained");         

				} catch (SchedulerException se) {
					se.printStackTrace();
				} catch (IOException exc) {
					// TODO Auto-generated catch block
					exc.printStackTrace();
				}
			}

		}
		return scheduler;
	}


	@Override
	public void start() {
		synchronized (factoryMutex) {
			try {
				scheduler.start();
			} catch (SchedulerException exc) {
				log.error("Could not start the scheduler");
				exc.printStackTrace();
			}
		}
		
	}


	@Override
	public void stop() {
		synchronized (factoryMutex) {
			try {
				scheduler.shutdown();
			} catch (SchedulerException exc) {
				log.error("Could not stop the scheduler");
				exc.printStackTrace();
			}
		}
		
	}
	
	
}
