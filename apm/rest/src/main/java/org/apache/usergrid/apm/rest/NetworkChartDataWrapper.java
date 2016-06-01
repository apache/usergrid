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
package org.apache.usergrid.apm.rest;

import java.util.List;

import org.apache.usergrid.apm.service.charts.service.AggregatedNetworkData;
import org.apache.usergrid.apm.service.charts.service.AttributeValueChartData;
import org.apache.usergrid.apm.service.charts.service.NetworkRequestsErrorsChartData;

/**
 * 
 * @author ApigeeCorporation
 * This is different from Log and Session Chart data wrapped because for network, we show two different charts.
 * 
 */
public class NetworkChartDataWrapper {
	
	AggregatedNetworkData summaryData;
	List<AttributeValueChartData> responseTimes;
	List<NetworkRequestsErrorsChartData> requestErrorCounts;

	public AggregatedNetworkData getSummaryData() {
		return summaryData;
	}

	public void setSummaryData(AggregatedNetworkData summaryData) {
		this.summaryData = summaryData;
	}	

	public List<AttributeValueChartData> getResponseTimes() {
		return responseTimes;
	}

	public void setResponseTimes(List<AttributeValueChartData> responseTimes) {
		this.responseTimes = responseTimes;
	}

	public List<NetworkRequestsErrorsChartData> getRequestErrorCounts() {
		return requestErrorCounts;
	}

	public void setRequestErrorCounts(
			List<NetworkRequestsErrorsChartData> requestErrorCounts) {
		this.requestErrorCounts = requestErrorCounts;
	}



}
