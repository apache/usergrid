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


import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;

import org.apache.usergrid.apm.service.charts.filter.TimeRangeFilter;
import org.apache.usergrid.apm.service.charts.service.CrashChartUtil;
import org.apache.usergrid.apm.service.charts.service.CrashRawCriteria;
import org.apache.usergrid.apm.model.CrashLogDetails;

public class CrashLogDBService {

	private static final Log log = LogFactory.getLog(CrashLogDBService.class);


	public void saveCrashLog(CrashLogDetails cl) {
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();       
			transaction = session.beginTransaction();
			session.saveOrUpdate(cl);
			log.debug("CrashLog record " + cl + " saved.");
			transaction.commit();
		} catch (HibernateException e) {
			log.error(e.getCause());
			transaction.rollback();
			throw new HibernateException("Cannot save CrashLog call record.", e);
		}

	}



	public void saveCrashLogs(List<CrashLogDetails> cls) {
		Session session = null;
		Transaction transaction = null;


		if (null == cls || cls.size() == 0) {
			log.info("Can not save empty list of crash logs");
			return;
		}     

		try {
			Iterator<CrashLogDetails> objIterator = cls.iterator();
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			while (objIterator.hasNext())
			{
				int numRecordsProcessed = 0;
				while (numRecordsProcessed < ServiceFactory.hibernateBatchSize && objIterator.hasNext())            
				{
					CrashLogDetails cl = objIterator.next();              
					session.saveOrUpdate(cl);
					log.debug("CrashLog record " + cl + " saved.");
					numRecordsProcessed++;
				}		
				session.flush();  
				session.clear();
			}
			transaction.commit();         
		}
		catch (HibernateException e) {
			transaction.rollback();
			throw new HibernateException("Cannot save crash logs .", e);
		} 


	}

	public CrashLogDetails getCrashLog(Long clId) {
		return null;
	}

	/**
	 * Use this to get all crash logs for a given app since given time
	 * @param instaOpsAppId
	 * @param since
	 */

	@SuppressWarnings("unchecked")
	public List<CrashLogDetails> getCrashLogs(Long instaOpsAppId, Date since) {
		log.info("getting crash logs for app  " + instaOpsAppId + " since " + since.toString());
		Long minuteValue = since.getTime()/1000/60;

		String queryString = "FROM CrashLogDetails m where m.appId = :appId and m.endMinute >=  :minuteValue "; 

		List<CrashLogDetails> crashLogs = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery(queryString);
			query.setParameter("appId", instaOpsAppId);
			query.setParameter("minuteValue", minuteValue);

			crashLogs = query.list();
			transaction.commit();
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Problem gettign crash logs for app " + instaOpsAppId, e);
		}
		return crashLogs;


	}
	
	@SuppressWarnings("unchecked")
	public List<CrashLogDetails> getCrashLogs(CrashRawCriteria cq) {
		/*Long instaOpsAppId = cq.getChartCriteria().getAppId();
		Date since = cq.getChartCriteria().getStartDate();
		log.info("getting crash logs for app  " + instaOpsAppId + " since " + since);
		Long minuteValue = since.getTime()/1000/60;

		String queryString = "FROM CrashLogDetails m where m.appId = :appId and m.endMinute >=  :minuteValue ";
		*/ 
		
		List<CrashLogDetails> crashLogs = null;
		Session session = null;
		Transaction transaction = null;
		List<Criterion> criteria = CrashChartUtil.getRawCrashCriteriaList(cq);
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(CrashLogDetails.class);	
			if ( criteria !=null) {
				Iterator<Criterion> it = criteria.iterator();
				while (it.hasNext()){
					crit.add(it.next());
				}
			}
			String endField = new TimeRangeFilter(cq.getChartCriteria()).getPropertyName();
			crit.addOrder(Order.desc(endField));

			if (cq.getStartRow() > 0)
				crit.setFirstResult(cq.getStartRow());
			if (cq.getRowCount() > 0)
				crit.setMaxResults( cq.getRowCount());
			log.info("getting  crash logs with " +  " criteria " + criteria.toString() +  " order  " + endField + " desc");
			crashLogs = crit.list();
			transaction.commit();
			for (CrashLogDetails clog : crashLogs)  {
				clog.setCrashLogUrl(AWSUtil.generatePresignedURLForCrashLog(clog.getFullAppName(), clog.getCrashFileName()));
				log.info("Pre-signed url is " + clog.getCrashLogUrl());
			}
			
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Problem getting crash logs for app " + cq.getChartCriteria().getAppId(), e);
		} 
		return crashLogs;
	}
	
	@SuppressWarnings("unchecked")
	public List<CrashLogDetails> getRawCrashLogsForAPointInTime(Long appId, Long endMinute, int maxResults) throws HibernateException {
		
		log.info("Getting raw crash logs for app id " + appId + " for endMinute " + endMinute);

		List<CrashLogDetails> metrics = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery("from CrashLogDetails as t where t.appId = :appId and t.endMinute= :minute");
			query.setParameter("appId", appId);
			query.setParameter("minute", endMinute);
			query.setMaxResults(maxResults);
			metrics = (List<CrashLogDetails>) query.list();
			transaction.commit();

		} catch (HibernateException e) {
			transaction.rollback();
			throw new HibernateException("Problem getting crash  log  for app id " + appId + " for endMinute " + endMinute , e);
		} 
		
		return metrics;
		
	}


}
