/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.services;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.services.ServiceParameter.QueryParameter;
import org.apache.usergrid.services.ServiceResults.Type;

import org.apache.shiro.SecurityUtils;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.usergrid.utils.ListUtils.isEmpty;
import static org.apache.usergrid.utils.ListUtils.last;


public class ServiceRequest {

    private static final Logger logger = LoggerFactory.getLogger( ServiceRequest.class );

    public static long count = 0;

    private final long id = count++;

    private final ServiceManager services;

    private final ServiceAction action;
    private final ServiceRequest parent;
    private final EntityRef owner;
    private final String serviceName;
    private final String path;
    private final List<ServiceParameter> parameters;
    private final String childPath;
    private final boolean returnsTree;
    private final boolean returnsInboundConnections;
    private final boolean returnsOutboundConnections;
    private final ServicePayload payload;
    private final List<ServiceParameter> originalParameters;

    // return results_set, result_entity, new_service, param_list, properties


    public ServiceRequest( ServiceManager services, ServiceAction action, String serviceName,
                           List<ServiceParameter> parameters, ServicePayload payload, boolean returnsTree,
                           boolean returnsInboundConnections, boolean returnsOutboundConnections ) {
        this.services = services;
        this.action = action;
        parent = null;
        owner = services.getApplicationRef();
        childPath = null;
        this.serviceName = serviceName;
        path = "/" + serviceName;
        this.parameters = parameters;
        this.originalParameters = Collections.unmodifiableList( new ArrayList<ServiceParameter>( parameters ) );
        this.returnsTree = returnsTree;
        this.returnsInboundConnections = returnsInboundConnections;
        this.returnsOutboundConnections = returnsOutboundConnections;
        if ( payload == null ) {
            payload = new ServicePayload();
        }

        this.payload = payload;
    }

    public ServiceRequest( ServiceManager services, ServiceAction action, String serviceName,
                           List<ServiceParameter> parameters, ServicePayload payload, boolean returnsTree) {
        this( services, action, serviceName, parameters, payload, returnsTree, true, true);
    }


    public ServiceRequest( ServiceManager services, ServiceAction action, String serviceName,
                           List<ServiceParameter> parameters, ServicePayload payload ) {
        this( services, action, serviceName, parameters, payload, false, true, true );
    }


    public ServiceRequest( ServiceRequest parent, EntityRef owner, String path, String childPath, String serviceName,
                           List<ServiceParameter> parameters ) {
        this.services = parent.services;
        this.returnsTree = parent.returnsTree;
        this.returnsInboundConnections = parent.returnsInboundConnections;
        this.returnsOutboundConnections = parent.returnsOutboundConnections;
        this.action = parent.action;
        this.payload = parent.payload;
        this.parent = parent;
        this.owner = owner;
        this.serviceName = serviceName;
        if ( parameters == null ) {
            parameters = new ArrayList<ServiceParameter>();
        }
        this.parameters = parameters;
        this.originalParameters = Collections.unmodifiableList( new ArrayList<ServiceParameter>( parameters ) );
        this.path = path;
        this.childPath = childPath;
    }


    public ServiceRequest( ServiceManager services, ServiceAction action, ServiceRequest parent, EntityRef owner,
                           String path, String childPath, String serviceName, List<ServiceParameter> parameters,
                           ServicePayload payload, boolean returnsTree, boolean returnsInboundConnections,
                           boolean returnsOutboundConnections ) {
        this.services = services;
        this.action = action;
        this.parent = parent;
        this.owner = owner;
        this.serviceName = serviceName;
        this.path = path;
        this.parameters = parameters;
        this.originalParameters = Collections.unmodifiableList( new ArrayList<ServiceParameter>( parameters ) );
        this.childPath = childPath;
        this.returnsTree = returnsTree;
        this.returnsInboundConnections = returnsInboundConnections;
        this.returnsOutboundConnections = returnsOutboundConnections;
        this.payload = payload;
    }

    public ServiceRequest( ServiceManager services, ServiceAction action, ServiceRequest parent, EntityRef owner,
                           String path, String childPath, String serviceName, List<ServiceParameter> parameters,
                           ServicePayload payload, boolean returnsTree ) {
        this(services, action, parent, owner, path, childPath, serviceName, parameters, payload, returnsTree, true, true);
    }


    public static ServiceRequest withPath( ServiceRequest r, String path ) {
        return new ServiceRequest( r.services, r.action, r.parent, r.owner, path, r.childPath, r.serviceName,
                r.parameters, r.payload, r.returnsTree, r.returnsInboundConnections, r.returnsOutboundConnections );
    }


    public static ServiceRequest withChildPath( ServiceRequest r, String childPath ) {
        return new ServiceRequest( r.services, r.action, r.parent, r.owner, r.path, childPath, r.serviceName,
                r.parameters, r.payload, r.returnsTree, r.returnsInboundConnections, r.returnsOutboundConnections );
    }


    public ServiceRequest withPath( String path ) {
        return withPath( this, path );
    }


    public ServiceRequest withChildPath( String childPath ) { return withChildPath( this, childPath ); }


    public long getId() {
        return id;
    }


    public String getPath() {
        return path;
    }


    public ServiceAction getAction() {
        return action;
    }


    public ServicePayload getPayload() {
        return payload;
    }


    public ServiceManager getServices() {
        return services;
    }


    public ServiceRequest getParent() {
        return parent;
    }


    public String getServiceName() {
        return serviceName;
    }


    public EntityRef getPreviousOwner() {
        if ( parent == null ) {
            return null;
        }
        return parent.getOwner();
    }


    public ServiceResults execute() throws Exception {
        try {
            return execute( null );
        }
        catch ( Exception e ) {
            // don't log as error because some exceptions are not actually errors, e.g. resource not found
            logger.debug( debugString(), e );
            throw e;
        }
    }


    private String debugString() {
        StringBuffer sb = new StringBuffer();
        sb.append( "request details:\n  " );
        sb.append( action );
        sb.append( " " );
        sb.append( this );
        sb.append( "\n  payload: " );
        sb.append( payload );
        sb.append( "\n  owner: " );
        sb.append( owner );
        sb.append( "\n  principal: " );
        sb.append( SecurityUtils.getSubject().getPrincipal() );
        return sb.toString();
    }


    public ServiceResults execute( ServiceResults previousResults ) throws Exception {

        // initServiceName();

        ServiceResults results = null;
        Service s = services.getService( serviceName );
        if ( s != null ) {
            results = s.invoke( action, this, previousResults, payload );
            if ( ( results != null ) && results.hasMoreRequests() ) {

                results = invokeMultiple( results );
            }
        }

        if ( results == null ) {
            results = new ServiceResults( null, this, previousResults, null, Type.GENERIC, null, null, null );
        }

        return results;
    }


    private ServiceResults invokeMultiple( ServiceResults previousResults ) throws Exception {

        List<ServiceRequest> requests = previousResults.getNextRequests();

        if ( returnsTree ) {

            for ( ServiceRequest request : requests ) {

                ServiceResults rs = request.execute( previousResults );
                if ( rs != null ) {
                    previousResults.setChildResults( rs );
                }
            }

            return previousResults;
        }
        else {
            ServiceResults aggregate_results = null;

            for ( ServiceRequest request : requests ) {

                ServiceResults rs = request.execute( previousResults );
                if ( rs != null ) {
                    if ( aggregate_results == null ) {
                        aggregate_results = rs;
                    }
                    else {
                        aggregate_results.merge( rs );
                    }
                }
            }

            return aggregate_results;
        }
    }


    public List<ServiceParameter> getParameters() {
        return parameters;
    }


    public boolean hasParameters() {
        return !isEmpty( parameters );
    }


    public EntityRef getOwner() {
        return owner;
    }


    public Query getLastQuery() {
        if ( !isEmpty( parameters ) ) {
            return last( parameters ).getQuery();
        }
        return null;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if ( serviceName != null ) {
            sb.append( "/" );
            sb.append( serviceName );
        }
        for ( int i = 0; i < parameters.size(); i++ ) {
            ServiceParameter p = parameters.get( i );
            if ( p instanceof QueryParameter ) {
                if ( i == ( parameters.size() - 1 ) ) {
                    sb.append( '?' );
                }
                else {
                    sb.append( ';' );
                }
                boolean has_prev_param = false;
                String q = p.toString();
                if ( isNotBlank( q ) ) {
                    try {
                        sb.append("ql=").append(URLEncoder.encode(q, "UTF-8"));
                    }
                    catch ( UnsupportedEncodingException e ) {
                        logger.error( "Unable to encode url", e );
                    }
                    has_prev_param = true;
                }
                int limit = p.getQuery().getLimit();
                if ( limit != Query.DEFAULT_LIMIT ) {
                    if ( has_prev_param ) {
                        sb.append( '&' );
                    }
                    sb.append("limit=").append(limit);
                    has_prev_param = true;
                }
                if ( p.getQuery().getStartResult() != null ) {
                    if ( has_prev_param ) {
                        sb.append( '&' );
                    }
                    sb.append("start=").append(p.getQuery().getStartResult());
                    has_prev_param = true;
                }
            }
            else {
                sb.append( '/' );
                sb.append( p.toString() );
            }
        }
        return sb.toString();
    }


    public String getChildPath() {
        return childPath;
    }


    public boolean isReturnsTree() { return returnsTree; }

    public boolean isReturnsInboundConnections() { return returnsInboundConnections; }

    public boolean isReturnsOutboundConnections() { return returnsOutboundConnections; }

    public List<ServiceParameter> getOriginalParameters() {
        return originalParameters;
    }
}
