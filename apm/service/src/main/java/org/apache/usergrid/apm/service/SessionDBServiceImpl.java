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

import java.util.Collection;
import java.util.Date;
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
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;

import org.apache.usergrid.apm.model.ClientSessionMetrics;
import org.apache.usergrid.apm.service.charts.service.AggregatedSessionData;
import org.apache.usergrid.apm.service.charts.service.AttributeValueChartData;
import org.apache.usergrid.apm.service.charts.service.SessionMetricsChartUtil;
import org.apache.usergrid.apm.service.charts.service.SqlOrderGroupWhere;
import org.apache.usergrid.apm.model.CompactSessionMetrics;
import org.apache.usergrid.apm.model.SessionChartCriteria;
import org.apache.usergrid.apm.model.SummarySessionMetrics;

public class SessionDBServiceImpl implements SessionDBService
{
	private static final Log log = LogFactory.getLog(SessionDBServiceImpl.class);


	@Override
	public void saveSessionMetrics(ClientSessionMetrics sessionDatum) throws HibernateException
	{
		//log.info ("saving raw session metrics");
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();

			// Calculate end Minute / hour / Day week / etc
			//Note in this case it makes sense to use start time instead of end time. For network metrics we have used end time

			Date date = sessionDatum.getTimeStamp();
			if (date != null)
			{
				sessionDatum.setEndMinute(date.getTime() / 1000/60);
				sessionDatum.setEndHour(date.getTime() / 1000/ 60 / 60);
				sessionDatum.setEndDay(date.getTime() / 1000/ 60 / 60 / 24);
				sessionDatum.setEndWeek(date.getTime() / 1000/ 60 / 60 / 24 / 7);
				sessionDatum.setEndMonth(date.getTime() / 1000/ 60 / 60 / 24 / 7 / 12);
			}


			session.saveOrUpdate(sessionDatum);
			transaction.commit();
			log.debug("raw session summary record " + sessionDatum.toString() + " saved.");
		} catch (HibernateException e) {
			e.printStackTrace();
			transaction.rollback();
			throw new HibernateException("Cannot save client session metrics.", e);
		} 

	}

	@Override
	public void saveSessionMetricsInBatch(List<ClientSessionMetrics> objs) throws HibernateException
	{
		Session session = null;
		Transaction transaction = null;


		if (null == objs || objs.size() == 0) {
			log.info("Can not save empty list of Raw session metrics");
			return;
		}     

		try {
			Iterator<ClientSessionMetrics> objIterator = objs.iterator();
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			while (objIterator.hasNext())
			{
				int numRecordsProcessed = 0;
				while (numRecordsProcessed < ServiceFactory.hibernateBatchSize && objIterator.hasNext())
				{
					ClientSessionMetrics sessionDatum = objIterator.next();
					Date date = sessionDatum.getTimeStamp();
					if (date != null)
					{
						sessionDatum.setEndMinute(date.getTime() / 1000/60);
						sessionDatum.setEndHour(date.getTime() / 1000/ 60 / 60);
						sessionDatum.setEndDay(date.getTime() / 1000/ 60 / 60 / 24);
						sessionDatum.setEndWeek(date.getTime() / 1000/ 60 / 60 / 24 / 7);
						sessionDatum.setEndMonth(date.getTime() / 1000/ 60 / 60 / 24 / 7 / 12);
					}
					session.saveOrUpdate(sessionDatum);
					log.debug("Raw session Metrics record " + sessionDatum.toString() + " saved.");
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
			throw new HibernateException("Cannot save raw session metrics.", e);
		} 
	}




	@Override
	public List<ClientSessionMetrics> getAllSessionMetrics() throws HibernateException
	{
		// TODO Auto-generated method stub
		log.info ("You should not be using this. What's your usecase?");
		return null;
	}

	@Override
	public List<ClientSessionMetrics> getSessionList(Integer startId, int numberOfRows, List<Criterion> criteria, List<Order> orders) throws HibernateException
	{
		log.info("getting client session metrics list with startId=" + startId + ", numberOfRows=" + numberOfRows + ", " +
				"order=" + orders.toString() + " criteria " + criteria.toString());

		List<ClientSessionMetrics> wsRecList = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(ClientSessionMetrics.class);
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
			wsRecList = crit.list(); 
			transaction.commit();
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get session metrics. ", e);
		} 

		return wsRecList;

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ClientSessionMetrics> getSessionMetricsForApp(Long appId) throws HibernateException
	{ log.info("Getting all network metrics list");


	List<ClientSessionMetrics> smBeans = null;
	Session session = null;
	Transaction transaction = null;
	try {
		session = ServiceFactory.getAnalyticsHibernateSession();
		transaction = session.beginTransaction();
		Query query = session.createQuery("from ClientSesionMetrics as t where t.appId = :appId");
		query.setParameter("appId", appId);
		smBeans = (List<ClientSessionMetrics>) query.list();
		transaction.commit();
	} catch (HibernateException e) {
		e.printStackTrace();
		transaction.rollback();
		throw new HibernateException("Cannot get session metrics. ", e);
	} 

	return smBeans;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CompactSessionMetrics> getCompactSessionMetrics(List<Criterion> criteria, ProjectionList projections, List<Order> orders) throws HibernateException
	{
		log.info ("Getting data from compact session metrics table " + criteria.toString());
		List<CompactSessionMetrics> hcsms = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(CompactSessionMetrics.class);        
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

			crit.setProjection(projections);
			log.info("getting session metrics with " +  " criteria " + criteria.toString() + " groupby " + projections.toString() + " orders " + orders.toString());
			hcsms = crit.list(); 
			transaction.commit();
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get network metrics. ", e);
		} 

		return null;
	}

	@Override
	public List<CompactSessionMetrics> getComactSessionMetrics(List<Criterion> criteria, List<Order> orders) throws HibernateException
	{
		log.info ("Getting data from compact session metrics " + criteria.toString());

		List<CompactSessionMetrics> metrics = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(CompactSessionMetrics.class);

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
			//log.info("getting metrics with " +  " criteria " + criteria.toString() +  " orders " + orders.toString());

			metrics = (List<CompactSessionMetrics>) crit.list();
			transaction.commit();
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get metrics from CompactSessionMetrics ", e);
		} 

		return metrics;
	}

	@Override
	public void saveCompactSessionMetrics(CompactSessionMetrics obj) throws HibernateException
	{
		//log.info ("saving session metrics");
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			session.saveOrUpdate(obj);
			transaction.commit();
			log.debug("compact session record " + obj.toString() + " saved.");
		} catch (HibernateException e) {
			e.printStackTrace();
			transaction.rollback();
			throw new HibernateException("Cannot save compact session metrics.", e);
		} 

	}


	@Override
	public void saveCompactSessionMetrics(List<CompactSessionMetrics> objs) throws HibernateException
	{
		Session session = null;
		Transaction transaction = null;     

		if (null == objs || objs.size() == 0) {
			log.info("Can not save empty list of compact session metrics");
			return;
		}     

		try {
			Iterator<CompactSessionMetrics> objIterator = objs.iterator();
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			while (objIterator.hasNext())
			{
				int numRecordsProcessed = 0;
				while (numRecordsProcessed < ServiceFactory.hibernateBatchSize && objIterator.hasNext())
				{
					CompactSessionMetrics wsBean = objIterator.next();
					session.saveOrUpdate(wsBean);
					log.debug("Compacts session Metrics record " + wsBean.toString() + " saved.");
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
			throw new HibernateException("Cannot save compact session metrics.", e);
		} 
	}


	@Override
	public void saveSummarySessionMetrics(SummarySessionMetrics summarySession) throws HibernateException
	{
		log.info ("saving summary session metrics");
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			session.saveOrUpdate(summarySession);
			transaction.commit();
			log.debug("summary session record " + summarySession.toString() + " saved.");
		} catch (HibernateException e) {
			e.printStackTrace();
			transaction.rollback();
			throw new HibernateException("Cannot save symmary session metrics.", e);
		} 

	}

	@Override
	public void saveSummarySessionMetricsInBatch(List<SummarySessionMetrics> objs) throws HibernateException
	{
		Transaction transaction = null;
		Session session = null;

		if (null == objs || objs.size() == 0) {
			log.info("Can not save empty list of summary session metrics");
			return;
		}     

		try {
			Iterator<SummarySessionMetrics> objIterator = objs.iterator();
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			while (objIterator.hasNext())
			{
				int numRecordsProcessed = 0;
				while (numRecordsProcessed < ServiceFactory.hibernateBatchSize && objIterator.hasNext())
				{
					SummarySessionMetrics wsBean = objIterator.next();
					session.saveOrUpdate(wsBean);
					log.debug("summary session Metrics record " + wsBean.toString() + " saved.");
					numRecordsProcessed++;               
				}            
				session.flush();  
				session.clear();
			}
			transaction.commit();         
		}
		catch (HibernateException e) {
			transaction.rollback();
			throw new HibernateException("Cannot save summary session metrics.", e);
		} 
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SummarySessionMetrics> getSummarySessionMetrics(List<Criterion> criteria, List<Order> orders) throws HibernateException
	{
		log.info ("Getting data from summary session metrics " + criteria.toString());

		List<SummarySessionMetrics> metrics = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(SummarySessionMetrics.class);

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
			log.info("getting metrics with " +  " criteria " + criteria.toString() +  " orders " + orders.toString());

			metrics = (List<SummarySessionMetrics>) crit.list();
			transaction.commit();
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get metrics from Summary SessionMetrics ", e);
		} 

		return metrics;
	}

	@Override
	public Long getCompactSessionMetricsRowCount() {
		String query = "SELECT MAX(id) from COMPACT_SESSION_METRICS";
		Transaction transaction = null;
		Session session = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			SQLQuery sqlquery = session.createSQLQuery(query);

			Number number = (Number) sqlquery.uniqueResult();
			transaction.commit();
			if (number != null)
				return number.longValue();
			else 
				return 0L;
		} catch (HibernateException e) {
			e.printStackTrace();
			log.error("Problem getting Compact Session Metrics Count");
			throw new HibernateException("Cannot get compact session metrics. ", e);
		} 

	}

	@Override
	public Long getSummarySessionMetricsRowCountByApp(Long appId) {
		String query = "SELECT COUNT(*) from SUMMARY_SESSION_METRICS as t where t.appId = :appId";
		Transaction transaction = null;
		Session session = null;
		Long totalCount = 0L;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			SQLQuery sqlquery = session.createSQLQuery(query);
			sqlquery.setParameter("appId", appId);
			totalCount = ((Number) sqlquery.uniqueResult()).longValue();
			transaction.commit();
		} catch (HibernateException e) {
			e.printStackTrace();
			transaction.rollback();
			throw new HibernateException("Cannot get client session metrics. ", e);
		} 

		return totalCount;

	}

	@Override
	public Long getClientSessionMetricsRowCount() {

		String query = "SELECT MAX(id) from CLIENT_SESSION_METRICS";
		Transaction transaction = null;
		Session session = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			SQLQuery sqlquery = session.createSQLQuery(query);
			Number number = (Number) sqlquery.uniqueResult();
			transaction.commit();
			if (number != null)
				return number.longValue();
			else 
				return 0L;
		} catch (HibernateException e) {
			e.printStackTrace();
			log.error("Problem getting Client Session Metrics Count");
			transaction.rollback();
			throw new HibernateException("Cannot get client session metrics. ", e);
		} 
		/*
		Session s = ServiceFactory.getAnalyticsHibernateSession();
		Transaction t = s.beginTransaction();
		Number count = (Number)s.createCriteria(ClientSessionMetrics.class).    
		setProjection(Projections.rowCount()).uniqueResult();
		t.commit();
		log.info("available row count for Client Session Metrics is = " + (count!=null?count.intValue():count));
		return count != null ? count.intValue() : 0;
		 */
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CompactSessionMetrics> getCompactSessionMetricsUsingNativeSqlQuery (SessionChartCriteria cq) {
		SqlOrderGroupWhere og = SessionMetricsChartUtil.getOrdersAndGroupings(cq);

		String query = "SELECT * FROM COMPACT_SESSION_METRICS where " + og.whereClause +
				" group by " + og.groupBy + " order by " + og.orderBy;

		log.info("Getting compact session metrics with query " + query);

		Session session = null;
		Transaction transaction = null;
		List<CompactSessionMetrics> metrics = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			SQLQuery sqlquery = session.createSQLQuery(query).addEntity(CompactSessionMetrics.class);
			metrics =  sqlquery.list();
			transaction.commit();			
		} catch (HibernateException e) {
			e.printStackTrace();
			transaction.rollback();
			throw new HibernateException("Cannot get compact session metrics. ", e);
		} finally {
			
		}     
		return metrics;

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CompactSessionMetrics> getNewCompactSessionMetricsUsingNativeSqlQuery (SessionChartCriteria cq) {
		SqlOrderGroupWhere og = SessionMetricsChartUtil.getOrdersAndGroupings(cq);

		//String query = "SELECT * FROM COMPACT_SESSION_METRICS where " + og.whereClause +
		//" group by " + og.groupBy + " order by " + og.orderBy;

		String query = "SELECT sum(sessionCount) as sessionCount, sum(numUniqueUsers) as numUniqueUsers," +
				" sum(sumSessionLength) as sumSessionLength, " +
				" endMinute, endHour, endDay,endWeek,endMonth, devicePlatform,CONFIG_TYPE as appConfigType, applicationVersion,deviceModel,deviceOperatingSystem,networkCarrier,networkType " +
				" FROM COMPACT_SESSION_METRICS where " +
				og.whereClause + " group by " + og.groupBy + " order by " + og.orderBy;

		log.info("Getting compact session metrics with query " + query);

		Session session = null;
		Transaction transaction = null;
		List<CompactSessionMetrics> returnList = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			SQLQuery sqlquery = session.createSQLQuery(query)
					.addScalar("sessionCount", Hibernate.LONG)
					.addScalar("numUniqueUsers", Hibernate.LONG)         
					.addScalar("sumSessionLength", Hibernate.LONG)        
					.addScalar("endMinute", Hibernate.LONG)
					.addScalar("endHour", Hibernate.LONG)
					.addScalar("endDay", Hibernate.LONG)
					.addScalar("endWeek", Hibernate.LONG)
					.addScalar("endMonth", Hibernate.LONG)
					.addScalar("devicePlatform")
					.addScalar("appConfigType")//Hibernate.custom(com.ideawheel.portal.model.String.class))        
					.addScalar("applicationVersion")
					.addScalar("deviceModel")
					.addScalar("deviceOperatingSystem")
					.addScalar("networkCarrier")
					.addScalar("networkType");

			sqlquery.setResultTransformer(Transformers.aliasToBean(CompactSessionMetrics.class));
			returnList = (List<CompactSessionMetrics>) sqlquery.list();
			transaction.commit();			

		} catch (HibernateException e) {
			e.printStackTrace();
			transaction.rollback();
			throw new HibernateException("Cannot get compact session metrics. ", e);
		} 
		return returnList;

	}


	@SuppressWarnings("unchecked")
	public List<CompactSessionMetrics> getCompactSessionMetricsForApp(Long appId) throws HibernateException
	{ 

		log.info("Getting all session metrics list");


		List<CompactSessionMetrics> smBeans = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery("from CompactSessionMetrics as t where t.appId = :appId");
			query.setParameter("appId", appId);
			smBeans = (List<CompactSessionMetrics>) query.list();
			transaction.commit();
		} catch (HibernateException e) {
			transaction.rollback();
			throw new HibernateException("Cannot get compact session metrics. ", e);
		}

		return smBeans;
	}

	@Override
	@SuppressWarnings("unchecked")
	public AggregatedSessionData getAggreateSessionData(SessionChartCriteria chartCriteria) throws HibernateException
	{
		String query = "SELECT count(*)  as totalSessions, count(distinct deviceId) as totalUniqueUsers," +
				" avg(sessionLength) as avgSessionLength FROM SUMMARY_SESSION_METRICS " + 
				SessionMetricsChartUtil.getWhereClauseForAggregateSessionData(chartCriteria);

		log.info("Aggregated session data query is " + query);
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			SQLQuery sqlquery = session.createSQLQuery(query);
			sqlquery.setResultTransformer(Transformers.aliasToBean(AggregatedSessionData.class));
			AggregatedSessionData data = (AggregatedSessionData) sqlquery.uniqueResult();
			transaction.commit();
			log.info ("aggregated session data " + data.toString());

			/*//List<Long> results = (List<Long>) sqlquery.list();
         Object[] rawResult = (Object[]) sqlquery.uniqueResult();
         log.info("there should be 3 columns returned " + rawResult.length);
         AggregatedSessionData data = new AggregatedSessionData();
         data.setTotalSessions( ((BigInteger) rawResult[0]).longValue());
         data.setTotalUniqueUsers(((BigInteger)rawResult[1]).longValue());
         data.setAvgSessionLength(((BigDecimal) rawResult[2]).longValue());*/         
			return data;

		} catch (HibernateException e) {
			e.printStackTrace();
			transaction.rollback();
			throw new HibernateException("Cannot get aggregated data from summary session metrics. ", e);
		} 

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SummarySessionMetrics> getSummarySessionsForDevices(Long applicationId, List<String> deviceIds, Date sessionExpiryTime) throws HibernateException
	{

		log.info("getting summary session metrics with for " + deviceIds.toString() +  " sessionExpiryTime " + sessionExpiryTime.toString());
		if(deviceIds == null || deviceIds.size() == 0 || sessionExpiryTime == null) {
			log.info("List of deviceIds is null or sessionExpiryTime is null. Going to return an empty summary session metrics");
			return null;
		}

		//		String queryString = "FROM SummarySessionMetrics m where m.appId = :appId and m.deviceId in (:devices) " +
		//		"and m.sessionEndTime >= :sessionExpTime "; 

		String queryString = "FROM SummarySessionMetrics m where m.appId = :appId and m.deviceId in (:devices) "; 



		List<SummarySessionMetrics> metrics = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery(queryString);
			query.setParameter("appId", applicationId);
			query.setParameterList("devices", deviceIds);

			//This doesn't make any sense - need to eliminate because session expiry time is different for each session
			//query.setParameter("sessionExpTime", sessionExpiryTime);

			/*Criteria crit = session.createCriteria(SummarySessionMetrics.class);
         session.createQuery(arg0)
         crit.add(Restrictions.eq("appId", applicationId));
         crit.add(Restrictions.in("deviceId", deviceIds));
         crit.add(Restrictions.le("sessionEndTime", sessionExpiryTime));  
         crit.setProjection(Projections.groupProperty("deviceId"));
         crit.setProjection(Projections.property("sessionStartTime"));
         crit.addOrder(Order.desc("sessionEndTime"));

         log.info("getting summary session metrics with for " + deviceIds.toString() +  " sessionExpiryTime " + sessionExpiryTime.toString());
         metrics = (List<SummarySessionMetrics>) crit.list();*/
			metrics = query.list();

			transaction.commit();
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get metrics from Summary SessionMetrics ", e);
		}
		return metrics;
	}

	@SuppressWarnings("unchecked")
	public List<String> getDistinctValuesFromSummarySessionMetrics (String column, Date since) throws HibernateException {
		//String query = "SELECT DISTINCT " + column + "FROM SUMMARY_SESSION_METRICS where " +
		log.info("Getting distinct values for " + column + " since " + since);
		List<String> results = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(SummarySessionMetrics.class);
			crit.add (Restrictions.gt("sessionEndTime", since));
			crit.setProjection( Projections.distinct( Projections.property(column) ) );
			results = (List<String>) crit.list();
			transaction.commit();
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get distict values from SummarySessionMetrics ", e);
		} 
		return results;

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SummarySessionMetrics> getSummarySessionsBySessionId(
			Long appId, Collection<String> sessionIds) throws HibernateException {
		// TODO Auto-generated method stub

		log.info("getting summary sessions matching  " + sessionIds.toString());

		String queryString = "FROM SummarySessionMetrics m where m.appId = :appId and m.sessionId in (:sessions) "; 

		List<SummarySessionMetrics> metrics = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery(queryString);
			query.setParameter("appId", appId);
			query.setParameterList("sessions", sessionIds);

			metrics = query.list();
			transaction.commit();
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get metrics from Summary SessionMetrics ", e);
		}
		return metrics;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CompactSessionMetrics> getSessionChartDataForHourlyGranularity (SessionChartCriteria cq) {
		SqlOrderGroupWhere og = SessionMetricsChartUtil.getOrdersAndGroupingsForSummarySessionMetrics(cq);	

		/*query will be ultimately of following form. We are using endMinute on where clause because that's where index exists.

		SELECT count(*) as numSession, avg(sessionLength) as sesLen, deviceModel,endHour FROM instaops_analytics.SUMMARY_SESSION_METRICS 
		where appId=3 and endMinute > 22516200 and endMinute < 22520040 
		group by endHour,deviceModel order by endHour,deviceModel asc;*/

		String query = "SELECT count(*) as sessionCount, avg(sessionLength) as sumSessionLength,"+	
				"endHour,deviceModel,devicePlatform,deviceOperatingSystem, CONFIG_TYPE as appConfigType, " +
				"applicationVersion,networkCarrier,networkType " +
				" FROM SUMMARY_SESSION_METRICS where " +
				og.whereClause + " group by " + og.groupBy + " order by " + og.orderBy;

		log.info("Getting summary session metrics with query " + query);

		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			SQLQuery sqlquery = session.createSQLQuery(query)
					.addScalar("sessionCount", Hibernate.LONG)			        
					.addScalar("sumSessionLength", Hibernate.LONG)			
					.addScalar("endHour", Hibernate.LONG)
					.addScalar("deviceModel")
					.addScalar("devicePlatform")
					.addScalar("deviceOperatingSystem")
					.addScalar("appConfigType")//Hibernate.custom(com.ideawheel.portal.model.String.class))        
					.addScalar("applicationVersion")			
					.addScalar("networkCarrier")
					.addScalar("networkType");
			sqlquery.setResultTransformer(Transformers.aliasToBean(CompactSessionMetrics.class));
			List<CompactSessionMetrics> returnList = (List<CompactSessionMetrics>) sqlquery.list();
			transaction.commit();
			return returnList;

		} catch (HibernateException e) {
			e.printStackTrace();
			transaction.rollback();
			throw new HibernateException("Cannot get compact session metrics. ", e);
		} 

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<AttributeValueChartData> getCountFromSummarySessionMetrics( SessionChartCriteria cq) throws HibernateException {
		
		//Index on SummarySessionMetrics is on columns appId, endMinute
		String columnName = SessionMetricsChartUtil.getColumnName(cq) ;
		Long startMinuteValue = cq.getStartDate().getTime()/1000/60;
		Long endMinuteValue = cq.getEndDate().getTime()/1000/60;
		
		String query = "SELECT " + columnName + " as attribute , count(*)  as value FROM SUMMARY_SESSION_METRICS " +
			" where appId = " +  cq.getAppId() +  " and endMinute >= " + startMinuteValue + " and endMinute <= " + endMinuteValue + 
			" group by attribute order by value desc";
		
		log.info("Query for SummarySessionMetrics table is " + query);
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			SQLQuery sqlquery = session.createSQLQuery(query)
			.addScalar("attribute", Hibernate.STRING)
			.addScalar("value", Hibernate.LONG);
			sqlquery.setResultTransformer(Transformers.aliasToBean(AttributeValueChartData.class));			
			List<AttributeValueChartData> result = ( List<AttributeValueChartData>) sqlquery.list();
			transaction.commit();
			return result;

		} catch (HibernateException e) {
			e.printStackTrace();	
			transaction.rollback();
			throw new HibernateException("Cannot get total count for column " + columnName, e);
		} 

	}


}
