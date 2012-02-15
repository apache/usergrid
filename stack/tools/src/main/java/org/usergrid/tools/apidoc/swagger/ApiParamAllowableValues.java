package org.usergrid.tools.apidoc.swagger;

import static org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion.NON_NULL;

import java.util.List;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.usergrid.utils.JsonUtils;

public class ApiParamAllowableValues {
	List<String> values;
	String valueType;

	public ApiParamAllowableValues() {
	}

	@JsonSerialize(include = NON_NULL)
	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}

	@JsonSerialize(include = NON_NULL)
	public String getValueType() {
		return valueType;
	}

	public void setValueType(String valueType) {
		this.valueType = valueType;
	}

	@Override
	public String toString() {
		return JsonUtils.mapToJsonString(this);
	}

}
