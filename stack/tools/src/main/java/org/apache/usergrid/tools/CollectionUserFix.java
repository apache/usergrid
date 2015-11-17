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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Identifier;
import org.apache.usergrid.persistence.PagingResultsIterator;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


/**
 * Fixes issues where admin data is in organizations but can't accessed or reset the password of. This usually returns
 * the error "Could not find organization for email" because the user can't be found but when the org is queried the
 * user shows up there. If you see that then run this tool.
 */
//If index corruptions show up then the tool won't complete.
public class CollectionUserFix extends ExportingToolBase {
    private static final int PAGE_SIZE = 1000;

    private static final Logger logger = LoggerFactory.getLogger( CollectionUserFix.class );

    /**
     *
     */
    private static final String ORGANIZATION_ARG = "org";

    /**
     *
     */
    private static final String APPLICATION_ARG = "app";

    /**
     *
     */
    private static final String QUERY_ARG = "query";

    @Override
    @SuppressWarnings( "static-access" )
    public Options createOptions() {

        Option hostOption =
                OptionBuilder.withArgName( "host" ).hasArg().isRequired( true ).withDescription( "Cassandra host" )
                             .create( "host" );

        Option orgOption = OptionBuilder.withArgName( ORGANIZATION_ARG ).hasArg().isRequired( false )
                                        .withDescription( "organization id" ).create( ORGANIZATION_ARG );
        Option appOption = OptionBuilder.withArgName( APPLICATION_ARG ).hasArg().isRequired( false )
                                        .withDescription( "application uuid" ).create( APPLICATION_ARG );
        Option queryOption = OptionBuilder.withArgName( QUERY_ARG ).hasArg().isRequired( false )
                                        .withDescription( "query" ).create( QUERY_ARG );

        //add a query to it

        Options options = new Options();
        options.addOption( hostOption );
        options.addOption( orgOption );
        options.addOption( appOption );
        options.addOption( queryOption );

        return options;
    }


    /**
     * @param line
     * @throws Exception
     */


    @Override
    public void runTool( CommandLine line ) throws Exception {


        startSpring();

        logger.info( "Starting crawl of all users" );
        System.out.println( "Starting crawl of all users" );
        Set<Application> applicationSet = new HashSet<Application>(  );
        EntityManager em = null;



        if(line.getOptionValue( ORGANIZATION_ARG )==null||line.getOptionValue( ORGANIZATION_ARG ).isEmpty()) {
            em = emf.getEntityManager( UUID.fromString( line.getOptionValue( APPLICATION_ARG ) ) );
            applicationSet.add( em.getApplication() );
        }
        else{
            BiMap applicationsForOrganization =
                    managementService
                            .getApplicationsForOrganization( UUID.fromString(line.getOptionValue( ORGANIZATION_ARG )));

            applicationSet = applicationsForOrganization.keySet();
        }
        startCollectionFlow( em, applicationSet, line.getOptionValue( QUERY_ARG ) );


        logger.info( "Repair complete" );
        System.out.println("Repair Complete");
    }

    private void startCollectionFlow(final EntityManager entityManager, final Set<Application> app, final String queryString )
            throws Exception {// search for all orgs

        Query query = new Query();
        if(queryString!=null){
            query = query.fromQL( queryString );
        }
        query.setLimit( PAGE_SIZE );
        EntityManager em = null;
        Identifier identifier = new Identifier();


        for ( Application application : app ) {

            em = emf.getEntityManager( application.getUuid() );

            PagingResultsIterator pagingResultsIterator =
                    new PagingResultsIterator( em.searchCollection( application, "users", query ) );

            while(pagingResultsIterator.hasNext()){
                Entity entity = ( Entity ) pagingResultsIterator.next();
                String username =  entity.getProperty( "username" ).toString().toLowerCase();
                em.getUserByIdentifier( identifier.fromName( username ) );
            }

            System.out.println("Repair of application: "+ application.getApplicationName() + " complete");
        }
    }
}
