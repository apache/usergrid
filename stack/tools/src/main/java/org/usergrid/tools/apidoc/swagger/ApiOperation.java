package org.usergrid.tools.apidoc.swagger;

import static org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion.NON_NULL;

import java.util.List;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.usergrid.utils.JsonUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ApiOperation {
	String httpMethod;
	String nickname;
	String summary;
	String notes;
	String responseTypeInternal;
	String responseClass;
	String tags;
	List<ApiParam> parameters;
	List<ApiOperationError> errorResponses;

	public ApiOperation() {
	}

	@JsonSerialize(include = NON_NULL)
	public String getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}

	@JsonSerialize(include = NON_NULL)
	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	@JsonSerialize(include = NON_NULL)
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	@JsonSerialize(include = NON_NULL)
	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	@JsonSerialize(include = NON_NULL)
	public String getResponseTypeInternal() {
		return responseTypeInternal;
	}

	public void setResponseTypeInternal(String responseTypeInternal) {
		this.responseTypeInternal = responseTypeInternal;
	}

	@JsonSerialize(include = NON_NULL)
	public String getResponseClass() {
		return responseClass;
	}

	public void setResponseClass(String responseClass) {
		this.responseClass = responseClass;
	}

	@JsonSerialize(include = NON_NULL)
	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	@JsonSerialize(include = NON_NULL)
	public List<ApiParam> getParameters() {
		return parameters;
	}

	public void setParameters(List<ApiParam> parameters) {
		this.parameters = parameters;
	}

	@JsonSerialize(include = NON_NULL)
	public List<ApiOperationError> getErrorResponses() {
		return errorResponses;
	}

	public void setErrorResponses(List<ApiOperationError> errorResponses) {
		this.errorResponses = errorResponses;
	}

	@Override
	public String toString() {
		return JsonUtils.mapToJsonString(this);
	}

	public Element createWADLMethod(Document doc, Api api) {
		Element method = doc.createElement("method");
		if (httpMethod != null) {
			method.setAttribute("name", httpMethod);
		}
		if (summary != null) {
			method.setAttributeNS("http://api.apigee.com/wadl/2010/07/",
					"apigee:displayName", summary);
		}
		if (nickname != null) {
			method.setAttribute("id", nickname);
		}

		Element tags = doc.createElementNS(
				"http://api.apigee.com/wadl/2010/07/", "tags");
		Element tag = doc.createElementNS(
				"http://api.apigee.com/wadl/2010/07/", "tag");
		tag.setAttribute("primary", "true");
		tags.appendChild(tag);
		tag.setTextContent(api.description != null ? api.description
				: "Objects");
		method.appendChild(tags);

		Element authentication = doc.createElementNS(
				"http://api.apigee.com/wadl/2010/07/", "authentication");
		authentication.setAttribute("required", "false");
		method.appendChild(authentication);

		Element example = doc.createElementNS(
				"http://api.apigee.com/wadl/2010/07/", "example");
		example.setAttribute("url", api.path);
		method.appendChild(example);

		if (notes != null) {
			Element d = doc.createElement("doc");
			d.setTextContent(notes);
			method.appendChild(d);
		}
		if ((parameters != null) && !parameters.isEmpty()) {
			Element request = doc.createElement("request");
			method.appendChild(request);
			boolean isForm = false;
			boolean isJson = false;
			for (ApiParam param : parameters) {
				if ("post".equalsIgnoreCase(param.getParamType())) {
					isForm = true;
				} else if ("body".equalsIgnoreCase(param.getParamType())) {
					isJson = true;
				}
			}

			for (ApiParam param : parameters) {
				if (!"post".equalsIgnoreCase(param.getParamType())
						&& !"body".equalsIgnoreCase(param.getParamType())) {
					request.appendChild(param.createWADLParam(doc, this));
				}
			}

			if (isForm) {
				Element contentType = doc.createElement("param");
				contentType.setAttribute("name", "Content-Type");
				contentType.setAttribute("type", "string");
				contentType.setAttribute("style", "header");
				contentType.setAttribute("required", "true");
				contentType.setAttribute("default",
						"application/x-www-form-urlencoded");
				request.appendChild(contentType);

				Element representation = doc.createElement("representation");
				representation.setAttribute("mediaType",
						"application/x-www-form-urlencoded");
				request.appendChild(representation);
				for (ApiParam param : parameters) {
					if ("post".equalsIgnoreCase(param.getParamType())) {
						representation.appendChild(param.createWADLParam(doc,
								this));
					}
				}

			} else if (isJson) {
				Element contentType = doc.createElement("param");
				contentType.setAttribute("name", "Content-Type");
				contentType.setAttribute("type", "string");
				contentType.setAttribute("style", "header");
				contentType.setAttribute("required", "true");
				contentType.setAttribute("default", "application/json");
				request.appendChild(contentType);

				Element representation = doc.createElement("representation");
				representation.setAttribute("mediaType", "application/json");
				request.appendChild(representation);
				Element payload = doc.createElementNS(
						"http://api.apigee.com/wadl/2010/07/", "payload");
				representation.appendChild(payload);
				payload.setTextContent("{ }");

			}
		}

		Element response = doc.createElement("response");
		method.appendChild(response);
		Element representation = doc.createElement("representation");
		representation.setAttribute("mediaType", "application/json");
		response.appendChild(representation);

		return method;
	}

}
