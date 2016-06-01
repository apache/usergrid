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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

/**
 * 
 * @author prabhat jha
 *
 */

public abstract class CommonHibernateServiceBackup {
	
	/**
	 * 
	 */
	private static final Object factoryMutex = new Object();
	
	/**
	 * 
	 */
	private static SessionFactory sessionFactory;
	private static int jdbcBatchSize = 1;
	
	
	@SuppressWarnings("unused")
	protected Session getHibernateSession() {
		if (sessionFactory == null) {
			synchronized (factoryMutex) {
				AnnotationConfiguration configuration = new AnnotationConfiguration().configure("/conf/hibernate_ws.cfg.xml");
				
				String jdbcBatchSizeString = configuration.getProperty("hibernate.jdbc.batch_size");
				
				if(jdbcBatchSizeString != null)
				{
					setJdbcBatchSize(Integer.parseInt(jdbcBatchSizeString));
				}
				 
				sessionFactory = configuration.buildSessionFactory();
			}
			//TODO: Need other way to start the Quartz scheduler..it's a hack
			((QuartzScheduler)ServiceFactory.getSchedulerService()).getScheduler();
		}
		return sessionFactory.getCurrentSession();
	}


	/**
	 * @param jdbcBatchSize the jdbcBatchSize to set
	 */
	protected static void setJdbcBatchSize(int jdbcBatchSize) {
		//CommonHibernateService.jdbcBatchSize = jdbcBatchSize;
	}


	/**
	 * @return the jdbcBatchSize
	 */
	protected static int getJdbcBatchSize() {
		return jdbcBatchSize;
	}
	

}
