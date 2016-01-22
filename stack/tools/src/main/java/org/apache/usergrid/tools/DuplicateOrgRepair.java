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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.*;
import rx.Scheduler;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.usergrid.tools.UserOrgInterface.Org;
import static org.apache.usergrid.tools.UserOrgInterface.OrgUser;

/**
 * Find duplicate orgs, delete all but oldest of each and assign users to it.
 */
public class DuplicateOrgRepair extends ToolBase {

    UserOrgInterface        manager = null;

    Map<String, Set<Org>>   orgsByName = new HashMap<String, Set<Org>>();

    Map<UUID, Org>          orgsById = new HashMap<UUID, Org>();

    Map<OrgUser, Set<Org>>  orgsByUser = new HashMap<OrgUser, Set<Org>>();

    Map<String, Set<Org>>   duplicatesByName = new HashMap<String, Set<Org>>();

    static final String     THREADS_ARG_NAME = "threads";

    int                     threadCount = 5;

    static final String     DRYRUN_ARG_NAME = "dryrun";

    boolean                 dryRun = false;

    static final String     NO_AUGMENT_ARG_NAME = "noaugment";

    boolean                 noAugment = false;

    static final String     ORG1_ID = "org1";

    static final String     ORG2_ID = "org2";

    boolean                 testing = false;


    DuplicateOrgRepair() {
        super();
    }

    DuplicateOrgRepair( EntityManagerFactory emf, ManagementService managementService ) {
        this();
        this.emf = emf;
        this.managementService = managementService;
    }

    @Override
    @SuppressWarnings("static-access")
    public Options createOptions() {

        Options options = super.createOptions();

        Option dryRunOption = OptionBuilder.hasArg()
            .withType(Boolean.TRUE)
            .withDescription( "-" + DRYRUN_ARG_NAME + " true to print what tool would do and do not alter data.")
            .create( DRYRUN_ARG_NAME );
        options.addOption( dryRunOption );

        Option noAugmentRunOption = OptionBuilder.hasArg()
            .withType(Boolean.TRUE)
            .withDescription( "-" + NO_AUGMENT_ARG_NAME + " true to skip the augment step.")
            .create( NO_AUGMENT_ARG_NAME );
        options.addOption( noAugmentRunOption );


        Option writeThreadsOption = OptionBuilder.hasArg()
            .withType(0)
            .withDescription( "Write Threads -" + THREADS_ARG_NAME )
            .create(THREADS_ARG_NAME);
        options.addOption( writeThreadsOption );

        Option org1Option = OptionBuilder.hasArg()
            .withType(0)
            .withDescription( "Duplicate org #1 id -" + ORG1_ID)
            .create(ORG1_ID);
        options.addOption( org1Option );

        Option org2Option = OptionBuilder.hasArg()
            .withType(0)
            .withDescription( "Duplicate org #2 id -" + ORG2_ID)
            .create(ORG2_ID);
        options.addOption( org2Option );

        return options;
    }

    @Override
    public void runTool(CommandLine line) throws Exception {

        startSpring();
        setVerbose( line );

        UUID org1uuid = null;
        UUID org2uuid = null;

        String org1string = line.getOptionValue( ORG1_ID );
        String org2string = line.getOptionValue( ORG2_ID );

        if ( org1string != null && org2string == null ) {
            logger.error("- if {} is specified you must also specify {} and vice-versa", ORG1_ID, ORG2_ID);
            return;

        } else if ( org2string != null && org1string == null ) {
            logger.error("- if {} is specified you must also specify {} and vice-versa", ORG2_ID, ORG1_ID);
            return;

        } else if ( org1string != null && org2string != null ) {

            try {
                org1uuid = UUID.fromString( org1string );
                org2uuid = UUID.fromString( org2string );
            } catch (Exception e) {
                logger.error("{} and {} must be specified as UUIDs", ORG1_ID, ORG2_ID);
                return;
            }
        }

        if (StringUtils.isNotEmpty( line.getOptionValue( THREADS_ARG_NAME ) )) {
            try {
                threadCount = Integer.parseInt( line.getOptionValue( THREADS_ARG_NAME ) );
            } catch (NumberFormatException nfe) {
                logger.error( "-" + THREADS_ARG_NAME + " must be specified as an integer. Aborting..." );
                return;
            }
        }

        if ( StringUtils.isNotEmpty( line.getOptionValue( DRYRUN_ARG_NAME ) )) {
            dryRun = Boolean.parseBoolean( line.getOptionValue( DRYRUN_ARG_NAME ));
        }

        if ( StringUtils.isNotEmpty( line.getOptionValue( NO_AUGMENT_ARG_NAME ) )) {
            noAugment = Boolean.parseBoolean( line.getOptionValue( NO_AUGMENT_ARG_NAME ));
        }

        if ( manager == null ) { // we use a special manager when mockTesting
            if (dryRun) {
                manager = new DryRunUserOrgManager( emf, managementService );
            } else {
                manager = new UserOrgManager( emf, managementService );
            }
        }

        logger.info( "DuplicateOrgRepair tool starting up... manager: " + manager.getClass().getSimpleName() );

        if ( org1uuid != null && org2uuid != null ) {

            Org org1 = manager.getOrg( org1uuid );
            Org org2 = manager.getOrg( org2uuid );

            if ( org1.getName().equalsIgnoreCase( org2.getName() )) {
                buildOrgMaps( org1, org2 );
            } else {
                logger.error("org1 and org2 do not have same duplicate name");
                return;
            }

        } else {
            buildOrgMaps();
        }

        if ( noAugment == false ) {
            augmentUserOrgsMap();
        }

        manager.logDuplicates( duplicatesByName );

        mergeDuplicateOrgs();

        removeDuplicateOrgs();

        logger.info( "DuplicateOrgRepair work is done!");
    }


    public UserOrgManager createNewRepairManager() {
        return new UserOrgManager( emf, managementService );
    }


    private void buildOrgMaps(Org org1, Org org2) {

        Set<Org> orgs = new HashSet<Org>();
        orgs.add( org1 );
        orgs.add( org2 );
        orgsByName.put(       org1.getName().toLowerCase(), orgs );
        duplicatesByName.put( org1.getName().toLowerCase(), orgs );

        orgsById.put( org1.getId(), org1 );
        orgsById.put( org2.getId(), org2 );

        for ( Org org : orgs ) {
            try {
                Set<OrgUser> orgUsers = manager.getOrgUsers( org );
                for (OrgUser user : orgUsers) {
                    Set<Org> usersOrgs = orgsByUser.get( user );
                    if (usersOrgs == null) {
                        usersOrgs = new HashSet<Org>();
                        orgsByUser.put( user, usersOrgs );
                    }
                    usersOrgs.add( org );
                }
            } catch (Exception e) {
                logger.error( "Error getting users for org {}:{}", org.getName(), org.getId() );
                logger.error( "Stack trace is: ", e );
            }
        }

    }


    /**
     * build map of orgs by name, orgs by id, orgs by user and duplicate orgs by name
     */
    private void buildOrgMaps() throws Exception {

        manager.getOrgs().doOnNext( new Action1<Org>() {
            @Override
            public void call(Org org) {

                // orgs by name and duplicate orgs by name maps

                Set<Org> orgs = orgsByName.get( org.getName().toLowerCase() );
                if (orgs == null) {
                    orgs = new HashSet<Org>();
                    orgsByName.put( org.getName().toLowerCase(), orgs );
                } else {
                    duplicatesByName.put( org.getName().toLowerCase(), orgs );
                }
                orgs.add( org );

                orgsById.put( org.getId(), org );

                // orgs by user map, created via org -> user connections

                try {
                    Set<OrgUser> orgUsers = manager.getOrgUsers( org );
                    for ( OrgUser user : orgUsers ) {
                        Set<Org> usersOrgs = orgsByUser.get( user );
                        if (usersOrgs == null) {
                            usersOrgs = new HashSet<Org>();
                            orgsByUser.put( user, usersOrgs );
                        }
                        usersOrgs.add( org );
                    }
                } catch (Exception e) {
                    logger.error("Error getting users for org {}:{}", org.getName(), org.getId());
                    logger.error("Stack trace is: ", e);
                }

            }

        } ).toBlocking().lastOrDefault( null );

        logger.info( "DuplicateOrgRepair tool built org maps");
    }


    /**
     * augment user orgs map via user -> org connections
     */
    private void augmentUserOrgsMap() throws Exception {

        ExecutorService writeThreadPoolExecutor = Executors.newFixedThreadPool( threadCount );
        Scheduler scheduler = Schedulers.from( writeThreadPoolExecutor );

        manager.getUsers().doOnNext( new Action1<OrgUser>() {
            @Override
            public void call(OrgUser user) {
                try {
                    Set<Org> connectedToOrgs = manager.getUsersOrgs(user);
                    Set<Org> usersOrgs = orgsByUser.get(user);
                    if ( usersOrgs == null ) {
                        usersOrgs = new HashSet<Org>();
                    }
                    for ( Org org : connectedToOrgs ) {
                        if (!usersOrgs.contains(org)) {
                            usersOrgs.add(org);
                        }
                    }

                } catch (Exception e) {
                    logger.error("Error getting orgs for user {}:{}", user.getUsername(), user.getId());
                    logger.error("Stack trace is: ", e);
                }
            }
        } ).subscribeOn( scheduler ).toBlocking().lastOrDefault( null );

        logger.info( "DuplicateOrgRepair augmented user orgs map");
    }


    /**
     * For each duplicate name, pick best org and merge apps and users into it
     */
    private void mergeDuplicateOrgs() throws Exception {

        for ( String dupName : duplicatesByName.keySet() ) {
            Set<Org> duplicateOrgs = duplicatesByName.get(dupName);
            Org bestOrg = manager.selectBest( duplicateOrgs );

            for ( Org org : duplicateOrgs ) {

                if ( !org.equals( bestOrg )) {

                    Set<OrgUser> orgUsers = new HashSet<OrgUser>( manager.getOrgUsers( org ) );

                    for (OrgUser user : orgUsers) {
                        if (dryRun) {
                            Object[] args = new Object[]{
                                user.getUsername(), user.getId(), bestOrg.getName(), bestOrg.getId()};
                            logger.info( "Would add user {}:{} to org {}:{}", args );
                            args = new Object[]{
                                user.getUsername(), user.getId(), org.getName(), org.getId()};
                            logger.info( "Would remove user {}:{}  org {}:{}", args );
                        } else {
                            try {
                                manager.addUserToOrg( user, bestOrg );
                            } catch ( Exception e ) {
                                Object[] args = new Object[]{
                                    user.getUsername(), user.getId(), bestOrg.getName(), bestOrg.getId()};
                                logger.error( "Error adding user {}:{} to org {}:{}", args );
                            }
                            try {
                                manager.removeUserFromOrg( user, org );
                            } catch ( Exception e ) {
                                Object[] args = new Object[]{
                                    user.getUsername(), user.getId(), org.getName(), org.getId()};
                                logger.info( "Error removing user {}:{}  org {}:{}", args );
                            }
                        }
                    }

                    Set<UUID> orgApps = new HashSet<UUID>( manager.getOrgApps( org ) );

                    for (UUID appId : orgApps) {
                        if (dryRun) {
                            Object[] args = new Object[]{ appId, bestOrg.getName(), bestOrg.getId()};
                            logger.info( "Would add app {} to org {}:{}", args );
                            args = new Object[]{ appId, org.getName(), org.getId()};
                            logger.info( "Would remove app {} org {}:{}", args );
                        } else {
                            try {
                                manager.addAppToOrg( appId, bestOrg );
                            } catch ( Exception e ) {
                                Object[] args = new Object[]{ appId, bestOrg.getName(), bestOrg.getId()};
                                logger.error( "Error adding app {} to org {}:{}", args );
                            }
                            try {
                                manager.removeAppFromOrg( appId, org );
                            } catch (Exception e  ) {
                                Object[] args = new Object[]{ appId, org.getName(), org.getId()};
                                logger.info( "Error removing app {} org {}:{}", args );
                            }
                        }
                    }

                }
            }
        }

        logger.info( "DuplicateOrgRepair merged duplicate orgs");
    }


    /**
     * remove/rename duplicate orgs so they no longer impact operation of system
     */
    private void removeDuplicateOrgs() throws Exception {
        for ( String dupName : duplicatesByName.keySet() ) {
            Set<Org> orgs = duplicatesByName.get( dupName );
            Org best = manager.selectBest( orgs );
            for ( Org candidate : orgs ) {
                if ( !candidate.equals(best) ) {
                    if ( dryRun ) {
                        logger.info("Would rename/remove org {}:{}",
                            new Object[] { candidate.getName(), candidate.getId() });
                    } else {
                        manager.removeOrg( best, candidate );
                    }
                }
            }
        }

        logger.info( "DuplicateOrgRepair renamed/removed duplicate orgs");
    }

}
