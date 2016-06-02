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
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;

import org.apache.usergrid.apm.model.ClientSessionMetrics;
import org.apache.usergrid.apm.service.charts.service.AggregatedSessionData;
import org.apache.usergrid.apm.service.charts.service.AttributeValueChartData;
import org.apache.usergrid.apm.model.CompactSessionMetrics;
import org.apache.usergrid.apm.model.SessionChartCriteria;
import org.apache.usergrid.apm.model.SummarySessionMetrics;


public interface SessionDBService
{

   public void saveSessionMetrics(ClientSessionMetrics sessionDatum) throws HibernateException;

   /**
    * 
    * @param pcb
    * @throws HibernateException
    */
   public void saveSessionMetricsInBatch(List<ClientSessionMetrics> sessionData) throws HibernateException;


   /**
    * For some weird reason you may want to get all the rows from DB. Make sure you have big enough heap size.
    * It is primiarily to help with initial testing
    * @return
    * @throws HibernateException
    */

   public List<ClientSessionMetrics> getAllSessionMetrics() throws HibernateException;


   /**
    * @param startId  if null, first result criterion is not set
    * @param numberOfRows if 0, maxResult criteria is not set
    * @param criteria   
    * @param order
    * @return
    * @throws HibernateException
    */

   public List<ClientSessionMetrics> getSessionList(Integer startId, int numberOfRows, List<Criterion> criteria, List<Order> orders) throws HibernateException;

   public List<ClientSessionMetrics> getSessionMetricsForApp(Long appId) throws HibernateException;


   //public List<CompactSessionMetrics> getHourlyRawSessionMetrics(List<Criterion> criteria, ProjectionList projections, List<Order> orders) throws HibernateException;

   public List<CompactSessionMetrics> getComactSessionMetrics(List<Criterion> criteria, List<Order> orders) throws HibernateException;
   
   public List<CompactSessionMetrics> getCompactSessionMetrics(List<Criterion> criteria, ProjectionList projections, List<Order> orders) throws HibernateException;

   public void saveCompactSessionMetrics(List<CompactSessionMetrics> objs) throws HibernateException;

   public void saveCompactSessionMetrics(CompactSessionMetrics obj) throws HibernateException;

   public void saveSummarySessionMetrics(SummarySessionMetrics sessionDatum) throws HibernateException;  
   
   public List<SummarySessionMetrics> getSummarySessionsForDevices (Long appId, List<String> deviceIds, Date sessionExpiryTime) throws HibernateException;
      
   public List<SummarySessionMetrics> getSummarySessionsBySessionId (Long appId, Collection<String> sessionIds) throws HibernateException;
   
   
   

   /**
    * 
    * @param pcb
    * @throws HibernateException
    */
   public void saveSummarySessionMetricsInBatch(List<SummarySessionMetrics> sessionData) throws HibernateException;

   public List<SummarySessionMetrics> getSummarySessionMetrics(List<Criterion> criteria, List<Order> orders) throws HibernateException;
   

   public Long getCompactSessionMetricsRowCount();
   
   public Long getClientSessionMetricsRowCount();
   
   public List<CompactSessionMetrics> getCompactSessionMetricsUsingNativeSqlQuery (SessionChartCriteria cq);
   public List<CompactSessionMetrics> getNewCompactSessionMetricsUsingNativeSqlQuery (SessionChartCriteria cq);
   
   public AggregatedSessionData getAggreateSessionData (SessionChartCriteria chartCriteria) throws HibernateException;
   
   public List<String> getDistinctValuesFromSummarySessionMetrics (String column, Date since) throws HibernateException;
   
   public List<CompactSessionMetrics> getSessionChartDataForHourlyGranularity (SessionChartCriteria cq);

   public Long getSummarySessionMetricsRowCountByApp(Long appId);
   
   public List<AttributeValueChartData> getCountFromSummarySessionMetrics(SessionChartCriteria cq) throws HibernateException;
}
