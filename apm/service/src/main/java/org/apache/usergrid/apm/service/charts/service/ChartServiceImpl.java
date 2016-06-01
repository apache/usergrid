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

import org.apache.usergrid.apm.model.LogChartCriteria;
import org.apache.usergrid.apm.model.MetricsChartCriteria;
import org.apache.usergrid.apm.model.SessionChartCriteria;


public class ChartServiceImpl implements ChartService {

	private static final Log log = LogFactory.getLog(ChartServiceImpl.class);

	@Override
	public List<NetworkMetricsChartDTO> getNetworkMetricsChartData(MetricsChartCriteria cq) {
		log.info ("Getting chart data for " + cq.getChartName() + " " + cq.getAppId() );		

		return new NetworkChartStrategy().getChartData(cq);

	}

	@Override
	public List<SessionMetricsChartDTO> getSessionMetricsChartData(SessionChartCriteria criteria)
	{
		return new SessionChartStrategy().getChartData(criteria);
	}

	@Override
	public List<LogChartDTO> getLogChartData (LogChartCriteria criteria)
	{
		return new LogChartStrategy().getChartData(criteria);
	}				
}



/*List<NetworkMetricsChartDTO> chartData = null;
List<CompactNetworkMetrics> cachedMetrics = null;
List<CompactNetworkMetrics> freshMetrics = null;

//this comparison will be removed once I have right algo figured for hourly first
//Will need to use strategy pattern

if (cq.getLastX().equals(LastX.LAST_HOUR)) { 
   TimeRangeFilter trf = new TimeRangeFilter(LastX.LAST_HOUR);
   Long from = trf.getFrom();
   Long to = trf.getTo();
   log.info("Looking for hourly data between endMinute " + from + " AND " + to  );
   cachedMetrics = ServiceFactory.getWsHibernateServiceInstance().
      getHourlyComactMatrics(NetworkMetricsChartUtil.getChartCriteriaForCacheTable(cq), NetworkMetricsChartUtil.getOrdersForChartCriteria(cq));
   Long latestEndMinuteInCachedMetrics = 0l;
   if (cachedMetrics.size() > 0) {
      latestEndMinuteInCachedMetrics = findTheLatestTime(cachedMetrics);            
      log.info("rows found in hourly cache table upto endMinute " + latestEndMinuteInCachedMetrics);
   }

   if (to - latestEndMinuteInCachedMetrics > 0) {
      List<Criterion> crits = NetworkMetricsChartUtil.getChartCriteriaWithOverRiddenStartTime (cq, latestEndMinuteInCachedMetrics);
      ProjectionList projections = NetworkMetricsChartUtil.getProjectionList(cq);                       
      freshMetrics = ServiceFactory.getWsHibernateServiceInstance().getHourlyRawMetrics(crits,projections, NetworkMetricsChartUtil.getOrdersForChartCriteria(cq));           
   }
}



chartData = compileChartData (cachedMetrics, freshMetrics, cq);

//TODO: asynchronously save the freshMetrics into cache table
log.info("Saving " + freshMetrics.size() + " rows in hourly cache table");
ServiceFactory.getWsHibernateServiceInstance().saveHourlyCompactMetrics(freshMetrics);

return chartData;
 */ 

/**new ArrayList <NetworkMetricsChartDTO> ();
 * Algo:
 * Find if there is any grouping.
 * 	- If there is, split the metrics into different buckets
 *  - If not, return an array list containing one NetworkMetricsChartDTO
 * @param cachedMetrics
 * @param freshMetrics
 * @param cq
 * @return
 */
/*public List<NetworkMetricsChartDTO> compileChartData (List<CompactNetworkMetrics>  cachedMetrics, List<CompactNetworkMetrics> freshMetrics, ChartCriteria cq ) {

		//the simple case
		if (!cq.hasGrouping()) {
			return compileChartDataWithNoGrouping(cachedMetrics, freshMetrics, cq);

		} else {
			return compileChartDataWithGrouping(cachedMetrics, freshMetrics, cq);
		}
	}

	List<NetworkMetricsChartDTO> compileChartDataWithNoGrouping (List<CompactNetworkMetrics>  cachedMetrics, List<CompactNetworkMetrics> freshMetrics, ChartCriteria cq ) {
		List <NetworkMetricsChartDTO> charts = new ArrayList<NetworkMetricsChartDTO>();
		NetworkMetricsChartDTO dto = new NetworkMetricsChartDTO ();
		populateUIChartData(dto, cachedMetrics, cq.getId());
		populateUIChartData(dto, freshMetrics, cq.getId());
		charts.add(dto);
		return charts;

	}

	public List<NetworkMetricsChartDTO> compileChartDataWithGrouping (List<CompactNetworkMetrics>  cachedMetrics, List<CompactNetworkMetrics> freshMetrics, ChartCriteria cq ) {		
		Hashtable <String, Integer> groupIndex = getChartGroups (cachedMetrics, cq);		
		//We need to do this because there could be new group such as Sprint network while cached ones only has ATT and Verizon
		Hashtable <String, Integer> groupIndexFresh = getChartGroups (freshMetrics, cq);
		Enumeration <String> keys = groupIndexFresh.keys();
		while (keys.hasMoreElements()) {
			String temp = keys.nextElement();
			if(!groupIndex.containsKey(temp))
				groupIndex.put(temp, groupIndex.size());
		}

		log.info("Total number of chart groups " + groupIndex.size());	
		//TODO: There probably is a better way
		List <NetworkMetricsChartDTO> charts = new ArrayList<NetworkMetricsChartDTO>(groupIndex.size());
		for (int i = 0; i < groupIndex.size(); i++)  {
			charts.add(new NetworkMetricsChartDTO());
		}

		keys = groupIndex.keys();
		String key;
		NetworkMetricsChartDTO dto;
		while (keys.hasMoreElements()) {
			key = keys.nextElement();
			dto = charts.get(groupIndex.get(key));
			dto.setChartGroupName(key);			
		}

		populateUIChartDataWithGroup(charts, cachedMetrics, groupIndex, cq);
		populateUIChartDataWithGroup(charts, freshMetrics, groupIndex, cq);		
		return charts;

	}

	public Hashtable<String, Integer> getChartGroups (List<CompactNetworkMetrics> metrics, ChartCriteria cq) {
		Hashtable <String, Integer> groupIndex = new Hashtable<String,Integer> ();
		String previousGroup = "";
		int index = 0;
		if (cq.isGroupedByApp()) {
			for (int i = 0; i < metrics.size(); i++)  {
				if (!metrics.get(i).getAppId().toString().equals(previousGroup)) {
					previousGroup = metrics.get(i).getAppId().toString(); 
					groupIndex.put(previousGroup, index);
					index++;
				}
			}

		}
		else if (cq.isGroupedByNeworkProvider()) {
			for (int i = 0; i < metrics.size(); i++)  {
				if (!metrics.get(i).getNetworkCarrier().equals(previousGroup)) {
					previousGroup = metrics.get(i).getNetworkCarrier(); 
					groupIndex.put(previousGroup, index);
					index++;
				}
			}
		}
		else if (cq.isGroupedByNetworkType()) {
			for (int i = 0; i < metrics.size(); i++)  {
				if (!metrics.get(i).getNetworkType().equals(previousGroup)) {
					previousGroup = metrics.get(i).getNetworkType(); 
					groupIndex.put(previousGroup, index);
					index++;
				}
			}
		}
		return groupIndex;
	}


	public void populateUIChartData(NetworkMetricsChartDTO dto, List<CompactNetworkMetrics> metrics, Long chartCriteriaId)  {
		NetworkMetricsChartDataPoint dp;
		CompactNetworkMetrics tempMetrics;
		if (metrics != null) {
			for (int i = 0; i < metrics.size(); i++) {
				tempMetrics = metrics.get(i);
				dp = new NetworkMetricsChartDataPoint();
				dp.setErrors(tempMetrics.getNumErrors());
				dp.setMinLatency(tempMetrics.getMinLatency());
				dp.setMaxLatency(tempMetrics.getMaxLatency());
				dp.setSamples(tempMetrics.getNumSamples());
				if (tempMetrics.getNumSamples() != 0)
					dp.setAvgLatency(tempMetrics.getSumLatency()/tempMetrics.getNumSamples());
				else
					dp.setAvgLatency(0l); //so that it's not null ..will probably mess up the chart at UI otherwise
				dp.setTimestamp(new Date(tempMetrics.getEndMinute()*60*1000));
				dto.addDataPoint(dp);
				//So that we dont not have to iterate just to update chart criteria id.
				tempMetrics.setChartCriteriaId(chartCriteriaId);
			}

		}
	}

	public void populateUIChartDataWithGroup(List<NetworkMetricsChartDTO> dtos, List<CompactNetworkMetrics> metrics, Hashtable <String, Integer> groupIndex, ChartCriteria cq)  {
		NetworkMetricsChartDataPoint dp;
		CompactNetworkMetrics tempMetrics;
		NetworkMetricsChartDTO dto = null;

		if (metrics != null) {
			for (int i = 0; i < metrics.size(); i++) {
				tempMetrics = metrics.get(i);
				dp = new NetworkMetricsChartDataPoint();
				dp.setErrors(tempMetrics.getNumErrors());
				dp.setMinLatency(tempMetrics.getMinLatency());
				dp.setMaxLatency(tempMetrics.getMaxLatency());
				dp.setSamples(tempMetrics.getNumSamples());
				if (tempMetrics.getNumSamples() != 0)
					dp.setAvgLatency(tempMetrics.getSumLatency()/tempMetrics.getNumSamples());
				else 
					dp.setAvgLatency(0l); //so that it's not null ..will probably mess up the chart at UI otherwise
				dp.setTimestamp(new Date(tempMetrics.getEndMinute()*60*1000));				
				//So that we dont not have to iterate just to update chart criteria id.
				tempMetrics.setChartCriteriaId(cq.getId());
				if (cq.isGroupedByApp()) 
						dto = dtos.get(groupIndex.get(tempMetrics.getAppId().toString()).intValue());						
				else if (cq.isGroupedByNeworkProvider()) 
						dto = dtos.get(groupIndex.get(tempMetrics.getNetworkCarrier()));							
				else if (cq.isGroupedByNetworkType()) 
					dto = dtos.get(groupIndex.get(tempMetrics.getNetworkType()));
				dto.addDataPoint(dp);
			}

		}
	}


	private Long findTheLatestTime(List<CompactNetworkMetrics> metrics)  {
		Long max = 0L;
		for (int i = 0; i < metrics.size(); i++)  {
			if ( metrics.get(i).getEndMinute() > max)
				max = metrics.get(i).getEndMinute();
		}
		return max;
	}



}
 */
