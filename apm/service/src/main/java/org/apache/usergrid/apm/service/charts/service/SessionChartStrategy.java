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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.usergrid.apm.service.charts.filter.SpecialTimeFilter;
import org.apache.usergrid.apm.model.CompactSessionMetrics;
import org.apache.usergrid.apm.model.SessionChartCriteria;
import org.apache.usergrid.apm.model.ChartCriteria.PeriodType;
import org.apache.usergrid.apm.service.ServiceFactory;

public class SessionChartStrategy
{

	private static final Log log = LogFactory.getLog(SessionChartStrategy.class);

	public List<SessionMetricsChartDTO> getChartData(SessionChartCriteria cq) {
		SessionChartCriteria tempCQ;
		Long oldCQId = cq.getId();
		if (cq.getChartName().equals(SessionChartCriteriaService.AVERAGE_SESSION_TIME)) {
			log.info("Session chart criteria is set for AVERAGE_SESSION_TIME. But going to use ACTIVE_USERS_SESSION instead since" +
					"it can be computed based on that criteria ");
			tempCQ = ServiceFactory.getSessionChartCriteriaService().
					getDefaultChartCriteriaByName(cq.getAppId(), SessionChartCriteriaService.ACTIVE_USERS_SESSION);
			cq.setId(tempCQ.getId());
		}

		log.info ("Getting session chart data for " + cq.getChartName() + " " + cq.getAppId() );
		log.info("Period type is " + cq.getPeriodType());
		if (cq.getPeriodType() == PeriodType.LAST_X)
			log.info("LastX is " + cq.getLastX());
		if (cq.getPeriodType() == PeriodType.SET_PERIOD)
			log.info ("Time period is based on explicit start and end time" + cq.getStartDate() + " " + cq.getEndDate());

		List<SessionMetricsChartDTO> chartData = null;
		List<CompactSessionMetrics> metrics = null;

		SpecialTimeFilter tf = new SpecialTimeFilter(cq);
		Long from = tf.getFrom();
		Long to = tf.getTo();

		log.info("Looking for active session between " + from + " and " + to );

		//metrics = ServiceFactory.getSessionDBService().getCompactSessionMetricsUsingNativeSqlQuery(cq);
		if (tf.getEndPropName().equals("endMinute"))
			metrics = ServiceFactory.getSessionDBService().getNewCompactSessionMetricsUsingNativeSqlQuery(cq);
		else if (tf.getEndPropName().equals("endHour"))
			//For last day and last week view, we don't use compactSessionMetrics
			metrics = ServiceFactory.getSessionDBService().getSessionChartDataForHourlyGranularity(cq);
		else
			return null;
		chartData = compileChartData ( metrics, cq);

		log.info("number of total active sessions " + metrics.size());
		if (oldCQId != null)
			cq.setId(oldCQId);
		return chartData;
	}


	protected List<SessionMetricsChartDTO> compileChartData (List<CompactSessionMetrics>  metrics, SessionChartCriteria cq ) {

		//the simple case
		if (!cq.hasGrouping()) {
			return compileChartDataWithNoGrouping(metrics, cq);

		} else {
			return compileChartDataWithGrouping(metrics, cq);

		}
	}

	protected List<SessionMetricsChartDTO> compileChartDataWithNoGrouping (List<CompactSessionMetrics>  metrics, SessionChartCriteria cq ) {

		List <SessionMetricsChartDTO> charts = new ArrayList<SessionMetricsChartDTO>();     
		SessionMetricsChartDTO dto = new SessionMetricsChartDTO ();
		dto.setChartGroupName("N/A"); //not applicable
		populateUIChartData( dto, metrics, cq);
		charts.add(dto);
		return charts;

	}

	public List<SessionMetricsChartDTO> compileChartDataWithGrouping (List<CompactSessionMetrics>  metrics, SessionChartCriteria cq ) {
		List <SessionMetricsChartDTO> charts = new ArrayList<SessionMetricsChartDTO>();
		String propName = new SpecialTimeFilter(cq).getEndPropName();
		String previousGroup="previous";     
		String currentGroup = null;
		SessionMetricsChartDTO dto = null;
		SessionDataPoint dp = null;
		for (CompactSessionMetrics m: metrics) {
			if (cq.isGroupedByApp())        
				currentGroup = m.getAppId().toString();
			else if (cq.isGroupedByNetworkType())
				currentGroup = m.getNetworkType();
			else if (cq.isGroupedByNetworkCarrier())        
				currentGroup = m.getNetworkCarrier();
			else if (cq.isGroupedByAppVersion()) 
				currentGroup = m.getApplicationVersion();
			else if (cq.isGroupedByAppConfigType()) 
				currentGroup = m.getAppConfigType().toString();
			else if (cq.isGroupedByDeviceModel())  
				currentGroup = m.getDeviceModel();
			else if (cq.isGroupedbyDevicePlatform())
				currentGroup = m.getDevicePlatform();
			else if (cq.isGroupedByDeviceOS())
				currentGroup = m.getDeviceOperatingSystem();

			if (currentGroup != null && !currentGroup.equals(previousGroup)) {
				dto = new SessionMetricsChartDTO();
				dto.setChartGroupName(currentGroup);           
				charts.add(dto);
				previousGroup = currentGroup;
			}

			dp = new SessionDataPoint();
			dp.setNumSessions(m.getSessionCount());
			if("endHour".equals(propName)) 
				dp.setAvgSessionTime(m.getSumSessionLength()); //see the query at getSessionChartDataForHourlyGranularity. Avg is already calculated
			else {
				if (m.getSessionCount() != 0)
					dp.setAvgSessionTime(m.getSumSessionLength()/m.getSessionCount()/1000);//data is stored in ms but we want to show in seconds
				else
					dp.setAvgSessionTime(0L);
			}
			dp.setNumUsers(m.getNumUniqueUsers());

			if ("endMinute".equals(propName))
				dp.setTimestamp(new Date(m.getEndMinute()*60*1000));
			else if ("endHour".equals(propName))
				dp.setTimestamp(new Date(m.getEndHour()*60*60*1000));
			else if ("endDay".equals(propName))
				dp.setTimestamp(new Date(m.getEndDay()*24*60*60*1000));
			//TODO: Do the rest of end

            if (dto != null) {
                dto.addDataPoint(dp);
            }
        }
		return charts;

	}



	protected void populateUIChartData(SessionMetricsChartDTO dto, List<CompactSessionMetrics> metrics, SessionChartCriteria cq) {

		SessionDataPoint dp;
		CompactSessionMetrics tempMetrics;
		String propName = new SpecialTimeFilter(cq).getEndPropName();
		if (metrics != null) {
			for (int i = 0; i < metrics.size(); i++) {
				tempMetrics = metrics.get(i);
				dp = new SessionDataPoint();
				dp.setNumSessions(tempMetrics.getSessionCount());
				if("endHour".equals(propName)) 
					dp.setAvgSessionTime(tempMetrics.getSumSessionLength()); //see the query at getSessionChartDataForHourlyGranularity. Avg is already calculated
				else {
					if (tempMetrics.getSessionCount() != 0)
						dp.setAvgSessionTime(tempMetrics.getSumSessionLength()/tempMetrics.getSessionCount()/1000);//data is stored in ms but we want to show in seconds
					else
						dp.setAvgSessionTime(0L);
				}
				dp.setNumUsers(tempMetrics.getNumUniqueUsers());
				
				if ("endMinute".equals(propName))
					dp.setTimestamp(new Date(tempMetrics.getEndMinute()*60*1000));
				else if ("endHour".equals(propName))
					dp.setTimestamp(new Date(tempMetrics.getEndHour()*60*60*1000));
				else if ("endDay".equals(propName))
					dp.setTimestamp(new Date(tempMetrics.getEndDay()*24*60*60*1000));
				//TODO: Fill up the rest but that's not likely going to be shown in real time            


				dto.addDataPoint(dp);
			}
		}
	}



}
