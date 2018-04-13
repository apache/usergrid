package org.apache.usergrid.rest.exceptions;


import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import org.apache.usergrid.persistence.index.exceptions.TooManyDirectEntitiesException;

@Provider
public class TooManyDirectEntitiesExceptionMapper extends AbstractExceptionMapper<TooManyDirectEntitiesException> {
    @Override
    public Response toResponse( final TooManyDirectEntitiesException e ) {
        return toResponse( BAD_REQUEST, e );
    }
}
