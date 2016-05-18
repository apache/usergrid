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
