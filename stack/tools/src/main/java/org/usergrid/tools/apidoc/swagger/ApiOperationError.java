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
package org.usergrid.tools.apidoc.swagger;

import static org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion.NON_NULL;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.usergrid.utils.JsonUtils;

public class ApiOperationError {
	String reason;
	Integer code;

	public ApiOperationError() {
	}

	@JsonSerialize(include = NON_NULL)
	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	@JsonSerialize(include = NON_NULL)
	public Integer getCode() {
		return code;
	}

	public void setCode(Integer code) {
		this.code = code;
	}

	@Override
	public String toString() {
		return JsonUtils.mapToJsonString(this);
	}
}
