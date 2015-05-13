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
package org.apache.usergrid.services.applications;


import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.services.AbstractService;
import org.apache.usergrid.services.ServiceContext;
import org.apache.usergrid.services.ServiceParameter.QueryParameter;
import org.apache.usergrid.services.ServicePayload;
import org.apache.usergrid.services.ServiceResults;
import org.apache.usergrid.services.ServiceResults.Type;

import org.apache.commons.lang.StringUtils;

import static org.apache.usergrid.services.ServiceResults.genericServiceResults;
import static org.apache.usergrid.services.ServiceResults.simpleServiceResults;
import static org.apache.usergrid.utils.MapUtils.hashMap;


public class ApplicationsService extends AbstractService {

    private static final Logger logger = LoggerFactory.getLogger( ApplicationsService.class );


    public ApplicationsService() {
        super();
        logger.debug( "/applications" );
        declareEntityDictionary( "counters" );
        declareEntityCommand( "hello" );
        declareEntityCommand( "resetroles" );
    }


    @Override
    public ServiceResults invoke( ServiceContext context ) throws Exception {

        ServiceResults results = null;

        String metadataType = checkForServiceMetadata( context );
        if ( metadataType != null ) {
            return handleServiceMetadata( context, metadataType );
        }

        EntityDictionaryEntry entityDictionary = checkForEntityDictionaries( context );
        String entityCommand = checkForEntityCommands( context );

        results = invokeItemWithId( context, sm.getApplicationId() );
        context.dequeueParameter();

        results = handleEntityDictionary( context, results, entityDictionary );
        results = handleEntityCommand( context, results, entityCommand );

        return results;
    }


    @Override
    public ServiceResults getItemById( ServiceContext context, UUID id ) throws Exception {
        return getApplicationEntity( context );
    }


    @Override
    public ServiceResults putItemById( ServiceContext context, UUID id ) throws Exception {
        return updateApplicationEntity( context, context.getPayload() );
    }


    @Override
    public ServiceResults getEntityDictionary( ServiceContext context, List<EntityRef> refs,
                                               EntityDictionaryEntry dictionary ) throws Exception {

        if ( "counters".equalsIgnoreCase( dictionary.getName() ) ) {
            checkPermissionsForPath( context, "/counters" );

            if ( context.parameterCount() == 0 ) {
                return getApplicationCounterNames();
            }
            else if ( context.parameterCount() > 0 ) {
                if ( context.getParameters().get( 0 ) instanceof QueryParameter ) {
                    return getApplicationCounters( context.getParameters().get( 0 ).getQuery() );
                }
            }
        }

        return super.getEntityDictionary( context, refs, dictionary );
    }


    public ServiceResults getApplicationEntity( ServiceContext context ) throws Exception {

        checkPermissionsForPath( context, "/" );

        Entity entity = em.get( em.getApplicationRef() );
        Results r = Results.fromEntity( entity );

        Map<String, Object> collections = em.getApplicationCollectionMetadata();
        // Set<String> collections = em.getApplicationCollections();
        if ( collections.size() > 0 ) {
            r.setMetadata( em.getApplicationRef().getUuid(), "collections", collections );
        }

        return genericServiceResults( r );
    }


    public ServiceResults updateApplicationEntity( ServiceContext context, ServicePayload payload ) throws Exception {

        checkPermissionsForPath( context, "/" );

        Map<String, Object> properties = payload.getProperties();
        Object m = properties.get( "metadata" );
        if ( m instanceof Map ) {
            @SuppressWarnings("unchecked") Map<String, Object> metadata = ( Map<String, Object> ) m;
            Object c = metadata.get( "collections" );
            if ( c instanceof Map ) {
                @SuppressWarnings("unchecked") Map<String, Object> collections = ( Map<String, Object> ) c;
                for ( String collection : collections.keySet() ) {
                    if ( isReservedCollection( collection ) ) {
                        continue;
                    }

                    em.createApplicationCollection( collection );
                    logger.debug( "Created collection " + collection + " for application " + sm.getApplicationId() );
                }
            }
        }

        Entity entity = em.get( em.getApplicationRef() );
        em.updateProperties( entity, properties );
        entity.addProperties( properties );
        Results r = Results.fromEntity( entity );

        Set<String> collections = em.getApplicationCollections();
        if ( collections.size() > 0 ) {
            r.setMetadata( em.getApplicationRef().getUuid(), "collections", collections );
        }

        return genericServiceResults( r );
    }


    private boolean isReservedCollection( String collection ) {
        return StringUtils.equalsIgnoreCase("applications", collection) || StringUtils
                .equalsIgnoreCase("application", collection);

    }


    public ServiceResults getApplicationCounterNames() throws Exception {
        Set<String> counters = em.getCounterNames();
        ServiceResults results = genericServiceResults().withData( counters );
        return results;
    }


    public ServiceResults getApplicationCounters( Query query ) throws Exception {

        Results counters = em.getAggregateCounters( query );
        ServiceResults results = simpleServiceResults( Type.COUNTERS );
        if ( counters != null ) {
            results.withCounters( counters.getCounters() );
        }
        return results;
    }


    @Override
    public ServiceResults getEntityCommand( ServiceContext context, List<EntityRef> refs, String command )
            throws Exception {
        if ( "hello".equalsIgnoreCase( command ) ) {
            ServiceResults results = genericServiceResults().withData( hashMap( "say", "Hello!" ) );
            return results;
        }
        return super.getEntityCommand( context, refs, command );
    }


    @Override
    public ServiceResults postEntityCommand( ServiceContext context, List<EntityRef> refs, String command,
                                             ServicePayload payload ) throws Exception {
        if ( "resetroles".equalsIgnoreCase( command ) ) {
            em.resetRoles();
            //          TODO TN finish this  return getApplicationRoles();
        }
        return super.postEntityCommand( context, refs, command, payload );
    }
}
