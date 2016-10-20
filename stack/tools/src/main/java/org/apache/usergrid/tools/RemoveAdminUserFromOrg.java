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
package org.apache.usergrid.tools;

import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.usergrid.corepersistence.CpEntityManager;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.graph.*;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import java.util.List;
import java.util.UUID;


/**
 * Will log information about an Admin User and an Organization, and
 * if the "-remove" option is specified will remove the Admin User from the Organization.
 */
public class RemoveAdminUserFromOrg extends ToolBase {

    @Override
    public Options createOptions() {

        Options options = new Options();

        options.addOption( "u", "user", true, "Email of Admin User to examine" );
        options.addOption( "o", "org", true, "Organization to examine" );
        options.addOption( "r", "remove", false, "Remove Admin User from Organization" );

        return options;
    }


    @Override
    public void runTool( CommandLine line ) throws Exception {

        startSpring();

        String orgName = line.getOptionValue("org");
        String email = line.getOptionValue("user");

        if ( orgName == null ) {
            System.out.println("org argument is required");
        }

        if ( email == null ) {
            System.out.println("user argument is required");
        }

        boolean remove = line.hasOption( "remove" );

        // get user and org objects

        UUID ownerId = emf.getManagementAppId();
        CpEntityManager em = (CpEntityManager)emf.getEntityManager( ownerId );

        UserInfo userInfo = managementService.getAdminUserByEmail( email );
        User user = em.get( userInfo.getUuid(), User.class );

        OrganizationInfo orgInfo = managementService.getOrganizationByName( orgName );
        Group group = em.get( orgInfo.getUuid(), Group.class );


        StringBuilder sb = new StringBuilder();
        try {

            sb.append( "\nUser " ).append( user.getUsername() ).append( ":" ).append( user.getUuid().toString() );
            sb.append( "\nOrganization " ).append( orgName ).append( ":" ).append( orgInfo.getUuid() );


            //---------------------------------------------------------------------------------------------
            // log connections found via entity manager and management service

            Results users = em.getCollection( group, "users", null, 1000, Query.Level.ALL_PROPERTIES, false );
            if ( users.isEmpty() ) {
                sb.append("\n   Organization has no Users\n");
            } else {
                sb.append("\n   Organization has Users:\n");
                for ( Entity entity : users.getEntities() ) {
                    sb.append("      User ").append( entity.getUuid() ).append( ":" ).append( entity.getName());
                    sb.append("\n" );
                }
            }

            BiMap<UUID, String> orgsForAdminUser = managementService.getOrganizationsForAdminUser( user.getUuid() );
            if (orgsForAdminUser.isEmpty()) {
                sb.append( "   User has no Organizations\n" );
            } else {
                sb.append( "   User has Organizations\n" );
                for (UUID key : orgsForAdminUser.keySet()) {
                    String name = orgsForAdminUser.get( key );
                    sb.append( "       Organization " ).append( name ).append( ":" ).append( key ).append( "\n" );
                }
            }


            List<UserInfo> adminUsers = managementService.getAdminUsersForOrganization( orgInfo.getUuid() );
            if (adminUsers.isEmpty()) {
                sb.append( "   Organization has no Admin Users\n" );
            } else {
                sb.append( "   Organization has Admin Users:" ).append( "\n" );
                for (UserInfo info : adminUsers) {
                    sb.append( "       Admin User " )
                        .append( info.getUsername() ).append( ":" ).append( info.getUuid() ).append( "\n" );
                }
            }


            //---------------------------------------------------------------------------------------------
            // log connections found via graph manager

            final GraphManagerFactory gmf = injector.getInstance( GraphManagerFactory.class );
            final GraphManager graphManager = gmf.createEdgeManager(
                new ApplicationScopeImpl( new SimpleId( emf.getManagementAppId(), Application.ENTITY_TYPE ) ) );

            final Id groupId = new SimpleId( orgInfo.getUuid(), Group.ENTITY_TYPE );
            final Id userId = new SimpleId( user.getUuid(), User.ENTITY_TYPE );


            // edge versions from group -> user

            sb.append( "Edges from collectionToEntity:\n" );
            SearchByEdge collectionToEntity = CpNamingUtils.createEdgeFromCollectionName(
                groupId, "users", userId );

            graphManager.loadEdgeVersions( collectionToEntity ).forEach( edge ->
                sb.append( "edge from " ).append( edge.getSourceNode() )
                  .append( " to " ).append( edge.getTargetNode()));
            sb.append("\n" );

            // edge versions from user -> group

            sb.append( "Edges from entityToCollection:\n" );
            SearchByEdge entityToCollection = CpNamingUtils.createEdgeFromCollectionName(
                userId, "groups", groupId );

            graphManager.loadEdgeVersions( entityToCollection ).forEach( edge ->
                sb.append( "edge from " ).append( edge.getSourceNode() )
                    .append( " to " )
                    .append( edge.getTargetNode() ).append("\n") );


            //---------------------------------------------------------------------------------------------
            // optionally remove admin user

            if ( remove ) {
                // use normal means to remove user from org
                managementService.removeAdminUserFromOrganization( user.getUuid(), orgInfo.getUuid() );
            }

            // make sure no edges left behind

            String usersCollType     = CpNamingUtils.getEdgeTypeFromCollectionName( "users" );
            sb.append( "Edges of type ").append( usersCollType ).append(" targeting user:\n" );

            graphManager.loadEdgesToTarget( createSearch( userId, usersCollType ) ).forEach( edge -> {

                if ( remove && edge.getSourceNode().getUuid().equals( group.getUuid() ) ) {
                    sb.append( "    DELETING edge from " ).append( edge.getSourceNode() )
                        .append( " to " ).append( edge.getTargetNode() ).append("\n");

                    graphManager.markEdge( edge );
                    graphManager.deleteEdge( edge );

                } else {
                    sb.append( "    edge from " ).append( edge.getSourceNode() )
                        .append( " to " ).append( edge.getTargetNode() ).append("\n");
                }
            });

            sb.append( "Edges of type ").append( usersCollType ).append(" sourced from group:\n" );

            graphManager.loadEdgesFromSource( createSearch( groupId, usersCollType ) ).forEach( edge -> {

                if ( remove && edge.getTargetNode().getUuid().equals( user.getUuid() ) ) {
                    sb.append( "    DELETING edge from " ).append( edge.getSourceNode() )
                        .append( " to " ).append( edge.getTargetNode() ).append("\n");

                    graphManager.markEdge( edge ).toBlocking().lastOrDefault( null );
                    graphManager.deleteEdge( edge ).toBlocking().lastOrDefault( null );

                } else {
                    sb.append( "    edge from " ).append( edge.getSourceNode() )
                        .append( " to " ).append( edge.getTargetNode() ).append("\n");
                }}
            );

        } finally {
            logger.info( sb.toString() );
        }
    }

    private SearchByEdgeType createSearch( Id node, String edgeType ) {
        return new SimpleSearchByEdgeType(
            node,                             // node
            edgeType,                         // edge type
            0L,                               // max timestamp to return
            SearchByEdgeType.Order.ASCENDING, // order
            Optional.<Edge>absent());         // last

    }
}
