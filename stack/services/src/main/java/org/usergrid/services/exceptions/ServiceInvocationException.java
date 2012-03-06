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
package org.usergrid.services.exceptions;

import org.usergrid.services.ServiceContext;
import org.usergrid.services.ServiceRequest;

public class ServiceInvocationException extends ServiceException {

	final ServiceRequest request;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ServiceInvocationException(ServiceContext context) {
		super();
		request = context.getRequest();
	}

	public ServiceInvocationException(ServiceContext context, String message,
			Throwable cause) {
		super(message, cause);
		request = context.getRequest();
	}

	public ServiceInvocationException(ServiceContext context, String message) {
		super(message);
		request = context.getRequest();
	}

	public ServiceInvocationException(ServiceContext context, Throwable cause) {
		super(cause);
		request = context.getRequest();
	}

	public ServiceInvocationException(ServiceRequest request) {
		super();
		this.request = request;
	}

	public ServiceInvocationException(ServiceRequest request, String message,
			Throwable cause) {
		super(message, cause);
		this.request = request;
	}

	public ServiceInvocationException(ServiceRequest request, String message) {
		super(message);
		this.request = request;
	}

	public ServiceInvocationException(ServiceRequest request, Throwable cause) {
		super(cause);
		this.request = request;
	}

	public ServiceRequest getServiceRequest() {
		return request;
	}
}
