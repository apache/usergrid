package org.apache.usergrid.rest.exceptions;


import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;



@Provider
public class UnsupportedOperationExceptionMapper extends AbstractExceptionMapper<UnsupportedOperationException> {

    @Override
    public Response toResponse( UnsupportedOperationException e ) {
        return toResponse( INTERNAL_SERVER_ERROR, e );
    }
}
