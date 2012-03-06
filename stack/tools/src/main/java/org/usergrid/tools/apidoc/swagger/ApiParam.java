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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ApiParam {
	String name;
	String dataType;
	String description;
	String defaultValue;
	ApiParamAllowableValues allowableValues;
	Boolean required;
	String access;
	Boolean allowMultiple;
	String paramType;

	public ApiParam() {
	}

	@JsonSerialize(include = NON_NULL)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@JsonSerialize(include = NON_NULL)
	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	@JsonSerialize(include = NON_NULL)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@JsonSerialize(include = NON_NULL)
	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	@JsonSerialize(include = NON_NULL)
	public ApiParamAllowableValues getAllowableValues() {
		return allowableValues;
	}

	public void setAllowableValues(ApiParamAllowableValues allowableValues) {
		this.allowableValues = allowableValues;
	}

	@JsonSerialize(include = NON_NULL)
	public Boolean isRequired() {
		return required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}

	@JsonSerialize(include = NON_NULL)
	public String getAccess() {
		return access;
	}

	public void setAccess(String access) {
		this.access = access;
	}

	@JsonSerialize(include = NON_NULL)
	public Boolean isAllowMultiple() {
		return allowMultiple;
	}

	public void setAllowMultiple(Boolean allowMultiple) {
		this.allowMultiple = allowMultiple;
	}

	@JsonSerialize(include = NON_NULL)
	public String getParamType() {
		return paramType;
	}

	public void setParamType(String paramType) {
		this.paramType = paramType;
	}

	@Override
	public String toString() {
		return JsonUtils.mapToJsonString(this);
	}

	public Element createWADLParam(Document doc, ApiOperation operation) {
		Element param = doc.createElement("param");
		if (name != null) {
			param.setAttribute("name", name);
		}
		if ((required != null) && required) {
			param.setAttribute("required", required.toString());
		}
		if ((allowMultiple != null) && allowMultiple) {
			param.setAttribute("repeating", allowMultiple.toString());
		}
		if (dataType != null) {
			param.setAttribute("type", "xsd:" + dataType.toLowerCase());
		}
		if (defaultValue != null) {
			param.setAttribute("default", defaultValue);
		}
		if (description != null) {
			Element d = doc.createElement("doc");
			d.setTextContent(description);
			param.appendChild(d);
		}
		if (paramType != null) {
			param.setAttribute(
					"style",
					"post".equalsIgnoreCase(paramType) ? "query" : "path"
							.equalsIgnoreCase(paramType) ? "template"
							: paramType);
		}
		if ((allowableValues != null) && (allowableValues.values != null)
				&& (!allowableValues.values.isEmpty())) {
			for (String v : allowableValues.values) {
				Element option = doc.createElement("option");
				option.setAttribute("value", v);
				param.appendChild(option);
			}
		}
		return param;
	}

}
