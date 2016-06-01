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

/**
 * This is used for constructing data for bar and pie chart. Returned data is in descending order
 * Added summary data as well since we show that with bar chart as well now.
 * @author prabhat jha
 *
 */

public class AttributeValueChartData implements Comparable<AttributeValueChartData>{
	
	String annotation;
	
	String attribute;
	Long value;
	Double percentage;	
	
	
	public String getAnnotation() {
		return annotation;
	}
	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}
	public String getAttribute() {
		return attribute;
	}
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}	
	
	public Long getValue() {
		return value;
	}
	public void setValue(Long value) {
		this.value = value;
	}
	public Double getPercentage() {
		return percentage;
	}
	public void setPercentage(Double percentage) {
		this.percentage = percentage;
	}
	@Override
	public int compareTo(AttributeValueChartData arg0) {
		return (this.getValue() < arg0.getValue() ? 1 : (this.getValue() == arg0.getValue() ? 0 : -1));
		
	}
	
	
	
	
	

}
