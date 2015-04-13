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
package org.apache.usergrid;


import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.entities.Activity;
import org.apache.usergrid.persistence.entities.Role;
import org.apache.usergrid.services.ServiceAction;
import org.apache.usergrid.services.ServiceManager;
import org.apache.usergrid.services.ServiceRequest;
import org.apache.usergrid.services.ServiceResults;
import org.apache.usergrid.utils.JsonUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.apache.usergrid.services.ServiceParameter.parameters;
import static org.apache.usergrid.services.ServicePayload.batchPayload;
import static org.apache.usergrid.services.ServicePayload.payload;
import static org.apache.usergrid.utils.InflectionUtils.pluralize;


public class ServiceApplication extends CoreApplication {
    private static final Logger LOG = LoggerFactory.getLogger( ServiceApplication.class );

    protected ServiceManager sm;
    protected ServiceITSetup svcSetup;
    protected boolean svcEnabled = false;


    public ServiceApplication( ServiceITSetup svcSetup ) {
        super( svcSetup );
        this.svcSetup = svcSetup;
    }


    @Override
    protected void before( Description description ) throws Exception {
        super.before( description );
        sm = svcSetup.getSmf().getServiceManager( id );
    }


    public void add( Activity activity ) {
        this.properties.putAll( activity.getProperties() );
    }


    public ServiceResults testRequest( ServiceAction action, int expectedCount, Object... params ) throws Exception {
        ServiceResults testRequest = testRequest( action, expectedCount, true, params );

        if ( !action.equals( ServiceAction.GET )) {
            this.refreshIndex();
        }

        return testRequest;
    }


    public ServiceResults testRequest( ServiceAction action, int expectedCount, boolean clear, Object... params )
            throws Exception {
        ServiceResults results = invokeService( action, params );
        assertNotNull( results );
        assertEquals( expectedCount, results.getEntities().size() );
        dumpResults( results );

        if ( clear ) {
            properties.clear();
        }

        return results;
    }


    public ServiceResults invokeService( ServiceAction action, Object... params ) throws Exception {
        ServiceRequest request = sm.newRequest( action, parameters( params ), payload( properties ) );

        LOG.info( "Request: {} {}", action, request.toString() );
        dumpProperties( properties );
        ServiceResults results = request.execute();
        assertNotNull( results );
        dumpResults( results );

        if ( !action.name().equals( ServiceAction.GET )) {
            setup.getEntityIndex().refresh();
        }

        return results;
    }


    public void dumpProperties( Map<String, Object> properties ) {
        if ( properties != null && LOG.isInfoEnabled() ) {
            LOG.info( "Input:\n {}", JsonUtils.mapToFormattedJsonString( properties ) );
        }
    }


    public void dumpResults( ServiceResults results ) {
        if ( results != null ) {
            List<Entity> entities = results.getEntities();
            svcSetup.dump( "Results", entities );
        }
    }


    public Entity doCreate( String entityType, String name ) throws Exception {
        put( "name", name );

        Entity entity = testRequest( ServiceAction.POST, 1, pluralize( entityType ) ).getEntity();
        setup.getEntityIndex().refresh();
        return entity;
    }


    public void createConnection( Entity subject, String verb, Entity noun ) throws Exception {
        sm.getEntityManager().createConnection( subject, verb, noun );
        setup.getEntityIndex().refresh();
    }


    public ServiceResults testBatchRequest( ServiceAction action, int expectedCount, List<Map<String, Object>> batch,
                                            Object... params ) throws Exception {
        ServiceRequest request = sm.newRequest( action, parameters( params ), batchPayload( batch ) );
        LOG.info( "Request: " + action + " " + request.toString() );
        // dump( "Batch", batch );
        ServiceResults results = request.execute();
        assertNotNull( results );
        assertEquals( expectedCount, results.getEntities().size() );
        dumpResults( results );

        if ( !action.name().equals( ServiceAction.GET )) {
            setup.getEntityIndex().refresh();
        }

        return results;
    }


    public ServiceResults testDataRequest( ServiceAction action, Object... params ) throws Exception {
        ServiceRequest request = sm.newRequest( action, parameters( params ), payload( properties ) );
        LOG.info( "Request: {} {}", action, request.toString() );
        dumpProperties( properties );
        ServiceResults results = request.execute();
        assertNotNull( results );
        assertNotNull( results.getData() );

        if ( !action.name().equals( ServiceAction.GET )) {
            setup.getEntityIndex().refresh();
        }

        // dump( results.getData() );
        return results;
    }


    public Entity createRole( String name, String title, int inactivity ) throws Exception {
        Entity createRole = sm.getEntityManager().createRole( name, title, inactivity );
        setup.getEntityIndex().refresh();
        return createRole;
    }


    public void grantRolePermission( String role, String permission ) throws Exception {
        sm.getEntityManager().grantRolePermission( role, permission );
        setup.getEntityIndex().refresh();
    }


    public void grantUserPermission( UUID uuid, String permission ) throws Exception {
        sm.getEntityManager().grantUserPermission( uuid, permission );
        setup.getEntityIndex().refresh();
    }


    public Set<String> getRolePermissions( String role ) throws Exception {
        Set<String> rolePermissions = sm.getEntityManager().getRolePermissions( role );
        return rolePermissions;
    }


    public EntityRef getAlias( String aliasType, String alias ) throws Exception {
        return em.getAlias( aliasType, alias );
    }


    public <T extends Entity> T get( EntityRef ref, Class<T> clazz ) throws Exception {
        return em.get( ref, clazz );
    }


    public Map<String, Role> getRolesWithTitles( Set<String> roleNames ) throws Exception {
        return em.getRolesWithTitles( roleNames );
    }


    public Entity createGroupRole( UUID id, String role, int inactivity ) throws Exception {
        return em.createGroupRole( id, role, inactivity );
    }


    public void grantGroupRolePermission( UUID id, String role, String permission ) throws Exception {
        em.grantGroupRolePermission( id, role, permission );
    }


    public ServiceManager getSm() {
        return sm;
    }
}
