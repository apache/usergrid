package org.usergrid.rest.exceptions;

import org.usergrid.rest.ApiResponse;
import org.usergrid.rest.ServerEnvironmentProperties;

import javax.ws.rs.core.UriInfo;

import static org.usergrid.utils.JsonUtils.mapToJsonString;

/**
 * @author zznate
 */
public class OrganizationApplicationNotFoundException extends RuntimeException {
    private ApiResponse apiResponse;

  	public OrganizationApplicationNotFoundException(String orgAppName, UriInfo uriInfo, ServerEnvironmentProperties properties) {
      super("Could not find application for " + orgAppName + " from URI: " + uriInfo.getPath());
      apiResponse = new ApiResponse(properties);

      apiResponse.setError(this);
  	}


  	public ApiResponse getApiResponse() {
  		return apiResponse;
  	}

  	public String getJsonResponse() {
  		return mapToJsonString(apiResponse);
  	}

}
