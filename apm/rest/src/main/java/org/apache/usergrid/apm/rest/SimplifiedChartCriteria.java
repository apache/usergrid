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

public class SimplifiedChartCriteria {
	
	public static String TYPE_SESSION = "session";
	public static String TYPE_LOG = "log";
	public static String TYPE_NETWORK = "network";
	
	private Long chartCriteriaId;
	private Long instaOpsApplicationId;
	private String chartName;
	private String chartDescription;
	private String type; //session or log or network
	
	public Long getChartCriteriaId() {
		return chartCriteriaId;
	}
	public void setChartCriteriaId(Long chartCriteriaId) {
		this.chartCriteriaId = chartCriteriaId;
	}
	public Long getInstaOpsApplicationId() {
		return instaOpsApplicationId;
	}
	public void setInstaOpsApplicationId(Long instaOpsApplicationId) {
		this.instaOpsApplicationId = instaOpsApplicationId;
	}
	public String getChartName() {
		return chartName;
	}
	public void setChartName(String chartName) {
		this.chartName = chartName;
	}
	public String getChartDescription() {
		return chartDescription;
	}
	public void setChartDescription(String chartDescription) {
		this.chartDescription = chartDescription;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	
	

}
