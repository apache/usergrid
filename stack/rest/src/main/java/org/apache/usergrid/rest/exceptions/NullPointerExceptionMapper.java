package org.apache.usergrid.rest.exceptions;


import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;


/**
 * Wraps null pointer exceptions to only return 500's.
 */
public class NullPointerExceptionMapper extends AbstractExceptionMapper<NullPointerException> {
    private static final Logger logger = LoggerFactory.getLogger( NullPointerExceptionMapper.class );

    @Override
    public Response toResponse( NullPointerException e ) {

        logger.error( "Illegal argument was passed, returning bad request to user", e );

        return toResponse( INTERNAL_SERVER_ERROR, e );
    }
}
