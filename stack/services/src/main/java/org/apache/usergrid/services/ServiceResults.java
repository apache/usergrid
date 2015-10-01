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


import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.AggregateCounterSet;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;


public class ServiceResults extends Results {

    private static final Logger logger = LoggerFactory.getLogger( ServiceResults.class );


    public enum Type {
        GENERIC, COLLECTION, CONNECTION, COUNTERS
    }


    private final Service service;
    private final ServiceRequest request;
    private final Map<String, Object> serviceMetadata;

    private final List<ServiceRequest> nextRequests;

    private final String path;

    private final String childPath;

    private final Type resultsType;

    private final ServiceResults previousResults;


    public ServiceResults( Service service, ServiceRequest request, ServiceResults previousResults, String childPath,
                           Type resultsType, Results r, Map<String, Object> serviceMetadata,
                           List<ServiceRequest> nextRequests ) {
        super( r );
        this.service = service;
        this.request = request;
        this.previousResults = previousResults;
        this.childPath = childPath;
        this.resultsType = resultsType;
        if ( request != null ) {
            path = request.getPath();
        }
        else {
            path = null;
        }
        this.serviceMetadata = serviceMetadata;
        this.nextRequests = nextRequests;
        logger.debug( "Child path: {}", childPath );
    }


    public ServiceResults( Service service, ServiceContext context, Type resultsType, Results r,
                           Map<String, Object> serviceMetadata, List<ServiceRequest> nextRequests ) {
        super( r );
        this.service = service;
        request = context.getRequest();
        previousResults = context.getPreviousResults();
        childPath = context.getRequest().getChildPath();
        this.resultsType = resultsType;
        if ( request != null ) {
            path = request.getPath();
        }
        else {
            path = null;
        }
        this.serviceMetadata = serviceMetadata;
        this.nextRequests = nextRequests;
        logger.debug( "Child path: {}", childPath );
    }


    public static ServiceResults genericServiceResults() {
        return new ServiceResults( null, null, null, null, Type.GENERIC, null, null, null );
    }


    public static ServiceResults genericServiceResults( Results r ) {
        return new ServiceResults( null, null, null, null, Type.GENERIC, r, null, null );
    }


    public static ServiceResults simpleServiceResults( Type resultsType ) {
        return new ServiceResults( null, null, null, null, resultsType, null, null, null );
    }


    public static ServiceResults simpleServiceResults( Type resultsType, Results r ) {
        return new ServiceResults( null, null, null, null, resultsType, r, null, null );
    }


    public Service getService() {
        return service;
    }


    public ServiceRequest getRequest() {
        return request;
    }


    public Map<String, Object> getServiceMetadata() {
        return serviceMetadata;
    }



    public String getPath() {
        return path;
    }


    public List<ServiceRequest> getNextRequests() {
        return nextRequests;
    }


    public boolean hasMoreRequests() {
        return ( nextRequests != null ) && ( nextRequests.size() > 0 );
    }


    public String getChildPath() {
        return childPath;
    }


    public Type getResultsType() {
        return resultsType;
    }


    public void setChildResults( ServiceResults childResults ) {
        setChildResults( childResults.getResultsType(), childResults.getRequest().getOwner().getUuid(),
                childResults.getChildPath(), childResults.getEntities() );
    }


    public void setChildResults( Type rtype, UUID id, String name, List<Entity> results ) {
        if ( ( results == null ) || ( results.size() == 0 ) ) {
            return;
        }
        if ( rtype == Type.GENERIC ) {
            return;
        }
        List<Entity> entities = getEntities();
        if ( entities != null ) {
            for ( Entity entity : entities ) {
                if ( entity.getUuid().equals( id ) ) {
                    if ( rtype == Type.COLLECTION ) {
                        entity.setCollections( name, results );
                    }
                    else if ( rtype == Type.CONNECTION ) {
                        entity.setConnections( name, results );
                    }
                }
            }
        }
    }


    @Override
    public ServiceResults withQuery( Query query ) {
        return ( ServiceResults ) super.withQuery( query );
    }


    @Override
    public ServiceResults withIds( List<UUID> resultsIds ) {
        return ( ServiceResults ) super.withIds( resultsIds );
    }


    @Override
    public ServiceResults withRefs( List<EntityRef> resultsRefs ) {
        return ( ServiceResults ) super.withRefs( resultsRefs );
    }


    @Override
    public ServiceResults withRef( EntityRef ref ) {
        return ( ServiceResults ) super.withRef( ref );
    }


    @Override
    public ServiceResults withEntity( Entity resultEntity ) {
        return ( ServiceResults ) super.withEntity( resultEntity );
    }


    @Override
    public ServiceResults withEntities( List<? extends Entity> resultsEntities ) {
        return ( ServiceResults ) super.withEntities( resultsEntities );
    }


    @Override
    public ServiceResults withDataName( String dataName ) {
        return ( ServiceResults ) super.withDataName( dataName );
    }


    @Override
    public ServiceResults withCounters( List<AggregateCounterSet> counters ) {
        return ( ServiceResults ) super.withCounters( counters );
    }


    @Override
    public ServiceResults withNextResult( UUID nextResult ) {
        return ( ServiceResults ) super.withNextResult( nextResult );
    }


    @Override
    public ServiceResults withCursor( String cursor ) {
        return ( ServiceResults ) super.withCursor( cursor );
    }


    @Override
    public ServiceResults withMetadata( UUID id, String name, Object value ) {
        return ( ServiceResults ) super.withMetadata( id, name, value );
    }


    @Override
    public ServiceResults withMetadata( UUID id, Map<String, Object> data ) {
        return ( ServiceResults ) super.withMetadata( id, data );
    }


    @Override
    public ServiceResults withMetadata( Map<UUID, Map<String, Object>> metadata ) {
        return ( ServiceResults ) super.withMetadata( metadata );
    }


    @Override
    public ServiceResults withData( Object data ) {
        return ( ServiceResults ) super.withData( data );
    }
}
