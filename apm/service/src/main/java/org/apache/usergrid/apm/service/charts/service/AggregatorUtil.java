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
import java.util.Collections;
import java.util.List;


import org.apache.usergrid.apm.model.ApigeeMobileAPMConstants;

public class AggregatorUtil
{

	public static AggregatedNetworkData getNetworkAggregatedData (List<NetworkMetricsChartDTO> metricsChartDTO)  {
		AggregatedNetworkData data = null; 

		Long avgLatency=0L,  maxLatency=0L, totalRequests =0L, totalErrors=0L, sumLatency = 0L;

		for (NetworkMetricsChartDTO dto :metricsChartDTO) {
			List<NetworkMetricsChartDataPoint> dps = dto.getDatapoints();
			for (NetworkMetricsChartDataPoint dp : dps) {
				sumLatency += dp.getAvgLatency()* dp.getSamples();
				totalRequests += dp.getSamples();
				maxLatency = Math.max(maxLatency,dp.getMaxLatency());
				totalErrors += dp.getErrors();
			}
		}
		avgLatency = (totalRequests>0)?sumLatency / totalRequests:0;
		data = new AggregatedNetworkData();
		data.setAvgLatency(avgLatency);
		data.setMaxLatency(maxLatency);
		data.setTotalErrors(totalErrors);
		data.setTotalRequests(totalRequests);
		return data;
	}

	public static AggregatedLogData getLogAggregatedData(List<LogChartDTO> logChartDTO)  {
		AggregatedLogData data = null;
		Long totalAsserts = 0L, totalErrors=0L,  totalWarnings=0L, totalEvents =0L, totalCrashes = 0L, totalErrorAndAboveCount =0L;
		for (LogChartDTO dto : logChartDTO) {
			List<LogDataPoint> dps = dto.getDatapoints();
			for (LogDataPoint dp : dps) {
				totalErrorAndAboveCount += dp.getErrorAndAboveCount(); //error + assert
				totalAsserts += dp.getAssertCount();
				totalErrors += dp.getErrorCount();
				totalWarnings += dp.getWarnCount();
				totalEvents += dp.getEventCount();
				totalCrashes += dp.getCrashCount();
			}
		}
		data = new AggregatedLogData();
		data.setTotalErrorsAndAbove(totalErrorAndAboveCount); //usually means error and crashes
		data.setTotalAsserts(totalAsserts);
		data.setTotalErrors(totalErrors);
		data.setTotalWarnings(totalWarnings);
		data.setTotalEvents(totalEvents);
		data.setTotalCrashes(totalCrashes);
		data.setTotalAppErrors(totalErrorAndAboveCount-totalCrashes);
		data.setTotalCriticalErrors(totalAsserts - totalCrashes);
		return data;
	}

	/**
	 * This util method is a bit different from similar util for network and log data bacause data needs to be compiled from
	 * CompactSessionMetrics as well as SummarySessionMetrics.
	 * @param sessionChartDTO
	 * @param cq
	 * @return
	 */

	public static AggregatedSessionData getSessionAggregatedData (List<SessionMetricsChartDTO> sessionChartDTO, AggregatedSessionData data) {
		Long maxConcurrentSessions = 0L;
		for ( SessionMetricsChartDTO dto: sessionChartDTO) {
			List<SessionDataPoint> dps = dto.getDatapoints();
			for (SessionDataPoint dp : dps) {
				maxConcurrentSessions = Math.max (maxConcurrentSessions, dp.getNumSessions());
			}
		}
		data.setMaxConcurrentSessions(maxConcurrentSessions);
		return data;
	}

	/**
	 * This is currently used for getting crashes by deviceModel, device Paltform and platform versions
	 * @param logChartDTO
	 * @return
	 */

	public static List<AttributeValueChartData> getRankedCrashData(List<LogChartDTO> logChartDTO, String chartType)  {

		List<AttributeValueChartData> data = new ArrayList<AttributeValueChartData>();
		Long totalCrashes = 0L;
		Long grandTotal = 0L;
		for (LogChartDTO dto : logChartDTO) {				    	  
			List<LogDataPoint> dps = dto.getDatapoints();
			totalCrashes = 0L;
			for (LogDataPoint dp : dps) {
				totalCrashes += dp.getCrashCount();
			}
			grandTotal += totalCrashes;
			if (totalCrashes > 0) {//if no crash for this attribute then we don't want to include it in pie or bar chart
				AttributeValueChartData entry = new AttributeValueChartData();
				entry.setAttribute(dto.getChartGroupName());
				entry.setValue(totalCrashes);
				data.add(entry);
			}			

		}
		Collections.sort(data);
		for (AttributeValueChartData d : data)
			d.setPercentage((d.getValue().doubleValue()/grandTotal) * 100 );
		return AggregatorUtil.sliceDataForChartType(data, chartType, grandTotal);

	}

	public static List<AttributeValueChartData> getRankedAppErrorData(List<LogChartDTO> logChartDTO, String chartType)  {

		List<AttributeValueChartData> data = new ArrayList<AttributeValueChartData>();
		Long totalAppErrors = 0L;
		Long grandTotal = 0L;
		for (LogChartDTO dto : logChartDTO) {				    	  
			List<LogDataPoint> dps = dto.getDatapoints();
			totalAppErrors = 0L;
			for (LogDataPoint dp : dps) {
				totalAppErrors += (dp.getErrorAndAboveCount() - dp.getCrashCount());
			}
			grandTotal += totalAppErrors;
			if (totalAppErrors > 0) {//if no appErrors for this attribute then we don't want to include it in pie or bar chart
				AttributeValueChartData entry = new AttributeValueChartData();
				entry.setAttribute(dto.getChartGroupName());
				entry.setValue(totalAppErrors);
				data.add(entry);
			}			

		}
		Collections.sort(data);
		for (AttributeValueChartData d : data)
			d.setPercentage((d.getValue().doubleValue()/grandTotal) * 100 );
		return AggregatorUtil.sliceDataForChartType(data, chartType, grandTotal);

	}


	public static List<AttributeValueChartData> getRankedAvgResponseTimeData(List<NetworkMetricsChartDTO> chartDTO, String chartType)  {
		List<AttributeValueChartData> data = new ArrayList<AttributeValueChartData>();
		Long totalAvgResponseTime = 0L;
		Long grandTotal = 0L;
		for (NetworkMetricsChartDTO dto : chartDTO) {				    	  
			List<NetworkMetricsChartDataPoint> dps = dto.getDatapoints();
			totalAvgResponseTime = 0L;
			for (NetworkMetricsChartDataPoint dp : dps) {
				totalAvgResponseTime += dp.getAvgLatency();
			}			

			AttributeValueChartData entry = new AttributeValueChartData();
			entry.setAttribute(dto.getChartGroupName());
			entry.setValue(totalAvgResponseTime/dps.size());//avg of avg -- not really exact but should be close
			data.add(entry);


		}
		Collections.sort(data);
		//We don't need percentage here.
		/*
		for (AttributeValueChartData d : data)
			d.setPercentage((d.getValue().doubleValue()/grandTotal) * 100 );
		 */
		return AggregatorUtil.sliceDataForChartType(data, chartType, grandTotal);


	}

	public static List<NetworkRequestsErrorsChartData> getRankedNetworkRequestsErrorsData (List<NetworkMetricsChartDTO> chartDTO, String chartType)  {
		List<NetworkRequestsErrorsChartData> data = new ArrayList<NetworkRequestsErrorsChartData>();
		Long totalRequests = 0L;
		Long totalErrors = 0L;
		Long grandTotalRequests = 0L;
		Long grandTotalErrors = 0L;

		for (NetworkMetricsChartDTO dto : chartDTO) {				    	  
			List<NetworkMetricsChartDataPoint> dps = dto.getDatapoints();
			totalErrors = 0L;
			totalRequests = 0L;
			for (NetworkMetricsChartDataPoint dp : dps) {
				totalErrors += dp.getErrors();
				totalRequests += dp.getSamples();
			}
			grandTotalErrors += totalErrors;
			grandTotalRequests += totalRequests;
			NetworkRequestsErrorsChartData entry = new NetworkRequestsErrorsChartData();
			entry.setAttribute(dto.getChartGroupName());
			entry.setErrors(totalErrors);
			entry.setRequests(totalRequests);
			data.add(entry);
		}

		Collections.sort(data);
		for (NetworkRequestsErrorsChartData d : data) {
			d.setRequestPercentage((d.getRequests().doubleValue()/grandTotalRequests) * 100 );
			if (grandTotalErrors != 0)
				d.setErrorPercentage((d.getErrors().doubleValue()/grandTotalErrors)*100);			
		}

		return AggregatorUtil.sliceDataForNetworkChartType(data, chartType, grandTotalRequests, grandTotalErrors);
	}




	public static List<AttributeValueChartData> updatePercentage(List<AttributeValueChartData> sortedData, String chartType)  {

		Long grandTotal = 0L;
		for (AttributeValueChartData d : sortedData)				    	  
			grandTotal +=  d.getValue();		

		for (AttributeValueChartData d : sortedData)
			d.setPercentage((d.getValue().doubleValue()/grandTotal) * 100 );
		return AggregatorUtil.sliceDataForChartType(sortedData, chartType, grandTotal);

	}

	public static List<AttributeValueChartData> sliceDataForChartType (List<AttributeValueChartData> data, String chartType, Long grandTotal) {
		//For pie chart we only show top 5 sections and bundle rest to "others". For bar chart, we show top 10 sections and bundle rest to others 
		int numDistinctAttributes = 0;
		if (ApigeeMobileAPMConstants.CHART_VIEW_PIE.equalsIgnoreCase(chartType)) 
			numDistinctAttributes = 5;
		else if (ApigeeMobileAPMConstants.CHART_VIEW_BAR.equalsIgnoreCase(chartType))
			numDistinctAttributes = 10;
		else 
			return data; //nothing to slice - return the whole thing
		List<AttributeValueChartData> dataSubSet = null;
		if (data.size() > numDistinctAttributes) {
			dataSubSet = data.subList(0, numDistinctAttributes);					
		}
		else
			return data;
		if (grandTotal ==0) //for some reason, it did happen and others calculation does not make sense in this case
			return dataSubSet;
		//Now bundle remaining to others
		AttributeValueChartData others = new AttributeValueChartData();
		others.setAttribute("others");
		Long firstXtotal = 0L;
		for (AttributeValueChartData d : dataSubSet)
			firstXtotal += d.getValue();
		others.setValue(grandTotal - firstXtotal);
		others.setPercentage((others.getValue().doubleValue()/grandTotal) * 100);
		dataSubSet.add(others);
		return dataSubSet;

	}


	public static List<NetworkRequestsErrorsChartData> sliceDataForNetworkChartType(List<NetworkRequestsErrorsChartData> data, String chartType, Long grandTotalRequests, Long grandTotalErrors) {

		//For pie chart we only show top 5 sections and bundle rest to "others". For bar chart, we show top 10 sections and bundle rest to others 
		int numDistinctAttributes = 0;
		if (ApigeeMobileAPMConstants.CHART_VIEW_PIE.equalsIgnoreCase(chartType)) 
			numDistinctAttributes = 5;
		else if (ApigeeMobileAPMConstants.CHART_VIEW_BAR.equalsIgnoreCase(chartType))
			numDistinctAttributes = 10;
		else 
			return data; //nothing to slice - return the whole thing
		List<NetworkRequestsErrorsChartData> dataSubSet = null;
		if (data.size() > numDistinctAttributes) {
			dataSubSet = data.subList(0, numDistinctAttributes);					
		}
		else
			return data;
		if (grandTotalRequests ==0) //for some reason, it did happen and others calculation does not make sense in this case
			return dataSubSet;
		//Now bundle remaining to others
		NetworkRequestsErrorsChartData others = new NetworkRequestsErrorsChartData();
		others.setAttribute("others");
		Long firstXtotalRequests = 0L;
		Long firstXtotalErrors = 0L;
		for (NetworkRequestsErrorsChartData d : dataSubSet) {
			firstXtotalRequests += d.getRequests();
			firstXtotalErrors += d.getErrors();
		}
		others.setRequests(grandTotalRequests - firstXtotalRequests);
		others.setErrors(grandTotalErrors - firstXtotalErrors);
		if (grandTotalErrors != 0)
			others.setErrorPercentage((others.getErrors().doubleValue()/grandTotalErrors) * 100);
		dataSubSet.add(others);
		return dataSubSet;
		

	}


}
