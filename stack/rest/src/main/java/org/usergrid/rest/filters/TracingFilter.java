package org.usergrid.rest.filters;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.usergrid.persistence.cassandra.util.TraceTag;
import org.usergrid.persistence.cassandra.util.TraceTagManager;
import org.usergrid.persistence.cassandra.util.TraceTagReporter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

/**
 * Attach and detach trace tags at start and end of request scopes
 *
 * @author zznate
 */
@Component
public class TracingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private Logger logger = LoggerFactory.getLogger(TracingFilter.class);

    @Autowired
    private TraceTagManager traceTagManager;
    @Autowired
    private TraceTagReporter traceTagReporter;


    @Context
    private HttpServletRequest httpServletRequest;

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        // TODO add 'trace=true' parameter check?
        TraceTag traceTag = traceTagManager.create("TRACE-".concat(request.getPath(true)));
        traceTagManager.attach(traceTag);
        return request;
    }

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        if ( traceTagManager.isActive() ) {
            TraceTag traceTag = traceTagManager.detach();
            traceTagReporter.report(traceTag);
        }
        return response;
    }
}
