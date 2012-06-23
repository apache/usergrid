package org.usergrid.rest.exceptions;

import org.usergrid.rest.ApiResponse;

import javax.ws.rs.core.UriInfo;

import static org.usergrid.utils.JsonUtils.mapToJsonString;

/**
 * @author zznate
 */
public class OrganizationApplicationNotFoundException extends RuntimeException {
    private ApiResponse apiResponse;

  	public OrganizationApplicationNotFoundException(String orgAppName, UriInfo uriInfo) {
      super("Could not find application for " + orgAppName + " from URI: " + uriInfo.getPath());
      apiResponse = new ApiResponse(uriInfo);

      apiResponse.setError(this);
  	}


  	public ApiResponse getApiResponse() {
  		return apiResponse;
  	}

  	public String getJsonResponse() {
  		return mapToJsonString(apiResponse);
  	}

}
