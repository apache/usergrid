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

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.transform.Transformers;

import org.apache.usergrid.apm.model.ClientLog;
import org.apache.usergrid.apm.service.charts.filter.TimeRangeFilter;
import org.apache.usergrid.apm.service.charts.service.AggregatedLogData;
import org.apache.usergrid.apm.service.charts.service.LogChartUtil;
import org.apache.usergrid.apm.service.charts.service.LogRawCriteria;
import org.apache.usergrid.apm.service.charts.service.SqlOrderGroupWhere;
import org.apache.usergrid.apm.model.CompactClientLog;
import org.apache.usergrid.apm.model.LogChartCriteria;

public class LogDBService {

	private static final Log log = LogFactory.getLog(LogDBService.class);

	public void saveLog (ClientLog logRecord) throws HibernateException  {
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();       
			transaction = session.beginTransaction();
			session.saveOrUpdate(logRecord);
			log.debug("ClientLog record " + logRecord + " saved.");
			transaction.commit();
		} catch (HibernateException e) {
			log.error(e.getCause());
			transaction.rollback();
			throw new HibernateException("Cannot save ClientLog call record.", e);
		}

	}

	public void saveLogs (List <ClientLog> logs) throws HibernateException{

		Session session = null;
		Transaction transaction = null;


		if (null == logs || logs.size() == 0) {
			log.info("Can not save empty list of logs");
			return;
		}     

		try {
			Iterator<ClientLog> objIterator = logs.iterator();
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			while (objIterator.hasNext())
			{
				int numRecordsProcessed = 0;
				while (numRecordsProcessed < ServiceFactory.hibernateBatchSize && objIterator.hasNext())            
				{
					ClientLog logRecord = objIterator.next();              
					session.saveOrUpdate(logRecord);
					log.debug("ClientLog record " + logRecord + " saved.");
					numRecordsProcessed++;
				}		
				session.flush();  
				session.clear();
			}
			transaction.commit();         
		}
		catch (HibernateException e) {
			e.printStackTrace();
			transaction.rollback();
			throw new HibernateException("Cannot save logs.", e);
		} 
	}

	public List<ClientLog> getLogsForApp(Long appId) throws HibernateException{
		log.info("This  should not be getting called. What's your usecase?");
		return null;
	}

	public enum OrderBy {ASCENDING, DECENDING};



	@SuppressWarnings("unchecked")
	public List<ClientLog> getLogs(List<Criterion> criteria, List<Order> orders) throws HibernateException {


		//    log.info("getting logs with startId=" + startId + ", numberOfRows=" + numberOfRows + ", " +
		//          "order=" + orders.toString() + " criteria " + criteria.toString());

		List<ClientLog> logRecList = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(ClientLog.class);

			if (criteria !=null) {
				Iterator<Criterion> it = criteria.iterator();
				while (it.hasNext()){
					crit.add(it.next());
				}
			}

			if (orders != null){
				Iterator<Order> it = orders.iterator();
				while (it.hasNext()) {
					crit.addOrder(it.next());
				}
			}
			logRecList = crit.list(); 
			transaction.commit();
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get client logs. ", e);
		} 

		return logRecList;
	}

	@SuppressWarnings("unchecked")
	public List<ClientLog> getLogs(Integer startId, int numberOfRows, List<Criterion> criteria, List<Order> orders) throws HibernateException {


		//		log.info("getting logs with startId=" + startId + ", numberOfRows=" + numberOfRows + ", " +
		//				"order=" + orders.toString() + " criteria " + criteria.toString());

		List<ClientLog> logRecList = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(ClientLog.class);
			if (startId != null) {
				crit.setFirstResult(startId.intValue());				
			}        

			if (numberOfRows != 0) 
				crit.setMaxResults(numberOfRows);

			if (criteria !=null) {
				Iterator<Criterion> it = criteria.iterator();
				while (it.hasNext()){
					crit.add(it.next());
				}
			}

			if (orders != null){
				Iterator<Order> it = orders.iterator();
				while (it.hasNext()) {
					crit.addOrder(it.next());
				}
			}
			logRecList = crit.list(); 
			transaction.commit();
		} catch (Exception e) {			
			e.printStackTrace();			
			transaction.rollback();
			throw new HibernateException("Cannot get client logs. ", e);
		} 

		return logRecList;
	}

	public void saveCompactLog (CompactClientLog logRecord) throws HibernateException  {
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();     
			session.saveOrUpdate(logRecord);
			transaction.commit();
			log.debug("Compact ClientLog record " + logRecord + " saved.");         
		} catch (HibernateException e) {
			e.printStackTrace();
			transaction.rollback();
			throw new HibernateException("Cannot save ClientLog call record.", e);
		} 
	}

	public void saveCompactLogs (List <CompactClientLog> logs) throws HibernateException{

		Session session = null;
		Transaction transaction = null;     

		if (null == logs || logs.size() == 0) {
			log.info("Can not save empty list of compact log metrics");
			return;
		}     

		try {
			Iterator<CompactClientLog> objIterator = logs.iterator();
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			while (objIterator.hasNext())
			{
				int numRecordsProcessed = 0;
				while (numRecordsProcessed < ServiceFactory.hibernateBatchSize && objIterator.hasNext())
				{
					CompactClientLog clog = objIterator.next();
					session.saveOrUpdate(clog);
					log.debug("Compacts logs record " + clog.toString() + " saved.");
					numRecordsProcessed++;               
				}            
				session.flush();  
				session.clear();
			}
			transaction.commit();         
		}
		catch (HibernateException e) {
			e.printStackTrace();
			transaction.rollback();
			throw new HibernateException("Cannot save compact log metrics.", e);
		} 
	}

	@SuppressWarnings("unchecked")
	public List<CompactClientLog> getCompactClientLogs(List<Criterion> criteria, List<Order> orders) throws HibernateException {


		//    log.info("getting logs with startId=" + startId + ", numberOfRows=" + numberOfRows + ", " +
		//          "order=" + orders.toString() + " criteria " + criteria.toString());

		List<CompactClientLog> logRecList = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(ClientLog.class);

			if (criteria !=null) {
				Iterator<Criterion> it = criteria.iterator();
				while (it.hasNext()){
					crit.add(it.next());
				}
			}

			if (orders != null){
				Iterator<Order> it = orders.iterator();
				while (it.hasNext()) {
					crit.addOrder(it.next());
				}
			}
			logRecList = crit.list(); 
			transaction.commit();
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get client logs. ", e);
		} 

		return logRecList;
	}

	@SuppressWarnings("unchecked")
	public List<CompactClientLog> getCompactClientLogUsingNativeSqlQuery(LogChartCriteria cq) {
		SqlOrderGroupWhere og = LogChartUtil.getOrdersAndGroupings(cq);

		String query = "SELECT * FROM COMPACT_CLIENT_LOG where " + og.whereClause +
				" group by " + og.groupBy + " order by " + og.orderBy;

		Session session = null;
		Transaction transaction = null;
		List<CompactClientLog> metrics = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			SQLQuery sqlquery = session.createSQLQuery(query).addEntity(CompactClientLog.class);
			metrics =  sqlquery.list();
			transaction.commit();
		} catch (HibernateException e) {
			e.printStackTrace();
			throw new HibernateException("Cannot get compact client logs. ", e);
		} 
		return metrics;

	}

	@SuppressWarnings("unchecked")
	public List<CompactClientLog> getCompactClientLogsForApp(Long appId) throws HibernateException
	{ 

		log.info("Getting all compact logs list");


		List<CompactClientLog> smBeans = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery("from CompactClientLog as t where t.appId = :appId");
			query.setParameter("appId", appId);
			smBeans = (List<CompactClientLog>) query.list();
			transaction.commit();

		} catch (HibernateException e) {
			throw new HibernateException("Cannot get compact logs ", e);
		} 

		return smBeans;
	}

	public AggregatedLogData getAggregatedLogData (LogChartCriteria chartCriteria) throws HibernateException {
		return null;
	}

	@SuppressWarnings("unchecked")
	public List<ClientLog> getRawLogData(LogRawCriteria logRawCriteria) {

		log.info ("Getting raw logs  ");

		List<ClientLog> logs = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(ClientLog.class);  

			List<Criterion> criteria = LogChartUtil.getRawLogCriteriaList(logRawCriteria);

			if ( criteria !=null) {
				Iterator<Criterion> it = criteria.iterator();
				while (it.hasNext()){
					crit.add(it.next());
				}
			}
			String endField = new TimeRangeFilter(logRawCriteria.getChartCriteria()).getPropertyName();
			crit.addOrder(Order.desc(endField));

			if (logRawCriteria.getStartRow() > 0)
				crit.setFirstResult(logRawCriteria.getStartRow());
			if (logRawCriteria.getRowCount() > 0)
				crit.setMaxResults(logRawCriteria.getRowCount());
			log.info("getting  logs with " +  " criteria " + criteria.toString() +  " order  endTime desc");

			logs = (List<ClientLog>) crit.list();
			transaction.commit();
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get logs from raw Client Logs ", e);
		} 

		return logs;


	}   

	public int getRawLogDataCount(LogRawCriteria logRawCriteria) {

		//this query is very expensive for various reasons. It does not make sense to do index
		//on logMessage. So we are going to return 1000 for now.
		log.warn("Always return 1000 for raw log data count for a given raw log criteria until we find more efficient storage and counter solution");
		return 1000;
		/*  SqlOrderGroupWhere ogw = LogChartUtil.getSqlStuff(logRawCriteria);
      String query = "SELECT count(distinct logMessage) as count FROM CLIENT_LOG "+
      "WHERE " + ogw.whereClause ; 
      log.info ("going to execute query for raw log data count " + query);

      Session session = null;
      Transaction transaction = null;
      try {
         session = ServiceFactory.getAnalyticsHibernateSession();
         transaction = session.beginTransaction();
         SQLQuery sqlQuery = session.createSQLQuery(query);
         BigInteger count = (BigInteger) sqlQuery.uniqueResult();
         log.info ("row count is " + count);
         return count.intValue();

      } catch (HibernateException e) {
         throw new HibernateException("Cannot get total cound of raw logs . ", e);
      } finally {
         transaction.commit();
      }
		 */   
	}


	/**
	 * Mostly for test purpose
	 * @return
	 */
	public int getCompactLogRowCount() {
		Session s = ServiceFactory.getAnalyticsHibernateSession();
		Transaction t = s.beginTransaction();
		Number count = (Number)s.createCriteria(CompactClientLog.class).    
				setProjection(Projections.rowCount()).uniqueResult();
		t.commit();
		log.info("available row count is = " + (count!=null?count.intValue():count));
		return count != null ? count.intValue() : 0;
	}


	@SuppressWarnings("unchecked")
	public List<CompactClientLog> getNewCompactClientLogUsingNativeSqlQuery(LogChartCriteria cq) {
		SqlOrderGroupWhere og = LogChartUtil.getOrdersAndGroupings(cq);

		//String query = "SELECT * FROM COMPACT_CLIENT_LOG where " + og.whereClause +
		//" group by " + og.groupBy + " order by " + og.orderBy;
		String query = "SELECT sum(assertCount) as assertCount, sum(debugCount) as debugCount," +
				" sum(errorAndAboveCount) as errorAndAboveCount, sum(errorCount) as errorCount, sum(infoCount) as infoCount,"+ 
				"sum(verboseCount) as verboseCount, sum(warnCount) as warnCount, sum(crashCount) as crashCount,"+
				" endMinute, endHour, endDay, endWeek,endMonth, devicePlatform,appConfigType, applicationVersion,deviceModel,deviceOperatingSystem,networkCarrier,networkType " +
				" FROM COMPACT_CLIENT_LOG where " +
				og.whereClause + " group by " + og.groupBy + " order by " + og.orderBy;


		Session session = null;
		Transaction transaction = null;
		List<CompactClientLog> returnList = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			SQLQuery sqlquery = session.createSQLQuery(query)
					.addScalar("assertCount", Hibernate.LONG)
					.addScalar("debugCount", Hibernate.LONG)         
					.addScalar("errorAndAboveCount", Hibernate.LONG)
					.addScalar("errorCount", Hibernate.LONG)
					.addScalar("infoCount", Hibernate.LONG)
					.addScalar("verboseCount", Hibernate.LONG)
					.addScalar("warnCount", Hibernate.LONG)
					.addScalar("crashCount", Hibernate.LONG)
					.addScalar("endMinute", Hibernate.LONG)
					.addScalar("endHour", Hibernate.LONG)
					.addScalar("endDay", Hibernate.LONG)
					.addScalar("endWeek", Hibernate.LONG)
					.addScalar("endMonth", Hibernate.LONG)
					.addScalar("devicePlatform")
					.addScalar("deviceOperatingSystem")
					.addScalar("appConfigType")//Hibernate.custom(com.ideawheel.portal.model.String.class))
					.addScalar("applicationVersion")
					.addScalar("deviceModel")
					.addScalar("networkCarrier")
					.addScalar("networkType");

			sqlquery.setResultTransformer(Transformers.aliasToBean(CompactClientLog.class));
			returnList = (List<CompactClientLog>) sqlquery.list();
			transaction.commit();


		} catch (HibernateException e) {
			e.printStackTrace();
			transaction.rollback();
			throw new HibernateException("Cannot get compact client logs. ", e);
		} 
		return  returnList;

	}
	//start crash stuff

	public int getCrashCount (LogRawCriteria logRawCriteria) {

		log.info ("Getting crash counts for app id  " + logRawCriteria.getChartCriteria().getAppId());
		//log.warn("Always return 1000 for crash data count for a given raw log criteria until we find " +
		//	"more efficient storage and counter solution");
		//return 1000;

		Session session = null;
		Transaction transaction = null;
		int count = 0;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(ClientLog.class);  

			List<Criterion> criteria = LogChartUtil.getRawLogCriteriaList(logRawCriteria);


			if ( criteria !=null) {
				Iterator<Criterion> it = criteria.iterator();
				while (it.hasNext()){
					crit.add(it.next());
				}
			}

			crit.setProjection(Projections.rowCount());

			log.info("getting crash count with " +  " criteria " + criteria.toString());

			count = ((Number) crit.uniqueResult()).intValue();
			transaction.commit();

		} catch (HibernateException e) {
			transaction.rollback();
			e.printStackTrace();
			throw new HibernateException("Cannot get crash count. ", e);
		} 
		return count;
	}

	@SuppressWarnings("unchecked")
	public List<ClientLog> getCrashLogs (LogRawCriteria logRawCriteria)  {
		log.info ("Getting crash logs for app id  " + logRawCriteria.getChartCriteria().getAppId());

		List<ClientLog> logs = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(ClientLog.class);  

			List<Criterion> criteria = LogChartUtil.getRawLogCriteriaList(logRawCriteria);

			if ( criteria !=null) {
				Iterator<Criterion> it = criteria.iterator();
				while (it.hasNext()){
					crit.add(it.next());
				}
			}			

			String endField = new TimeRangeFilter(logRawCriteria.getChartCriteria()).getPropertyName();
			crit.addOrder(Order.desc(endField));

			if (logRawCriteria.getStartRow() > 0)
				crit.setFirstResult(logRawCriteria.getStartRow());
			if (logRawCriteria.getRowCount() > 0)
				crit.setMaxResults(logRawCriteria.getRowCount());
			log.info("getting crash logs with " +  " criteria " + criteria.toString() +  " order " +  endField + " desc");

			logs = (List<ClientLog>) crit.list();
			transaction.commit();
			for (ClientLog clog : logs)  {
				//logMessage in ClientLog has CrashLog file name on S3
				clog.setCrashLogUrl(AWSUtil.generatePresignedURLForCrashLog(logRawCriteria.getChartCriteria().getAppId().toString(), clog.getLogMessage()));
				log.info("Pre-singed url is " + clog.getCrashLogUrl());
			}
			return logs;

		} catch (HibernateException e) {
			transaction.rollback();
			e.printStackTrace();
			throw new HibernateException("Cannot get crash logs. ", e);
		} 

	}

	@SuppressWarnings("unchecked")
	public List<ClientLog> getRawClientLogForAPointInTime(Long appId, Long endMinute, int maxResults) throws HibernateException {

		log.info("Getting raw client log for app id " + appId + " for endMinute " + endMinute);

		List<ClientLog> metrics = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery("from ClientLog as t where t.appId = :appId and t.endMinute= :minute");
			query.setParameter("appId", appId);
			query.setParameter("minute", endMinute);
			query.setMaxResults(maxResults);
			metrics = (List<ClientLog>) query.list();
			transaction.commit();

		} catch (HibernateException e) {
			transaction.rollback();
			e.printStackTrace();
			throw new HibernateException("Problem getting raw log  for app id " + appId + " for endMinute " + endMinute , e);
		} 
		return metrics;

	}
}
