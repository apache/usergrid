package org.apache.usergrid.rest.exceptions;


import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.usergrid.services.exceptions.UnsupportedServiceOperationException;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;


/**
 * Created by ApigeeCorporation on 1/8/16.
 */
@Provider
public class UnsupportedServiceOperationExceptionMapper extends AbstractExceptionMapper<UnsupportedServiceOperationException> {

    @Override
    public Response toResponse( UnsupportedServiceOperationException e ) {
        return toResponse( METHOD_NOT_ALLOWED, e );
    }
}
