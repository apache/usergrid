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

import org.apache.usergrid.apm.model.ClientNetworkMetrics;
import org.apache.usergrid.apm.service.charts.filter.TimeRangeFilter;
import org.apache.usergrid.apm.service.charts.service.AggregatedNetworkData;
import org.apache.usergrid.apm.service.charts.service.NetworkMetricsChartUtil;
import org.apache.usergrid.apm.service.charts.service.NetworkMetricsRawCriteria;
import org.apache.usergrid.apm.service.charts.service.SqlOrderGroupWhere;
import org.apache.usergrid.apm.model.CompactNetworkMetrics;
import org.apache.usergrid.apm.model.MetricsChartCriteria;


/**
 * Implementation of services for performing Database operations.
 *
 * @author prabhat jha 
 */
public class NetworkMetricsDBServiceImpl implements NetworkMetricsDBService {

	private static final Log log = LogFactory.getLog(NetworkMetricsDBServiceImpl.class);


	public static String METRICS_UNKNOWN_ATTRIBUTE_VALUE = "Unknown";


	@Override
	public void saveNetworkMetrics(ClientNetworkMetrics wsBean) throws HibernateException {
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();

			if (null == wsBean.getNetworkCarrier())
				wsBean.setNetworkCarrier(METRICS_UNKNOWN_ATTRIBUTE_VALUE);
			if (null == wsBean.getNetworkType())
				wsBean.setNetworkType(METRICS_UNKNOWN_ATTRIBUTE_VALUE);


			session.saveOrUpdate(wsBean);
			transaction.commit();
			log.debug("Client Network metrics record " + wsBean.toString() + " saved.");
		} catch (HibernateException e) {
			transaction.rollback();
			e.printStackTrace();
			throw new HibernateException("Cannot save Webservices call record.", e);
		} 
	}

	@Override
	public void saveCompactNetworkMetrics(CompactNetworkMetrics m) throws HibernateException {

		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();         

			session.saveOrUpdate(m);
			transaction.commit();
			log.debug("Compact Network Metrics " + m.toString() + " saved.");
		} catch (HibernateException e) {
			transaction.rollback();
			e.printStackTrace();
			throw new HibernateException("Cannot save Webservices call record.", e);
		} 
	}
	@SuppressWarnings("unchecked")
	@Override
	public List<ClientNetworkMetrics> getAllNetworkMetrics() throws HibernateException {
		log.info("Getting all network metrics list");

		List<ClientNetworkMetrics> wsRecList = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery("from ClientNetworkMetrics as t");
			wsRecList = (List<ClientNetworkMetrics>) query.list();
			transaction.commit();

		} catch (HibernateException e) {
			transaction.rollback();
			e.printStackTrace();
			throw new HibernateException("Cannot get network metrics. ", e);
		} 

		return wsRecList;
	}

	@SuppressWarnings("unchecked")
	public List<ClientNetworkMetrics> getNetworkMetricsForApp(Long appId) throws HibernateException {
		log.info("Getting all network metrics list");


		List<ClientNetworkMetrics> wsRecList = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery("from ClientNetworkMetrics as t where t.appId = :appId");
			query.setParameter("appId", appId);
			wsRecList = (List<ClientNetworkMetrics>) query.list();
			transaction.commit();

		} catch (HibernateException e) {
			transaction.rollback();
			e.printStackTrace();
			throw new HibernateException("Cannot get network metrics. ", e);
		}
		return wsRecList;

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ClientNetworkMetrics> getNetworkMetricsList(Integer startId, int numberOfRows, List<Criterion> criteria, List<Order> orders) throws HibernateException {

		log.info("getting webservices list with startId=" + startId + ", numberOfRows=" + numberOfRows + ", " +
				"order=" + orders.toString() + " criteria " + criteria.toString());

		List<ClientNetworkMetrics> wsRecList = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(ClientNetworkMetrics.class);
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
			throw new HibernateException("Cannot get network metrics. ", e);
		} 

		return wsRecList;
	}

	@SuppressWarnings("unchecked")
	public List<String> getUniqueUrls(List<Criterion> criteria) throws HibernateException {
		log.info ("Getting unique urls for " + criteria.toString());

		List<String> uniqueUrls = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(ClientNetworkMetrics.class);

			if (criteria !=null) {
				Iterator<Criterion> it = criteria.iterator();
				while (it.hasNext()){
					crit.add(it.next());
				}
			}
			crit.setProjection(Projections.distinct(Projections.property("regexUrl")));
			uniqueUrls = (List<String>) crit.list();
			transaction.commit();
		} catch (Exception e) {			
			e.printStackTrace();			
			transaction.rollback();
			throw new HibernateException("Cannot get unique urls ", e);
		} 
		return uniqueUrls;


	}



	@Override
	public int getRowCount() {
		Session s = ServiceFactory.getAnalyticsHibernateSession();
		Transaction t = s.beginTransaction();
		Number count = (Number)s.createCriteria(ClientNetworkMetrics.class).		
		setProjection(Projections.rowCount()).uniqueResult();
		t.commit();
		log.info("available row count is = " + (count!=null?count.intValue():count));
		return count != null ? count.intValue() : 0;
	}



	@Override
	public void saveNetworkMetricsInBatch(List<ClientNetworkMetrics> nms) throws HibernateException {
		Session session = null;
		Transaction transaction = null;

		if (null == nms || nms.size() == 0) {
			log.info("Can not save empty list of client network metrics");
			return;
		}     

		try {
			Iterator<ClientNetworkMetrics> objIterator = nms.iterator();
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			while (objIterator.hasNext())
			{
				int numRecordsProcessed = 0;
				while (numRecordsProcessed < ServiceFactory.hibernateBatchSize && objIterator.hasNext())            
				{            
					ClientNetworkMetrics bean = objIterator.next();
					session.save(bean);
					log.debug("Network metrics " + bean + " saved.");
				}     
				session.flush();  
				session.clear();
			}
			transaction.commit();  
		}
		catch (HibernateException e) {
			transaction.rollback();
			e.printStackTrace();
			throw new HibernateException("Cannot save network  metrics.", e);
		} 
	}

	@SuppressWarnings("unchecked")
	public List<CompactNetworkMetrics> getCompactNetworkMetrics(List<Criterion> criteria, List <Order> orders) throws HibernateException {
		log.info ("Getting data from hourly compact metrics " + criteria.toString());

		List<CompactNetworkMetrics> metrics = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(CompactNetworkMetrics.class);

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
			log.info("getting metrics with " +	" criteria " + criteria.toString() +  " orders " + orders.toString());

			metrics = (List<CompactNetworkMetrics>) crit.list();
			transaction.commit();
		} catch (Exception e) {			
			e.printStackTrace();			
			transaction.rollback();
			throw new HibernateException("Cannot get metrics from CompactNetworkMetrics ", e);
		} 

		return metrics;
	}

	

	public void saveCompactNetworkMetricsInBatch(List<CompactNetworkMetrics> objs) throws HibernateException {
		Session session = null;
		Transaction transaction = null;     

		if (null == objs || objs.size() == 0) {
			log.info("Can not save empty list of compact network metrics");
			return;
		}     

		try {
			Iterator<CompactNetworkMetrics> objIterator = objs.iterator();
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			while (objIterator.hasNext())
			{
				int numRecordsProcessed = 0;
				while (numRecordsProcessed < ServiceFactory.hibernateBatchSize && objIterator.hasNext())
				{
					CompactNetworkMetrics clog = objIterator.next();
					session.saveOrUpdate(clog);
					log.debug("Compacts network Metrics record " + clog.toString() + " saved.");
					numRecordsProcessed++;               
				}            
				session.flush();  
				session.clear();
			}
			transaction.commit();         
		}
		catch (HibernateException e) {
			transaction.rollback();
			e.printStackTrace();
			throw new HibernateException("Cannot save compact network metrics.", e);
		} 
	}

	@Override
	public AggregatedNetworkData getAggregatedNetworkMetricsData (MetricsChartCriteria chartCriteria) throws HibernateException {

		String query =  NetworkMetricsChartUtil.getQueryForAggregatedData(chartCriteria);

		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			SQLQuery sqlquery = session.createSQLQuery(query);         
			//List<Long> results = (List<Long>) sqlquery.list();
			Object[] rawResult = (Object[]) sqlquery.uniqueResult();
			transaction.commit();
			log.info("there should be 3 columns returned " + rawResult.length);
			AggregatedNetworkData data = new AggregatedNetworkData();
			data.setAvgLatency(new Long (rawResult[0].toString()));
			data.setMaxLatency(new Long (rawResult[1].toString()));
			data.setTotalRequests(new Long (rawResult[2].toString()));
			data.setTotalErrors(new Long (rawResult[3].toString()));
			return data;

		} catch (HibernateException e) {
			transaction.rollback();
			throw new HibernateException("Cannot get compact network metrics. ", e);
		}  

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ClientNetworkMetrics> getRawNetworkMetricsData(NetworkMetricsRawCriteria nmRawCriteria) throws HibernateException
	{

		log.info ("Getting raw network metrics data  ");

		List<ClientNetworkMetrics> metrics = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(ClientNetworkMetrics.class);  

			List<Criterion> criteria = NetworkMetricsChartUtil.getRawNetworkMetricsCriteriaList(nmRawCriteria);

			if ( criteria !=null) {
				Iterator<Criterion> it = criteria.iterator();
				while (it.hasNext()){
					crit.add(it.next());
				}
			}
			String endField = new TimeRangeFilter(nmRawCriteria.getChartCriteria()).getPropertyName();
			crit.addOrder(Order.desc(endField));

			if (nmRawCriteria.getStartRow() > 0)
				crit.setFirstResult(nmRawCriteria.getStartRow());
			if (nmRawCriteria.getRowCount() > 0)
				crit.setMaxResults(nmRawCriteria.getRowCount());
			log.info("getting metrics with " +  " criteria " + criteria.toString() +  " order  endTime desc");

			metrics = (List<ClientNetworkMetrics>) crit.list();
			transaction.commit();
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get metrics from raw ClientNetworkMetrics ", e);
		} 

		return metrics;

	}
	
	@SuppressWarnings("unchecked")
	public List<ClientNetworkMetrics> getRawNetworkMetricsDataForAPointInTime(Long appId, Long endMinute, int maxResults) throws HibernateException {
		
		log.info("Getting raw network metrics for app id " + appId + " for endMinute " + endMinute);

		List<ClientNetworkMetrics> metrics = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery("from ClientNetworkMetrics as t where t.appId = :appId and t.endMinute= :minute");
			query.setParameter("appId", appId);
			query.setParameter("minute", endMinute);
			query.setMaxResults(maxResults);
			metrics = (List<ClientNetworkMetrics>) query.list();
			transaction.commit();

		} catch (HibernateException e) {
			transaction.rollback();
			throw new HibernateException("Problem getting network metrics  for app id " + appId + " for endMinute " + endMinute , e);
		} 
		return metrics;
		
	}

	@Override
	public int getRawNetworkMetricsDataCount(NetworkMetricsRawCriteria nmRawCriteria) throws HibernateException
	{


	     //this query is very expensive for various reasons. It does not make sense to do index
		   //on logMessage. So we are going to return 1000 for now.
		   log.warn("Always return 1000 for raw network metrics count for a given criteria until we find more efficient storage and counter solution");
		   return 1000;
		/*log.info ("Getting raw network metrics data row count  ");
		int count;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			Criteria crit = session.createCriteria(ClientNetworkMetrics.class);  

			List<Criterion> criteria = NetworkMetricsChartUtil.getRawNetworkMetricsCriteriaList(nmRawCriteria);

			if ( criteria !=null) {
				Iterator<Criterion> it = criteria.iterator();
				while (it.hasNext()){
					crit.add(it.next());
				}
			}

			crit.setProjection(Projections.rowCount());
			log.info("getting network metrics count criteria " + criteria.toString());

			count = (Integer) crit.uniqueResult();
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get count from raw ClientNetworkMetrics ", e);
		} 

		return count;
		*/
	}


	@SuppressWarnings("unchecked")
	public List<CompactNetworkMetrics> getCompactNetworkMetricsUsingNativeSqlQuery(MetricsChartCriteria cq) {

		SqlOrderGroupWhere og = NetworkMetricsChartUtil.getOrdersAndGroupings(cq);

		String query = "SELECT sum(numSamples) as numSamples, sum(numErrors) as numErrors," +
		" sum(sumLatency) as sumLatency, max(maxLatency) as maxLatency, min(minLatency) as minLatency,"+
		" sum(sumServerLatency) as sumServerLatency, max(maxServerLatency) as maxServerLatency, min(minServerLatency) as minServerLatency,"+
		" endMinute, endHour, endDay, endWeek,endMonth, devicePlatform,appConfigType, applicationVersion,deviceModel,deviceOperatingSystem,networkCarrier,networkType,domain " +
		" FROM COMPACT_NETWORK_METRICS where " +
		og.whereClause + " group by " + og.groupBy + " order by " + og.orderBy;


		Session session = null;
		Transaction transaction = null;
		List<CompactNetworkMetrics> returnList = null;
		try {
			session = ServiceFactory.getAnalyticsHibernateSession();
			transaction = session.beginTransaction();
			SQLQuery sqlquery = session.createSQLQuery(query)
			.addScalar("numSamples", Hibernate.LONG)
			.addScalar("numErrors", Hibernate.LONG)         
			.addScalar("sumLatency", Hibernate.LONG)
			.addScalar("maxLatency", Hibernate.LONG)
			.addScalar("minLatency", Hibernate.LONG)  
			.addScalar("sumServerLatency", Hibernate.LONG)
			.addScalar("maxServerLatency", Hibernate.LONG)
			.addScalar("minServerLatency", Hibernate.LONG)  
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
			.addScalar("networkType")
			.addScalar("domain");

			sqlquery.setResultTransformer(Transformers.aliasToBean(CompactNetworkMetrics.class));
			returnList  = (List<CompactNetworkMetrics>) sqlquery.list();
			transaction.commit();			

		} catch (HibernateException e) {
			transaction.rollback();
			e.printStackTrace();
			throw new HibernateException("Cannot get compact network metrics ", e);
		}
		return returnList;


	}

}
