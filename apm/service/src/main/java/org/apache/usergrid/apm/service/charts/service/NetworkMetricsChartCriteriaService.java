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
package org.apache.usergrid.apm.service.charts.service;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import org.apache.usergrid.apm.service.charts.filter.AppsFilter;
import org.apache.usergrid.apm.model.MetricsChartCriteria;
import org.apache.usergrid.apm.service.ServiceFactory;


public class NetworkMetricsChartCriteriaService {

	private static final Log log = LogFactory.getLog(NetworkMetricsChartCriteriaService.class);

	public static final String NETWORK_OVERVIEW = "Overview";

	public static final String NETWORK_ERRORS_BY_NETWORK_TYPE= "by Network Types";


	public static final String NETWORK_ERRORS_BY_CARRIER= "by Carriers";

	public static final String NETWORK_ERRORS_BY_APP_VERSION= "by App Versions";


	public static final String NETWORK_ERRORS_BY_APP_CONFIG= "by Config Overrides";

	public static final String NETWORK_ERRORS_BY_PLATFROMS= "by Platforms";

	public static final String NETWORK_ERRORS_BY_OS_VERSIONS = "by OS Versions";

	public static final String NETWORK_ERRORS_BY_DOMAIN= "by Domain";

	/**
	 * value of 2000 means only the request where latency is bigger than 2 seconds
	 */
	public static final Long LARGE_LATENCY_THRESHOLD = 2000L;

	/**
	 * if max latency is more than 20 seconds we don't include them on average latency calculation
	 */

	public static final Long LATENCY_OUTLIER_THRESHOLD = 20000L;



	public MetricsChartCriteria getChartCriteria(Long chartId) {
		if (chartId == null) {
			log.error ("Can not get a chart with null chart Id");
			return null;
		}

		log.info("Getting chart criteria with id: " + chartId);
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();

			MetricsChartCriteria result = (MetricsChartCriteria)session.get(MetricsChartCriteria.class, chartId);
			transaction.commit();         
			return result;       
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get chart criteria : ", e);
		} 
	}

	@SuppressWarnings("unchecked")   
	public List<MetricsChartCriteria> getChartCriteriaForUser(String userName) {
		if (userName == null) {
			log.error ("Can not get an chart with null chart Id");
			return null;
		}

		log.info("Getting all saved chart criteria for user: " + userName);
		List<MetricsChartCriteria> cqs = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();

			Query query = session.createQuery("from MetricsChartCriteria as cq where cq.userName = :userName");
			query.setParameter("userName", userName);

			cqs = (List<MetricsChartCriteria>) query.list();
			transaction.commit();      

		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get chart criteria : ", e);
		} 

		return cqs;       
	}



	public void deleteChartCriteria(MetricsChartCriteria chart) {
		Session session = null;
		Transaction transaction = null;     
		log.info("Chart Criteria with id " + chart.getId() + " " + chart.getChartName() + " is going to be deleted");
		try {
			session = ServiceFactory.getHibernateSession();       
			transaction = session.beginTransaction();
			session.delete(chart);
			transaction.commit();
		} catch (HibernateException e) {
			transaction.rollback();
			log.error(e);
			throw new HibernateException("Cannot delete chart criteria", e);
		} 

	}


	public void saveDefaultChartCriteriaForApp (Long appId) {
		//Request Error Latency
		MetricsChartCriteria overview = new MetricsChartCriteria ();
		overview.setAppId(appId);
		overview.setChartName(NetworkMetricsChartCriteriaService.NETWORK_OVERVIEW);   
		overview.setDescription("Network Performance for a given time range");
		overview.setShowAll(true);
		overview.setDefaultChart(true);

		//Network Errors & Latency by Network Type
		MetricsChartCriteria byNetworkType = new MetricsChartCriteria ();
		byNetworkType.setAppId(appId);
		byNetworkType.setChartName(NetworkMetricsChartCriteriaService.NETWORK_ERRORS_BY_NETWORK_TYPE);
		byNetworkType.setDescription("Network Performance for different network types");
		byNetworkType.setShowAll(true);
		byNetworkType.setGroupedByNetworkType(true);
		byNetworkType.setDefaultChart(true);


		//Network Errors and Latency by Carrier
		MetricsChartCriteria byNetworkCarrier = new MetricsChartCriteria ();
		byNetworkCarrier.setAppId(appId);
		byNetworkCarrier.setChartName(NetworkMetricsChartCriteriaService.NETWORK_ERRORS_BY_CARRIER);
		byNetworkCarrier.setDescription("Network Performance for different network types");
		byNetworkCarrier.setShowAll(true);
		byNetworkCarrier.setGroupedByNetworkCarrier(true);
		byNetworkCarrier.setDefaultChart(true);  


		//Network Errors and Latency by App Versions
		MetricsChartCriteria byAppVersion = new MetricsChartCriteria ();
		byAppVersion.setAppId(appId);
		byAppVersion.setChartName(NetworkMetricsChartCriteriaService.NETWORK_ERRORS_BY_APP_VERSION);
		byAppVersion.setDescription("Network Performance for different app versions");
		byAppVersion.setShowAll(true);
		byAppVersion.setGroupedByAppVersion(true);
		byAppVersion.setDefaultChart(true); 
		byAppVersion.setVisible(false);


		//TODO: Chart Criteria by app configurations need to have app version specified. 
		//TODO: Better would to be to set app version id from UI and do that for all other chart criteria as well 

		//Network Errors and Latency by App Configurations
		MetricsChartCriteria byAppConfig = new MetricsChartCriteria ();
		byAppConfig.setAppId(appId);
		byAppConfig.setChartName(NetworkMetricsChartCriteriaService.NETWORK_ERRORS_BY_APP_CONFIG);
		byAppConfig.setDescription("Network Performance for different app configurations");
		byAppConfig.setShowAll(true);
		byAppConfig.setGroupedByAppConfigType(true);
		byAppConfig.setDefaultChart(true); 
		byAppConfig.setVisible(false);

		MetricsChartCriteria byPlatform = new MetricsChartCriteria ();
		byPlatform.setAppId(appId);
		byPlatform.setChartName(NetworkMetricsChartCriteriaService.NETWORK_ERRORS_BY_PLATFROMS);
		byPlatform.setDescription("Network Performance for different Platforms");
		byPlatform.setShowAll(true);
		byPlatform.setGroupedbyDevicePlatform(true);
		byPlatform.setDefaultChart(true);		

		MetricsChartCriteria byOSVersion = new MetricsChartCriteria ();
		byOSVersion.setAppId(appId);
		byOSVersion.setChartName(NetworkMetricsChartCriteriaService.NETWORK_ERRORS_BY_OS_VERSIONS);
		byOSVersion.setDescription("Network Performance for different OS versions");
		byOSVersion.setShowAll(true);
		byOSVersion.setGroupedByDeviceOS(true);
		byOSVersion.setDefaultChart(true);

		MetricsChartCriteria byDomain = new MetricsChartCriteria ();
		byDomain.setAppId(appId);
		byDomain.setChartName(NetworkMetricsChartCriteriaService.NETWORK_ERRORS_BY_DOMAIN);
		byDomain.setDescription("Network Performance for different domains");
		byDomain.setShowAll(true);
		byDomain.setGroupedByDomain(true);
		byDomain.setDefaultChart(true);		


		saveChartCriteria(overview);
		saveChartCriteria(byNetworkType);
		saveChartCriteria(byNetworkCarrier);		
		saveChartCriteria(byAppVersion);
		saveChartCriteria(byAppConfig);
		saveChartCriteria(byPlatform);
		saveChartCriteria(byOSVersion);
		saveChartCriteria(byDomain);

		log.info("8 default chart criteria for app " + appId + " saved");
	}

	@SuppressWarnings("unchecked")

	public List<MetricsChartCriteria> getDefaultChartCriteriaForApp(Long appId) {
		if (appId == null) {
			log.error ("Can not get an chart with null app Id");
			return null;
		}

		//log.info("Getting default chart criteria for app: " + appId);
		Session session = null;
		Transaction transaction = null;     
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(MetricsChartCriteria.class);
			crit.add(new AppsFilter(appId).getCriteria());
			crit.add(Restrictions.eq("defaultChart",true));
			List<MetricsChartCriteria> result = crit.list();         
			transaction.commit();         
			return result;       
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get default chart criteria for app : " + appId, e);
		} 

	}

	public List<MetricsChartCriteria> getVisibleChartCriteriaForApp(Long appId) {
		if (appId == null) {
			log.error ("Can not get an chart with null app Id");
			return null;
		}

		//log.info("Getting default chart criteria for app: " + appId);
		Session session = null;
		Transaction transaction = null;     
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(MetricsChartCriteria.class);
			crit.add(new AppsFilter(appId).getCriteria());
			crit.add(Restrictions.eq("visible",true));
			List<MetricsChartCriteria> result = crit.list();         
			transaction.commit();         
			return result;       
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get default chart criteria for app : " + appId, e);
		} 

	}


	public MetricsChartCriteria getNamedChartCriteriaForUser(String chartName, String userName) {
		// TODO Auto-generated method stub
		return null;
	}


	public Long saveChartCriteria(MetricsChartCriteria chart) {
		Session session = null;
		Transaction transaction = null;     
		try {
			session = ServiceFactory.getHibernateSession();       
			transaction = session.beginTransaction();
			session.saveOrUpdate(chart);  
			transaction.commit();
			log.info("Chart Criteria with id " + chart.getId() + " " + chart.getChartName() + " saved");
			return chart.getId();
		} catch (HibernateException e) {
			log.error(e);
			transaction.rollback();
			throw new HibernateException("Cannot save chart criteria", e);
		} 

	}


	/*public MetricsChartCriteria getErrorLogChartCriteriaForApp(Long appId) {
      if (appId == null) {
         log.error ("Can not get an chart with null app Id");
         return null;
      }

      log.info("Getting default chart criteria for app: " + appId);
      Session session = null;
      Transaction transaction = null;     
      try {
         session = ServiceFactory.getHibernateSession();
         transaction = session.beginTransaction();
         Criteria crit = session.createCriteria(MetricsChartCriteria.class);
         crit.add(new AppsFilter(appId).getCriteria());
         crit.add(Restrictions.eq("chartName",NetworkMetricsChartCriteriaService.LOG_ERRORS));
         List<MetricsChartCriteria> result = crit.list();         
         transaction.commit();         
         if(result.size() == 1)
         {
            return result.get(0);
         } else
         {
            throw new Exception("Did not find just one default chart call Log Errors. Num results:" + result.size() );
         }
      } catch (Exception e) {       
         e.printStackTrace();       
         transaction.rollback();
         throw new HibernateException("Cannot get default chart criteria for app : " + appId, e);
      } 
   }

	 */   
	public MetricsChartCriteria getDefaultChartCriteriaByName (Long appId, String chartName)  {
		List<MetricsChartCriteria> cqs = getDefaultChartCriteriaForApp (appId);
		MetricsChartCriteria chartCriteria = null;
		for (MetricsChartCriteria cq : cqs) {
			if (cq.getChartName().equals(chartName)) {
				chartCriteria = cq;
				break;
			}
		}
		return chartCriteria;
	}


}
