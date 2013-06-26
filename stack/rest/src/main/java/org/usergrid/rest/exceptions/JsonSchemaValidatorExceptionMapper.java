package org.usergrid.rest.exceptions;

import static org.usergrid.utils.JsonUtils.mapToJsonString;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.usergrid.persistence.exceptions.JsonSchemaValidatorException;
import org.usergrid.rest.ApiResponse;

@Provider
public class JsonSchemaValidatorExceptionMapper extends
        AbstractExceptionMapper<JsonSchemaValidatorException> {

    public Response toResponse(int status, JsonSchemaValidatorException e) {
        logger.error("Error in request (" + status + ")", e);
        ApiResponse response = new ApiResponse();
        AuthErrorInfo authError = AuthErrorInfo.getForException(e);
        if (authError != null) {
            response.setError(authError.getType(), authError.getMessage(), e);
        } else {
            response.setError(e);
        }
        response.setValidation(e.getProcessingReport());
        String jsonResponse = mapToJsonString(response);
        return toResponse(status, jsonResponse);
    }

}
