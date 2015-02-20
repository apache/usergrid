/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.rest;

import com.google.common.base.Preconditions;
import com.sun.jersey.api.json.JSONWithPadding;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.security.annotations.RequireSystemAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Classy class class.
 */
@Component
@Scope( "singleton" )
@Produces( {
        MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript"
} )
public class IndexResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger(IndexResource.class);

    @RequireSystemAccess
    @PUT
    @Path( "rebuild" )
    public JSONWithPadding rebuildIndexes( @Context UriInfo ui,
                                           @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
            throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "rebuild indexes" );


        final EntityManagerFactory.ProgressObserver po = new EntityManagerFactory.ProgressObserver() {


            @Override
            public void onProgress( final EntityRef entity ) {
                logger.info( "Indexing entity {}:{} ", entity.getType(), entity.getUuid() );
            }


            @Override
            public long getWriteDelayTime() {
                return 0;
            }
        };


        final Thread rebuild = new Thread() {

            @Override
            public void run() {
                logger.info( "Rebuilding all indexes" );

                try {
                    emf.rebuildAllIndexes( po );
                }
                catch ( Exception e ) {
                    logger.error( "Unable to rebuild indexes", e );
                }
            }
        };

        rebuild.setName( "Index rebuild all usergrid" );
        rebuild.setDaemon( true );
        rebuild.start();


        response.setSuccess();

        return new JSONWithPadding( response, callback );
    }


    @RequireSystemAccess
    @PUT
    @Path( "rebuild/" + RootResource.APPLICATION_ID_PATH )
    public JSONWithPadding rebuildIndexes( @Context UriInfo ui, @PathParam( "applicationId" ) String applicationIdStr,
                                           @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback,
                                           @QueryParam( "delay" ) @DefaultValue( "10" ) final long delay )

            throws Exception {

        final UUID appId = UUIDUtils.tryExtractUUID(applicationIdStr);
        ApiResponse response = createApiResponse();
        response.setAction( "rebuild indexes" );


        final EntityManager em = emf.getEntityManager( appId );

        final Set<String> collectionNames = em.getApplicationCollections();

        final Thread rebuild = new Thread() {

            @Override
            public void run() {
                for ( String collectionName : collectionNames )


                {
                    rebuildCollection( appId, collectionName, delay );
                }
            }
        };

        rebuild.setName( String.format( "Index rebuild for app %s", appId ) );
        rebuild.setDaemon( true );
        rebuild.start();

        response.setSuccess();

        return new JSONWithPadding( response, callback );
    }


    @RequireSystemAccess
    @PUT
    @Path( "rebuild/" + RootResource.APPLICATION_ID_PATH + "/{collectionName}" )
    public JSONWithPadding rebuildIndexes( @Context UriInfo ui,
                                           @PathParam( "applicationId" ) final String applicationIdStr,
                                           @PathParam( "collectionName" ) final String collectionName,
                                           @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback,
                                           @QueryParam( "delay" ) @DefaultValue( "10" ) final long delay )
            throws Exception {

        final UUID appId = UUIDUtils.tryExtractUUID( applicationIdStr );
        ApiResponse response = createApiResponse();
        response.setAction( "rebuild indexes" );

        final Thread rebuild = new Thread() {

            public void run() {

                rebuildCollection( appId, collectionName, delay );
            }
        };

        rebuild.setName( String.format( "Index rebuild for app %s and collection %s", appId, collectionName ) );
        rebuild.setDaemon( true );
        rebuild.start();

        response.setSuccess();

        return new JSONWithPadding( response, callback );
    }

    @RequireSystemAccess
    @POST
    @Path( RootResource.APPLICATION_ID_PATH )
    public JSONWithPadding addIndex(@Context UriInfo ui,
                                    @PathParam( "applicationId" ) final String applicationIdStr,
                                    Map<String, Object> config,
                                    @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback)  throws Exception{
        Preconditions.checkNotNull(config,"Payload for config is null, please pass {replicas:int, shards:int} in body");

        ApiResponse response = createApiResponse();
        final UUID appId = UUIDUtils.tryExtractUUID(applicationIdStr);

        if(!config.containsKey("replicas") || !config.containsKey("shards") ||
                !(config.get("replicas") instanceof Integer) || !(config.get("shards") instanceof Integer)){
            throw new IllegalArgumentException("body must contains 'replicas' of type int and 'shards' of type int");
        }

        if(!config.containsKey("indexSuffix")) {
            throw new IllegalArgumentException("Please add an indexSuffix to your post");
        }

        emf.addIndex(appId, config.get("indexSuffix").toString(), (int) config.get("shards"),(int) config.get("replicas"));
        response.setAction("Add index to alias");

        return new JSONWithPadding(response, callback);

    }

    private void rebuildCollection( final UUID applicationId, final String collectionName, final long delay ) {
        EntityManagerFactory.ProgressObserver po = new EntityManagerFactory.ProgressObserver() {

            @Override
            public void onProgress( final EntityRef entity ) {
                logger.info( "Indexing entity {}:{}", entity.getType(), entity.getUuid() );
            }
            @Override
            public long getWriteDelayTime() {
                return delay;
            }
        };


        logger.info( "Reindexing for app id: {} and collection {}", applicationId, collectionName );

        emf.rebuildCollectionIndex( applicationId, collectionName, po );
        emf.refreshIndex();
    }

}
