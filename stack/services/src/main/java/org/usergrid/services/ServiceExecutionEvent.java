/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Stack.
 * 
 * Usergrid Stack is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * Usergrid Stack is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Affero General Public License along
 * with Usergrid Stack. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU AGPL version 3 section 7
 * 
 * Linking Usergrid Stack statically or dynamically with other modules is making
 * a combined work based on Usergrid Stack. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 * 
 * In addition, as a special exception, the copyright holders of Usergrid Stack
 * give you permission to combine Usergrid Stack with free software programs or
 * libraries that are released under the GNU LGPL and with independent modules
 * that communicate with Usergrid Stack solely through:
 * 
 *   - Classes implementing the org.usergrid.services.Service interface
 *   - Apache Shiro Realms and Filters
 *   - Servlet Filters and JAX-RS/Jersey Filters
 * 
 * You may copy and distribute such a system following the terms of the GNU AGPL
 * for Usergrid Stack and the licenses of the other code concerned, provided that
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
