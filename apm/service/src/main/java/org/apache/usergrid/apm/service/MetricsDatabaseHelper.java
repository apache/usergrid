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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;

import org.apache.usergrid.apm.model.ClientNetworkMetrics;
import org.apache.usergrid.apm.service.charts.filter.AppsFilter;
import org.apache.usergrid.apm.service.charts.filter.DateIntervalFilter;
import org.apache.usergrid.apm.service.charts.filter.UrlFilter;
import org.apache.usergrid.apm.service.charts.service.NetworkMetricsChartUtil;
import org.apache.usergrid.apm.model.MetricsChartCriteria;

public class MetricsDatabaseHelper {

	private static final Log log = LogFactory.getLog(MetricsDatabaseHelper.class);

	/**
	 * This method ideally should not be used since it's potentially lots of data
	 * @parama appId
	 */

	public static List<ClientNetworkMetrics> getMetricsFor (Long appId) {
		if (appId == null)
			return new ArrayList<ClientNetworkMetrics> ();
		return ServiceFactory.getMetricsDBServiceInstance().getNetworkMetricsForApp(appId);		

	}

	/**
	 * This metho
	 * @param appId
	 * @param url
	 * @return
	 * None of these parameters can be null.
	 */

	public static List<ClientNetworkMetrics> getMetricsFor (Long appId, String url) {
		if (appId == null || url == null )
			return new ArrayList<ClientNetworkMetrics> ();

		List<Criterion> filters = new ArrayList<Criterion>();
		List<Order> orders = new ArrayList<Order>();
		filters.add((new AppsFilter(appId)).getCriteria());
		filters.add((new UrlFilter(url)).getCriteria());
		orders.add(Order.asc("startTime"));		
		return ServiceFactory.getMetricsDBServiceInstance().getNetworkMetricsList(null, 0, filters, orders);	

	}

	/**
	 * Note that startTime and endTime will be used in query as: where START_TIME > startTime AND
	 * START_TIME < endTime . This should be okay since it's more likely to be off only by few seconds
	 * and nobody is going to sue us. This helps with database indexing as well.
	 * @param appId
	 * @param startTime
	 * @param endTime
	 * @return List of beans for all urls for an app with given time interval
	 * None of these parameters can be null.
	 */
	public static List<ClientNetworkMetrics> getMetricsFor (Long appId, Date startTime, Date endTime) {
		if (appId == null || startTime == null)
			return new ArrayList<ClientNetworkMetrics> ();
		List<Criterion> filters = new ArrayList<Criterion>();
		List<Order> orders = new ArrayList<Order>();
		filters.add((new AppsFilter(appId)).getCriteria());
		filters.add((new DateIntervalFilter (startTime,endTime)).getCriteria());
		orders.add(Order.asc("startTime"));
		log.info("filter size " + filters.size());
		return ServiceFactory.getMetricsDBServiceInstance().getNetworkMetricsList(null, 0, filters, orders);		

	}

	/**
	 * Note that startTime and endTime will be used in query as: where START_TIME > startTime AND
	 * START_TIME < endTime . This should be okay since it's more likely to be off only by few seconds
	 * and nobody is going to sue us. This helps with database indexing as well.
	 * 
	 * @param appId
	 * @param url
	 * @param startTime
	 * @param endTime
	 * @return List of beans for given url for an app with given time interval ordered by startTime in ascending order. Use
	 * @see getMetricsFor (Long appId, String url, Date startTime, Date endTime, boolean isAscending) to get your ordering
	 * None of these parameters can be null.
	 */



	public static List<ClientNetworkMetrics> getMetricsFor (Long appId, String url, Date startTime, Date endTime) {
		if (appId == null || url == null || startTime == null)
			return new ArrayList<ClientNetworkMetrics> ();
		List<Criterion> filters = new ArrayList<Criterion>();
		List<Order> orders = new ArrayList<Order>();
		filters.add((new AppsFilter(appId)).getCriteria());
		filters.add((new UrlFilter(url)).getCriteria());
		filters.add((new DateIntervalFilter (startTime,endTime)).getCriteria());
		orders.add(Order.asc("startTime"));
		log.info("filter size " + filters.size());
		return ServiceFactory.getMetricsDBServiceInstance().getNetworkMetricsList(null, 0, filters, orders);
	}

	/**
	 * Note that startTime and endTime will be used in query as: where START_TIME > startTime AND
	 * START_TIME < endTime . This should be okay since it's more likely to be off only by few seconds
	 * and nobody is going to sue us. This helps with database indexing as well. None of these parameters can be null.
	 * 
	 * @param appId
	 * @param url
	 * @param startTime
	 * @param endTime
	 * @return List of beans for given url for an app with given time interval ordered by startTime in given order. 
	 * 
	 */

	public static List<ClientNetworkMetrics> getMetricsFor (Long appId, String url, Date startTime, Date endTime, boolean isAscending) {
		if (appId == null || url == null || startTime == null)
			return new ArrayList<ClientNetworkMetrics> ();
		List<Criterion> filters = new ArrayList<Criterion>();
		List<Order> orders = new ArrayList<Order>();
		filters.add((new AppsFilter(appId)).getCriteria());
		filters.add((new UrlFilter(url)).getCriteria());
		filters.add((new DateIntervalFilter (startTime,endTime)).getCriteria());
		if (isAscending)
			orders.add(Order.asc("endTime"));
		else
			orders.add(Order.desc("endTime"));

		return ServiceFactory.getMetricsDBServiceInstance().getNetworkMetricsList(null, 0, filters, orders);
	}

	public static List<ClientNetworkMetrics> getMetricsFor (Long appId, String url, Date startTime, Date endTime, int maxRow) {
		if (appId == null || url == null || startTime == null)
			return new ArrayList<ClientNetworkMetrics> ();
		List<Criterion> filters = new ArrayList<Criterion>();
		List<Order> orders = new ArrayList<Order>();
		filters.add((new AppsFilter(appId)).getCriteria());
		filters.add((new UrlFilter(url)).getCriteria());
		filters.add((new DateIntervalFilter (startTime,endTime)).getCriteria());
		orders.add(Order.asc("endTime"));

		return ServiceFactory.getMetricsDBServiceInstance().getNetworkMetricsList(null, maxRow, filters, orders);
	}
	/**
	 * None of these parameters can be null.
	 * @param appId
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	public static List<String> getUniqueUrlsFor (Long appId, Date startTime, Date endTime)  {
		if(appId == null || startTime == null)
			return new ArrayList<String> ();

		List<Criterion> filters = new ArrayList<Criterion>();		
		filters.add((new AppsFilter(appId)).getCriteria());		
		filters.add((new DateIntervalFilter (startTime,endTime)).getCriteria());
		return ServiceFactory.getMetricsDBServiceInstance().getUniqueUrls(filters);


	}

	
	public static List<Object [][]> getSummarizeMetrics(MetricsChartCriteria metricsChartCriteria)
	{

		/**
		 * Pseudocode:
		 * 1. Get the ChartCriteria
		 * 2. Populate the Criteria Object
		 * 3. Execute the query
		 */

		List wsRecList = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();

			Criteria crit = session.createCriteria(ClientNetworkMetrics.class);

			List<Criterion> criteria = NetworkMetricsChartUtil.getChartCriteriaList(metricsChartCriteria);

			if (criteria !=null) {
				Iterator<Criterion> it = criteria.iterator();
				while (it.hasNext()){
					crit.add(it.next());
				}
			}

			ProjectionList projList = NetworkMetricsChartUtil.getProjectionList(metricsChartCriteria);

			if(projList != null)
			{
				crit.setProjection(projList);
			} else
			{
				throw new HibernateException("Projection List was null. Cannot calculate Sumarized metrics ");
			}

			//crit.addOrder(Order.asc("Interval"));

			wsRecList = crit.list();

			transaction.commit();

		} catch (Exception e) {			
			e.printStackTrace();			
			transaction.rollback();
			throw new HibernateException("Cannot get summarized metrcis. ", e);
		} 

		return wsRecList;
	}




}
