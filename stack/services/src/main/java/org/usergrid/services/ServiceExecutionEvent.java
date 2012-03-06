/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.services;

public class ServiceExecutionEvent {

	ServiceAction action;
	ServiceRequest request;
	ServiceResults results;
	ServicePayload payload;

	public ServiceExecutionEvent() {
	}

	public ServiceExecutionEvent(ServiceAction action, ServiceRequest request,
			ServiceResults results, ServicePayload payload) {

		this.action = action;
		this.request = request;
		this.results = results;
		this.payload = payload;

	}

	public ServiceAction getAction() {
		return action;
	}

	public void setAction(ServiceAction action) {
		this.action = action;
	}

	public ServiceRequest getRequest() {
		return request;
	}

	public void setRequest(ServiceRequest request) {
		this.request = request;
	}

	public ServiceResults getResults() {
		return results;
	}

	public void setResults(ServiceResults results) {
		this.results = results;
	}

	public ServicePayload getPayload() {
		return payload;
	}

	public void setPayload(ServicePayload payload) {
		this.payload = payload;
	}
}
