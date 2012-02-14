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
