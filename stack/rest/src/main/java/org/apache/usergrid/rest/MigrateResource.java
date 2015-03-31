/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.rest;


import java.util.Map;
import java.util.Set;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.apache.usergrid.persistence.core.migration.data.DataMigrationManager;
import org.apache.usergrid.persistence.core.migration.schema.MigrationManager;
import org.apache.usergrid.rest.security.annotations.RequireSystemAccess;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import com.sun.jersey.api.json.JSONWithPadding;


@Component
@Scope( "singleton" )
@Produces( {
    MediaType.APPLICATION_JSON,
    "application/javascript",
    "application/x-javascript",
    "text/ecmascript",
    "application/ecmascript",
    "text/jscript"
} )
public class MigrateResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger( MigrateResource.class );

    public MigrateResource() {
        logger.info( "SystemResource initialized" );
    }

    @Autowired
    private Injector guiceInjector;

    @RequireSystemAccess
    @PUT
    @Path( "run" )
    public JSONWithPadding migrateData( @Context UriInfo ui,
                                        @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
        throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "Migrate Data" );
        //TODO make this use the task scheduler


        final Thread migrate = new Thread() {

            @Override
            public void run() {

                logger.info( "Migrating Schema" );

                try {
                    getMigrationManager().migrate();
                }
                catch ( Exception e ) {
                    logger.error( "Unable to migrate data", e );
                }

                logger.info( "Migrating Data" );

                try {
                    getDataMigrationManager().migrate();
                }
                catch ( Exception e ) {
                    logger.error( "Unable to migrate data", e );
                }
            }
        };

        migrate.setName( "Index migrate data formats" );
        migrate.setDaemon( true );
        migrate.start();

        response.setSuccess();

        return new JSONWithPadding( response, callback );
    }


    @RequireSystemAccess
    @PUT
    @Path( "set" )
    public JSONWithPadding setMigrationVersion(
        @Context UriInfo ui, Map<String, Object> json,
        @QueryParam( "callback" ) @DefaultValue( "" ) String callback )
        throws Exception {

        logger.debug( "setMigrationVersion" );

        Preconditions.checkNotNull( json, "You must provide a json body" );
        Preconditions.checkArgument( json.keySet().size() > 0, "You must specify at least one module and version" );

        /**
         *  Set the migration version for the plugins specified
         */
        for ( final String key : json.keySet() ) {
            String version = ( String ) json.get( key );

            Preconditions.checkArgument( version != null && version.length() > 0,
                "You must specify a version field per module name" );


            int intVersion = Integer.parseInt( version );

            getDataMigrationManager().resetToVersion( key, intVersion );
        }


        return migrateStatus( ui, callback );
    }


    @RequireSystemAccess
    @GET
    @Path( "status" )
    public JSONWithPadding migrateStatus(
        @Context UriInfo ui,
        @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
        throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "Migrate Schema indexes" );

        ObjectNode node = JsonNodeFactory.instance.objectNode();



        final DataMigrationManager dataMigrationManager = getDataMigrationManager();

        final Set<String> plugins = dataMigrationManager.getPluginNames();

        for(final String pluginName: plugins){
            node.put( pluginName, dataMigrationManager.getCurrentVersion( pluginName ) );
        }

        response.setData( node );

        response.setSuccess();

        return new JSONWithPadding( response, callback );
    }


    @RequireSystemAccess
    @GET
    @Path( "count" )
    public JSONWithPadding migrateCount(
        @Context UriInfo ui,
        @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
        throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction( "Current entity count in system" );

        response.setProperty( "count", emf.performEntityCount() );

        response.setSuccess();

        return new JSONWithPadding( response, callback );
    }


    /**
     * Get the schema migration manager
     */
    private MigrationManager getMigrationManager() {
        return guiceInjector.getInstance( MigrationManager.class );
    }

    /**
     * Get the Data migration manager
     */
    private DataMigrationManager getDataMigrationManager() {
        return guiceInjector.getInstance( DataMigrationManager.class );
    }
}

