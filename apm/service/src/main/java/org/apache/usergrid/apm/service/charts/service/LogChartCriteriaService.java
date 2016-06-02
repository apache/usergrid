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
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import org.apache.usergrid.apm.service.charts.filter.AppsFilter;
import org.apache.usergrid.apm.model.LogChartCriteria;
import org.apache.usergrid.apm.service.ServiceFactory;

public class LogChartCriteriaService
{

	private static final Log log = LogFactory.getLog(LogChartCriteriaService.class);

	public static final String LOG_ERRORS = "Overview";
	public static final String APP_ERRORS_BY_APP_VERSION = "by App Versions";
	public static final String APP_ERRORS_BY_APP_CONFIG_TYPE = "by Config Overrides";
	public static final String APP_ERRORS_BY_DEVICE_PLATFORM = "by Platforms";
	public static final String APP_ERRORS_BY_DEVICE_MODEL = "by Device Models";
	public static final String APP_ERRORS_BY_OS_VERSION = "by OS Versions";

	public Long saveChartCriteria (LogChartCriteria chart) {
		Session session = null;
		Transaction transaction = null;     
		try {
			session = ServiceFactory.getHibernateSession();       
			transaction = session.beginTransaction();
			session.saveOrUpdate(chart);   
			transaction.commit();
			log.info("App Diagnostic Chart Criteria with id " + chart.getId() + " " + chart.getChartName() + " saved");
			return chart.getId();
		} catch (HibernateException e) {
			log.error(e);
			transaction.rollback();
			throw new HibernateException("Cannot save app diagnostic chart criteria", e);
		} 

	}

	public void deleteChartCriteria(LogChartCriteria chart) {
		Session session = null;
		Transaction transaction = null;     
		log.info("Chart Criteria with id " + chart.getId() + " " + chart.getChartName() + " is going to be deleted");
		try {
			session = ServiceFactory.getHibernateSession();       
			transaction = session.beginTransaction();
			session.delete(chart);
			transaction.commit();
		} catch (HibernateException e) {
			log.error(e);
			transaction.rollback();
			throw new HibernateException("Cannot delete app diagnostic chart criteria", e);
		} 
	}

	public void saveDefaultChartCriteriaForApp (Long appId) {


		LogChartCriteria logErrors = new LogChartCriteria();
		logErrors.setAppId(appId);
		logErrors.setChartName(LogChartCriteriaService.LOG_ERRORS);   
		logErrors.setDescription("Total number of anomaly for time period");
		//only compact client logs which has error count 1 or higher
		logErrors.setErrorAndAboveCount(1l);
		logErrors.setShowErrorAndAboveCount(true);
		logErrors.setDefaultChart(true);


		LogChartCriteria byAppVersion = new LogChartCriteria ();
		byAppVersion.setAppId(appId);
		byAppVersion.setChartName(LogChartCriteriaService.APP_ERRORS_BY_APP_VERSION);   
		byAppVersion.setDescription("Grouped by app versions");
		//only compact client logs which has error count 1 or higher
		byAppVersion.setErrorAndAboveCount(1l);
		byAppVersion.setShowErrorAndAboveCount(true);
		byAppVersion.setGroupedByAppVersion(true);
		byAppVersion.setDefaultChart(true);


		LogChartCriteria byConfigTypes = new LogChartCriteria ();
		byConfigTypes.setAppId(appId);
		byConfigTypes.setChartName(LogChartCriteriaService.APP_ERRORS_BY_APP_CONFIG_TYPE);     
		byConfigTypes.setDescription("Grouped by Configuration Types");
		byConfigTypes.setGroupedByAppConfigType(true);
		//only compact client logs which has error count 1 or higher
		byConfigTypes.setErrorAndAboveCount(1l);
		byConfigTypes.setShowErrorAndAboveCount(true);
		byConfigTypes.setDefaultChart(true);



		LogChartCriteria byDevicePlatform = new LogChartCriteria ();
		byDevicePlatform.setAppId(appId);
		byDevicePlatform.setChartName(LogChartCriteriaService.APP_ERRORS_BY_DEVICE_PLATFORM);     
		byDevicePlatform.setDescription("Grouped by Device Platforms");
		byDevicePlatform.setGroupedbyDevicePlatform(true);
		//only compact client logs which has error count 1 or higher
		byDevicePlatform.setErrorAndAboveCount(1l);
		byDevicePlatform.setShowErrorAndAboveCount(true);
		byDevicePlatform.setDefaultChart(true);

		LogChartCriteria byDeviceModel = new LogChartCriteria ();
		byDeviceModel.setAppId(appId);
		byDeviceModel.setChartName(LogChartCriteriaService.APP_ERRORS_BY_DEVICE_MODEL);     
		byDeviceModel.setDescription("Grouped by Device Model");
		byDeviceModel.setGroupedByDeviceModel(true);
		//only compact client logs which has error count 1 or higher
		byDeviceModel.setErrorAndAboveCount(1l);
		byDeviceModel.setShowErrorAndAboveCount(true);      
		byDeviceModel.setDefaultChart(true); 

		LogChartCriteria byPlatformVersion = new LogChartCriteria ();
		byPlatformVersion.setAppId(appId);
		byPlatformVersion.setChartName(LogChartCriteriaService.APP_ERRORS_BY_OS_VERSION);     
		byPlatformVersion.setDescription("Grouped by Platform Versions");
		byPlatformVersion.setGroupedByDeviceOS(true);
		//only compact client logs which has error count 1 or higher
		byPlatformVersion.setErrorAndAboveCount(1l);
		byPlatformVersion.setShowErrorAndAboveCount(true);      
		byPlatformVersion.setDefaultChart(true);    


		saveChartCriteria(logErrors);
		saveChartCriteria(byAppVersion);
		saveChartCriteria(byConfigTypes);
		saveChartCriteria(byDevicePlatform);      
		saveChartCriteria(byDeviceModel);
		saveChartCriteria(byPlatformVersion);
		log.info("6 default app diagnostic chart criteria for app " + appId + " saved");

	}

	public LogChartCriteria getChartCriteria (Long chartId) {
		if (chartId == null) {
			log.error ("Can not get a chart with null chart Id");
			return null;
		}

		log.info("Getting chart criteria with id: " + chartId);
		Session session = null;
		Transaction transaction = null;
		LogChartCriteria result = null;
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();
			result = (LogChartCriteria)session.get(LogChartCriteria.class, chartId);
			transaction.commit();    

		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get session chart criteria : ", e);
		} 
		return result;

	}

	@SuppressWarnings("unchecked")
	public List<LogChartCriteria> getDefaultChartCriteriaForApp (Long appId) {
		if (appId == null) {
			log.error ("Can not get an chart with null app Id");
			return null;
		}

		log.debug("Getting default session chart criteria for app: " + appId);
		Session session = null;
		Transaction transaction = null;     
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(LogChartCriteria.class);
			crit.add(new AppsFilter(appId).getCriteria());
			crit.add(Restrictions.eq("defaultChart",true));
			List<LogChartCriteria> result = crit.list();         
			transaction.commit();         
			return result;       
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get default session chart criteria for app : " + appId, e);
		} 
	}

	@SuppressWarnings("unchecked")
	public List<LogChartCriteria> getVisibleChartCriteriaForApp (Long appId) {
		if (appId == null) {
			log.error ("Can not get an chart with null app Id");
			return null;
		}

		log.debug("Getting default session chart criteria for app: " + appId);
		Session session = null;
		Transaction transaction = null;     
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(LogChartCriteria.class);
			crit.add(new AppsFilter(appId).getCriteria());
			crit.add(Restrictions.eq("visible",true));
			List<LogChartCriteria> result = crit.list();         
			transaction.commit();         
			return result;       
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get default session chart criteria for app : " + appId, e);
		} 
	}

	public LogChartCriteria getDefaultChartCriteriaByName (Long appId, String chartName)  {
		List<LogChartCriteria> cqs = getDefaultChartCriteriaForApp (appId);
		LogChartCriteria chartCriteria = null;
		for (LogChartCriteria cq : cqs) {
			if (cq.getChartName().equals(chartName)) {
				chartCriteria = cq;
				break;
			}
		}
		return chartCriteria;
	}


}
