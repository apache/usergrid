package org.usergrid.tools.apidoc.swagger;

import static org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion.NON_NULL;

import java.util.List;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.usergrid.utils.JsonUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Api {
	String path;
	String description;
	List<ApiOperation> operations;

	public Api() {
	}

	@JsonSerialize(include = NON_NULL)
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@JsonSerialize(include = NON_NULL)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@JsonSerialize(include = NON_NULL)
	public List<ApiOperation> getOperations() {
		return operations;
	}

	public void setOperations(List<ApiOperation> operations) {
		this.operations = operations;
	}

	@Override
	public String toString() {
		return JsonUtils.mapToJsonString(this);
	}

	public Element createWADLResource(Document doc, ApiListing listing) {
		Element resource = doc.createElement("resource");
		if (path != null) {
			resource.setAttribute("path", path);
		}

		if ((operations != null) && !operations.isEmpty()) {
			for (ApiOperation operation : operations) {
				resource.appendChild(operation.createWADLMethod(doc, this));
			}
		}

		return resource;
	}
}
