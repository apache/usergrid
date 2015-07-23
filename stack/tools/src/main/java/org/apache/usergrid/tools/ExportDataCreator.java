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


import io.codearte.jfairy.Fairy;
import io.codearte.jfairy.producer.company.Company;
import io.codearte.jfairy.producer.person.Person;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.entities.Activity;
import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;

import java.util.*;


/**
 * Create an app full of users and data.
 */
public class ExportDataCreator extends ToolBase {

    public static final String APP_NAME = "application";
    public static final String ORG_NAME = "organization";
    public static final String NUM_USERS = "users";
    public static final String NUM_COLLECTIONS = "collections";
    public static final String NUM_ENTITIES = "entities";
    public static final String ADMIN_USERNAME = "username";
    public static final String ADMIN_PASSWORD = "password";

    public String appName = "test-app";
    public String orgName = "test-organization";
    public int numUsers = 100;
    public int numCollections = 20;
    public int numEntities = 100;
    public String adminUsername = "adminuser";
    public String adminPassword = "test";


    @Override
    public void runTool(CommandLine line) throws Exception {

        startSpring();

        setVerbose( line );

        if (line.hasOption( APP_NAME )) {
            appName = line.getOptionValue( APP_NAME );
        }
        if (line.hasOption( ORG_NAME )) {
            orgName = line.getOptionValue( ORG_NAME );
        }
        if (line.hasOption( NUM_USERS )) {
            numUsers = Integer.parseInt( line.getOptionValue( NUM_USERS ) );
        }
        if (line.hasOption( NUM_COLLECTIONS )) {
            numCollections = Integer.parseInt( line.getOptionValue( NUM_COLLECTIONS ) );
        }
        if (line.hasOption( NUM_ENTITIES )) {
            numEntities = Integer.parseInt( line.getOptionValue( NUM_ENTITIES ) );
        }
        if (line.hasOption( ADMIN_USERNAME )) {
            adminUsername = line.getOptionValue( ADMIN_USERNAME );
        }
        if (line.hasOption( ADMIN_PASSWORD )) {
            adminPassword = line.getOptionValue( ADMIN_PASSWORD );
        }

        createTestData();
    }


    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Options options = super.createOptions();

        Option appName = OptionBuilder.hasArg()
                .withDescription( "Application name to use" ).create( APP_NAME );

        Option orgName = OptionBuilder.hasArg()
                .withDescription( "Organization to use (will create if not present)" ).create( ORG_NAME );

        Option numUsers = OptionBuilder.hasArg()
                .withDescription( "Number of users create (in addition to users)" ).create( NUM_USERS );

        Option numCollection = OptionBuilder.hasArg()
                .withDescription( "Number of collections to create (in addition to users)" ).create( NUM_COLLECTIONS );

        Option numEntities = OptionBuilder.hasArg()
                .withDescription( "Number of entities to create per collection" ).create( NUM_ENTITIES );

        Option adminUsername = OptionBuilder.hasArg()
                .withDescription( "Admin Username" ).create( ADMIN_USERNAME );

        Option adminPassword = OptionBuilder.hasArg()
                .withDescription( "Admin Password" ).create( ADMIN_PASSWORD );

        options.addOption( appName );
        options.addOption( orgName );
        options.addOption( numUsers );
        options.addOption( numCollection );
        options.addOption( numEntities );
        options.addOption( adminUsername );
        options.addOption( adminPassword );

        return options;
    }


    public void createTestData() throws Exception {

        OrganizationInfo orgInfo = managementService.getOrganizationByName( orgName );

        if (orgInfo == null) {
            OrganizationOwnerInfo ownerInfo = managementService.createOwnerAndOrganization(
                    orgName, adminUsername + "@example.com", adminUsername,
                    adminUsername + "@example.com", adminPassword, true, false );
            orgInfo = ownerInfo.getOrganization();
        }

        ApplicationInfo appInfo = managementService.getApplicationInfo( orgName + "/" + appName );

        if (appInfo == null) {
            UUID appId = managementService.createApplication( orgInfo.getUuid(), appName ).getId();
            appInfo = managementService.getApplicationInfo( appId );
        }

        EntityManager em = emf.getEntityManager( appInfo.getId() );

        Fairy fairy = Fairy.create();

        List<Entity> users = new ArrayList<Entity>( numUsers );

        for (int i = 0; i < numUsers; i++) {

            final Person person = fairy.person();
            Entity userEntity = null;
            try {
                final Map<String, Object> userMap = new HashMap<String, Object>() {{
                    put( "username", person.username() );
                    put( "password", person.password() );
                    put( "email", person.email() );
                    put( "companyEmail", person.companyEmail() );
                    put( "dateOfBirth", person.dateOfBirth() );
                    put( "firstName", person.firstName() );
                    put( "lastName", person.lastName() );
                    put( "nationalIdentificationNumber", person.nationalIdentificationNumber() );
                    put( "telephoneNumber", person.telephoneNumber() );
                    put( "passportNumber", person.passportNumber() );
                    put( "address", person.getAddress() );
                }};

                userEntity = em.create( "user", userMap );
                users.add( userEntity );

            } catch (DuplicateUniquePropertyExistsException e) {
                logger.error( "Dup user generated: " + person.username() );
                continue;
            } catch (Exception e) {
                logger.error("Error creating user", e);
                continue;
            }

            final Company company = person.getCompany();
            try {
                EntityRef ref = em.getAlias( "company", company.name() );
                Entity companyEntity = (ref == null) ? null : em.get( ref );
              
                // create company if it does not exist yet
                if ( companyEntity == null ) {
                    final Map<String, Object> companyMap = new HashMap<String, Object>() {{
                        put( "name", company.name() );
                        put( "domain", company.domain() );
                        put( "email", company.email() );
                        put( "url", company.url() );
                        put( "vatIdentificationNumber", company.vatIdentificationNumber() );
                    }};
                    companyEntity = em.create( "company", companyMap );
                } else {
                    logger.info("Company {} already exists", company.name());
                }

                em.createConnection( userEntity, "employer", companyEntity );

            } catch (DuplicateUniquePropertyExistsException e) {
                logger.error( "Dup company generated {} property={}", company.name(), e.getPropertyName() );
                continue;
            } catch (Exception e) {
                logger.error("Error creating or connecting company", e);
                continue;
            }
            
            try {
                for (int j = 0; j < 5; j++) {
                    Activity activity = new Activity();
                    Activity.ActivityObject actor = new Activity.ActivityObject();
                    actor.setEntityType( "user" );
                    actor.setId( userEntity.getUuid().toString() );
                    activity.setActor( actor );
                    activity.setVerb( "POST" );
                    activity.setContent( "User " + person.username() + " generated a random string "
                            + RandomStringUtils.randomAlphanumeric( 5 ) );
                    em.createItemInCollection( userEntity, "activities", "activity", activity.getProperties() );
                }

                if (users.size() > 10) {
                    for (int j = 0; j < 5; j++) {
                        try {
                            em.createConnection( userEntity, "associate", users.get( (int) (Math.random() * users.size()) ) );
                        } catch (Exception e) {
                            logger.error( "Error connecting user to user: " + e.getMessage() );
                        }
                    }
                }
                
            } catch (Exception e) {
                logger.error("Error creating activities", e);
                continue;
            }

        }
    }

}
