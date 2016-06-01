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
import org.apache.usergrid.apm.model.SessionChartCriteria;
import org.apache.usergrid.apm.service.ServiceFactory;



public class SessionChartCriteriaService
{
   
   private static final Log log = LogFactory.getLog(SessionChartCriteriaService.class);

   public static final String ACTIVE_USERS_SESSION = "Overview";
   public static final String ACTIVE_SESSION_BY_APP_VERSION = "by App Versions";
   public static final String ACTIVE_SESSION_BY_APP_CONFIG_TYPE = "by Config Overrides";   
   public static final String ACTIVE_SESSIONS_BY_DEVICE_PLATFORM = "by Platforms";
   public static final String ACTIVE_SESSIONS_BY_DEVICE_MODEL = "by Device Models";
   public static final String ACTIVE_SESSIONS_BY_OS_VERSION = "by OS Versions"; //aka deviceOperatingSystem
   public static final String AVERAGE_SESSION_TIME = "Session Duration"; //currently not being used
   
   public Long saveChartCriteria (SessionChartCriteria chart) {
      Session session = null;
      Transaction transaction = null;     
      try {
         session = ServiceFactory.getHibernateSession();       
         transaction = session.beginTransaction();
         session.saveOrUpdate(chart);  
         transaction.commit();
         log.info("Session Chart Criteria with id " + chart.getId() + " " + chart.getChartName() + " saved");
         return chart.getId();
      } catch (HibernateException e) {
    	  transaction.rollback();
         log.error(e);         
         throw new HibernateException("Cannot save chart criteria", e);
      } 

   }

   public void deleteChartCriteria(SessionChartCriteria chart) {
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
         throw new HibernateException("Cannot delete session chart criteria", e);
      } 
   }

   public void saveDefaultChartCriteriaForApp (Long appId) {
      
      
      SessionChartCriteria activeUserSession = new SessionChartCriteria();
      activeUserSession.setAppId(appId);
      activeUserSession.setChartName(SessionChartCriteriaService.ACTIVE_USERS_SESSION);   
      activeUserSession.setDescription("Total number of active users and sessions for a given time range");
      activeUserSession.setShowSessionCount(true);
      activeUserSession.setShowUsers(true);  
      //activeUserSession.setShowSessionTime(true);
      activeUserSession.setDefaultChart(true);
      
      
      SessionChartCriteria byAppVersion = new SessionChartCriteria ();
      byAppVersion.setAppId(appId);
      byAppVersion.setChartName(SessionChartCriteriaService.ACTIVE_SESSION_BY_APP_VERSION);
      byAppVersion.setDescription("Active Sessions grouped by app versions");
      byAppVersion.setGroupedByAppVersion(true);
      byAppVersion.setShowSessionCount(true);
      byAppVersion.setDefaultChart(true);      
            
      SessionChartCriteria byConfigType = new SessionChartCriteria ();
      byConfigType.setAppId(appId);
      byConfigType.setChartName(SessionChartCriteriaService.ACTIVE_SESSION_BY_APP_CONFIG_TYPE);     
      byConfigType.setDescription("Active Sessions Grouped by Configuration Types");
      byConfigType.setGroupedByAppConfigType(true);
      byConfigType.setShowSessionCount(true);
      byConfigType.setDefaultChart(true);
     
       
     
      SessionChartCriteria byDevicePlatform = new SessionChartCriteria ();
      byDevicePlatform.setAppId(appId);
      byDevicePlatform.setChartName(SessionChartCriteriaService.ACTIVE_SESSIONS_BY_DEVICE_PLATFORM);
      byDevicePlatform.setDescription("Active Sessions by Device Platform");
      byDevicePlatform.setGroupedbyDevicePlatform(true);
      byDevicePlatform.setShowSessionCount(true);
      byDevicePlatform.setDefaultChart(true);      
       
      SessionChartCriteria byDeviceModel = new SessionChartCriteria ();
      byDeviceModel.setAppId(appId);
      byDeviceModel.setChartName(SessionChartCriteriaService.ACTIVE_SESSIONS_BY_DEVICE_MODEL);     
      byDeviceModel.setDescription("Active Sessions by Device Model");
      byDeviceModel.setGroupedByDeviceModel(true);
      byDeviceModel.setShowSessionCount(true);
      byDeviceModel.setDefaultChart(true);
      
      SessionChartCriteria byPlatformVersion = new SessionChartCriteria ();
      byPlatformVersion.setAppId(appId);
      byPlatformVersion.setChartName(SessionChartCriteriaService.ACTIVE_SESSIONS_BY_OS_VERSION);     
      byPlatformVersion.setDescription("Active Sessions by Platform Version");
      byPlatformVersion.setGroupedByDeviceOS(true);
      byPlatformVersion.setShowSessionCount(true);
      byPlatformVersion.setDefaultChart(true);
      
     /* SessionChartCriteria avgSessionLength = new SessionChartCriteria ();
      avgSessionLength.setAppId(appId);
      avgSessionLength.setChartName(SessionChartCriteriaService.AVERAGE_SESSION_TIME);
      avgSessionLength.setDescription("Average session lengths");
      avgSessionLength.setShowSessionTime(true);
      avgSessionLength.setDefaultChart(true);
      */
     
      
      
         
      saveChartCriteria(activeUserSession);
      saveChartCriteria(byAppVersion);
      saveChartCriteria(byConfigType);      
      saveChartCriteria(byDevicePlatform);
      saveChartCriteria(byDeviceModel);
      saveChartCriteria(byPlatformVersion);
      //saveChartCriteria(avgSessionLength);
      log.info("6 default session chart criteria for app " + appId + " saved");
      
   }

   public SessionChartCriteria getChartCriteria (Long chartId) {
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

         SessionChartCriteria result = (SessionChartCriteria)session.get(SessionChartCriteria.class, chartId);
         transaction.commit();         
         return result;       
      } catch (Exception e) {       
         e.printStackTrace();       
         transaction.rollback();
         throw new HibernateException("Cannot get session chart criteria : ", e);
      } 
  
   }
   
   public SessionChartCriteria getDefaultChartCriteriaByName (Long appId, String chartName) {
      List<SessionChartCriteria> cqs = getDefaultChartCriteriaForApp (appId);
      SessionChartCriteria chartCriteria = null;
      for (SessionChartCriteria cq : cqs) {
         if (cq.getChartName().equals(chartName)) {
            chartCriteria = cq;
            break;
         }
      }
      return chartCriteria;
   }

   @SuppressWarnings("unchecked")
   public List<SessionChartCriteria> getDefaultChartCriteriaForApp (Long appId) {
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
         Criteria crit = session.createCriteria(SessionChartCriteria.class);
         crit.add(new AppsFilter(appId).getCriteria());
         crit.add(Restrictions.eq("defaultChart",true));
         List<SessionChartCriteria> result = crit.list();         
         transaction.commit();         
         return result;       
      } catch (Exception e) {       
         e.printStackTrace();       
         transaction.rollback();
         throw new HibernateException("Cannot get default session chart criteria for app : " + appId, e);
      } 
   }
   
   @SuppressWarnings("unchecked")
   public List<SessionChartCriteria> getVisibleChartCriteriaForApp (Long appId) {
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
         Criteria crit = session.createCriteria(SessionChartCriteria.class);
         crit.add(new AppsFilter(appId).getCriteria());
         crit.add(Restrictions.eq("visible",true));
         List<SessionChartCriteria> result = crit.list();         
         transaction.commit();         
         return result;       
      } catch (Exception e) {       
         e.printStackTrace();       
         transaction.rollback();
         throw new HibernateException("Cannot get default session chart criteria for app : " + appId, e);
      } 
   }

   
}
