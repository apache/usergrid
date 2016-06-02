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
import org.apache.usergrid.apm.model.CompactNetworkMetrics;
import org.apache.usergrid.apm.model.MetricsChartCriteria;
import org.apache.usergrid.apm.service.ServiceFactory;

public class NetworkChartStrategy {

	private static final Log log = LogFactory.getLog(NetworkChartStrategy.class);


	public List<NetworkMetricsChartDTO> getChartData(MetricsChartCriteria cq) {
		log.info ("Getting houlry chart data for " + cq.getChartName() + " for app " + cq.getAppId() );
				
		List<NetworkMetricsChartDTO> chartData = null;
		List<CompactNetworkMetrics> compactNetworkMetrics = null;

		SpecialTimeFilter tf = new SpecialTimeFilter(cq);
		Long from = tf.getFrom();
		Long to = tf.getTo();

		log.info("Looking for network metrics data for " + tf.getEndPropName() + " between " + from + " and " + to );

		compactNetworkMetrics = ServiceFactory.getMetricsDBServiceInstance().getCompactNetworkMetricsUsingNativeSqlQuery(cq);
		log.info("number of total compact network metrics " + compactNetworkMetrics.size());
		chartData = compileChartData ( compactNetworkMetrics, cq);
		
		return chartData;
	}


	protected List<NetworkMetricsChartDTO> compileChartData (List<CompactNetworkMetrics>  metrics, MetricsChartCriteria cq ) {

		//the simple case
		if (!cq.hasGrouping()) {
			return compileChartDataWithNoGrouping(metrics, cq);

		} else {
			return compileChartDataWithGrouping(metrics, cq);

		}
	}

	protected List<NetworkMetricsChartDTO> compileChartDataWithNoGrouping (List<CompactNetworkMetrics>  metrics, MetricsChartCriteria cq ) {

		List <NetworkMetricsChartDTO> charts = new ArrayList<NetworkMetricsChartDTO>();     
		NetworkMetricsChartDTO dto = new NetworkMetricsChartDTO ();
		dto.setChartGroupName("N/A"); //not applicable
		populateUIChartData( dto, metrics, cq);
		charts.add(dto);
		return charts;

	}

	public List<NetworkMetricsChartDTO> compileChartDataWithGrouping (List<CompactNetworkMetrics>  metrics, MetricsChartCriteria cq ) {
		String propName = new SpecialTimeFilter(cq).getEndPropName();
		List <NetworkMetricsChartDTO> charts = new ArrayList<NetworkMetricsChartDTO>();
		log.info("Populating network metrics chart dto with group based on end* " + propName);
		String previousGroup="previous";     
		String currentGroup = "";
		NetworkMetricsChartDTO dto = null;
		NetworkMetricsChartDataPoint dp = null;
		for (CompactNetworkMetrics m: metrics) {
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
			else if (cq.isGroupedByDeviceOS()) //aka plaform version
				currentGroup = m.getDeviceOperatingSystem();
			else if (cq.isGroupedByDomain()) 
				currentGroup = m.getDomain();

			if (!currentGroup.equals(previousGroup)) {
				dto = new NetworkMetricsChartDTO();
				dto.setChartGroupName(currentGroup);           
				charts.add(dto);
				previousGroup = currentGroup;
			}

			dp = new NetworkMetricsChartDataPoint();
			dp.setErrors(m.getNumErrors());
			dp.setMinLatency(m.getMinLatency());
			dp.setMaxLatency(m.getMaxLatency());			
			dp.setSamples(m.getNumSamples());
			if (m.getNumSamples() != 0) {
				dp.setAvgLatency(m.getSumLatency()/m.getNumSamples());
				dp.setAvgServerLatency(m.getSumServerLatency()/m.getNumSamples());
			}
			else { 
				dp.setAvgLatency(0l); //so that it's not null ..will probably mess up the chart at UI otherwise
				dp.setAvgServerLatency(0L);
			}

			dp.setMinServerLatency(m.getMinServerLatency());
			dp.setMaxServerLatency(m.getMaxServerLatency());
			if ("endMinute".equals(propName))
				dp.setTimestamp(new Date(m.getEndMinute()*60*1000));
			else if ("endHour".equals(propName))
				dp.setTimestamp(new Date(m.getEndHour()*60*60*1000));
			else if ("endDay".equals(propName))
				dp.setTimestamp(new Date(m.getEndDay()*24*60*60*1000)); 
			dto.addDataPoint(dp);
		}
		return charts;

	}



	protected void populateUIChartData(NetworkMetricsChartDTO dto, List<CompactNetworkMetrics> metrics, MetricsChartCriteria cq) { 
		NetworkMetricsChartDataPoint dp;
		CompactNetworkMetrics tempMetrics;
		String propName = new SpecialTimeFilter(cq).getEndPropName();
		if (metrics != null) {
			for (int i = 0; i < metrics.size(); i++) {
				tempMetrics = metrics.get(i);
				dp = new NetworkMetricsChartDataPoint();
				dp.setErrors(tempMetrics.getNumErrors());
				dp.setMinLatency(tempMetrics.getMinLatency());
				dp.setMaxLatency(tempMetrics.getMaxLatency());
				dp.setSamples(tempMetrics.getNumSamples());
				if (tempMetrics.getNumSamples() != 0) {
					dp.setAvgLatency(tempMetrics.getSumLatency()/tempMetrics.getNumSamples());
					dp.setAvgServerLatency(tempMetrics.getSumServerLatency()/tempMetrics.getNumSamples());
				}
				else {
					dp.setAvgLatency(0l); //so that it's not null ..will probably mess up the chart at UI otherwise
					dp.setAvgServerLatency(0L);
				}

				dp.setMinServerLatency(tempMetrics.getMinServerLatency());
				dp.setMaxServerLatency(tempMetrics.getMaxServerLatency());


				if ("endMinute".equals(propName))
					dp.setTimestamp(new Date(tempMetrics.getEndMinute()*60*1000));
				else if ("endHour".equals(propName))
					dp.setTimestamp(new Date(tempMetrics.getEndHour()*60*60*1000));
				else if ("endDay".equals(propName))
					dp.setTimestamp(new Date(tempMetrics.getEndDay()*24*60*60*1000));         

				dto.addDataPoint(dp);
			}
		}
	}

}
