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

import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.usergrid.utils.JsonUtils;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ApiListing {
	String basePath;
	String swaggerVersion;
	String apiVersion;
	List<Api> apis;
	Map<String, Map<String, Object>> models;

	public ApiListing() {
	}

	@JsonSerialize(include = NON_NULL)
	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	@JsonSerialize(include = NON_NULL)
	public String getSwaggerVersion() {
		return swaggerVersion;
	}

	public void setSwaggerVersion(String swaggerVersion) {
		this.swaggerVersion = swaggerVersion;
	}

	@JsonSerialize(include = NON_NULL)
	public String getApiVersion() {
		return apiVersion;
	}

	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}

	@JsonSerialize(include = NON_NULL)
	public List<Api> getApis() {
		return apis;
	}

	public void setApis(List<Api> apis) {
		this.apis = apis;
	}

	@JsonSerialize(include = NON_NULL)
	public Map<String, Map<String, Object>> getModels() {
		return models;
	}

	public void setModels(Map<String, Map<String, Object>> models) {
		this.models = models;
	}

	@Override
	public String toString() {
		return JsonUtils.mapToJsonString(this);
	}

	public Document createWADLApplication() {
		DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = dbfac.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
		}
		DOMImplementation domImpl = docBuilder.getDOMImplementation();
		Document doc = domImpl.createDocument(
				"http://wadl.dev.java.net/2009/02", "application", null);
		Element application = doc.getDocumentElement();

		// add additional namespace to the root element
		application.setAttributeNS("http://www.w3.org/2000/xmlns/",
				"xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		application.setAttributeNS("http://www.w3.org/2000/xmlns/",
				"xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
		application.setAttributeNS("http://www.w3.org/2000/xmlns/",
				"xmlns:apigee", "http://api.apigee.com/wadl/2010/07/");
		application
				.setAttributeNS(
						"http://www.w3.org/2001/XMLSchema-instance",
						"xsi:schemaLocation",
						"http://wadl.dev.java.net/2009/02 http://apigee.com/schemas/wadl-schema.xsd http://api.apigee.com/wadl/2010/07/ http://apigee.com/schemas/apigee-wadl-extensions.xsd");

		if (apis != null) {
			Element resources = doc.createElement("resources");
			if (basePath != null) {
				resources.setAttribute("base", basePath);
			} else {
				resources.setAttribute("base", "http://api.usergrid.com");
			}
			application.appendChild(resources);
			for (Api api : apis) {
				resources.appendChild(api.createWADLResource(doc, this));
			}
		}
		return doc;
	}

}
