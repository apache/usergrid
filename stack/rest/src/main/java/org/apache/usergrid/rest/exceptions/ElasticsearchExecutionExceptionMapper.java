package org.apache.usergrid.rest.exceptions;


import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.elasticsearch.action.search.SearchPhaseExecutionException;

import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;



@Provider
public class ElasticsearchExecutionExceptionMapper
    extends AbstractExceptionMapper<SearchPhaseExecutionException>  {

    @Override
    public Response toResponse( SearchPhaseExecutionException spee ){
        return toResponse( SERVICE_UNAVAILABLE, spee );

    }
}
