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



public class NetworkRequestsErrorsChartData  implements Comparable<NetworkRequestsErrorsChartData>{ 

	String annotation;
	String attribute;
	Long requests;
	Long errors;
	Double requestPercentage;
	Double errorPercentage;


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

	public Long getRequests() {
		return requests;
	}
	public void setRequests(Long requests) {
		this.requests = requests;
	}
	public Long getErrors() {
		return errors;
	}
	public void setErrors(Long errors) {
		this.errors = errors;
	}
	public Double getRequestPercentage() {
		return requestPercentage;
	}
	public void setRequestPercentage(Double requestPercentage) {
		this.requestPercentage = requestPercentage;
	}
	public Double getErrorPercentage() {
		return errorPercentage;
	}
	public void setErrorPercentage(Double errorPercentage) {
		this.errorPercentage = errorPercentage;
	}

	@Override
	public int compareTo(NetworkRequestsErrorsChartData arg0) {
		return (this.getRequests() < arg0.getRequests() ? 1 : (this.getRequests() == arg0.getRequests() ? 0 : -1));

	}

}
