/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.collection.uniquevalues;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;
import akka.cluster.singleton.ClusterSingletonProxy;
import akka.cluster.singleton.ClusterSingletonProxySettings;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.usergrid.persistence.collection.exception.CollectionRuntimeException;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.*;
import java.util.concurrent.TimeUnit;


@Singleton
public class UniqueValuesServiceImpl implements UniqueValuesService {
    private static final Logger logger = LoggerFactory.getLogger( UniqueValuesServiceImpl.class );

    AkkaFig akkaFig;
    UniqueValuesTable table;
    private String hostname;
    private Integer port;
    private String currentRegion;

    private Map<String, ActorRef> requestActorsByRegion;
    private Map<String, String> regionsByType = new HashMap<>();

//    private final MetricRegistry metrics = new MetricRegistry();
//
//    private final Timer getTimer     = metrics.timer( "get" );
//    private final Timer   saveTimer    = metrics.timer( "save" );
//
//    private final Counter cacheCounter = metrics.counter( "cache" );
//    private final Counter dupCounter   = metrics.counter( "duplicates" );
//
//    private final Timer   reservationTimer = metrics.timer( "reservation" );
//    private final Timer   commitmentTimer  = metrics.timer( "commitment" );

    private ReservationCache reservationCache;

    private final boolean disableUniqueValues;


    @Inject
    public UniqueValuesServiceImpl( AkkaFig akkaFig, UniqueValuesTable table ) {
        this.akkaFig = akkaFig;
        this.table = table;

        ReservationCache.init( akkaFig.getUniqueValueCacheTtl() );
        this.reservationCache = ReservationCache.getInstance();

        this.disableUniqueValues = false;
    }


    /**
     * Init Akka ActorSystems and wait for request actors to start.
     */
    public void start() {

        this.hostname = akkaFig.getHostname();
        this.port = akkaFig.getPort();
        this.currentRegion = akkaFig.getRegion();

        initAkka();
        waitForRequestActors();
    }


    /**
     * For testing purposes only; does not wait for request actors to start.
     */
    public void start( String hostname, Integer port, String currentRegion ) {

        this.hostname = hostname;
        this.port = port;
        this.currentRegion = currentRegion;

        initAkka();
    }


    private Map<String, ActorRef> getRequestActorsByRegion() {
        return requestActorsByRegion;
    }


    private Map<String, String> getRegionsByType() {
        return regionsByType;
    }

//    public Counter getDupCounter() {
//        return dupCounter;
//    }
//
//    public Counter getCacheCounter() {
//        return cacheCounter;
//    }
//
//    public Timer getReservationTimer() {
//        return reservationTimer;
//    }
//
//    public Timer getCommitmentTimer() {
//        return commitmentTimer;
//    }
//
//    public Timer getSaveTimer() {
//        return saveTimer;
//    }
//
//    public Timer getGetTimer() {
//        return getTimer;
//    }

    private void initAkka() {
        logger.info("Initializing Akka");

        // Create one actor system with request actor for each region

        if ( StringUtils.isEmpty( hostname )) {
            throw new RuntimeException( "No value specified for akka.hostname");
        }

        if ( StringUtils.isEmpty( currentRegion )) {
            throw new RuntimeException( "No value specified for akka.region");
        }

        List regionList = Arrays.asList( akkaFig.getRegionList().toLowerCase().split(",") );

        logger.info("Initializing Akka for hostname {} region {} regionList {}", hostname, currentRegion, regionList);

        String typesValue = akkaFig.getRegionTypes();
        String[] regionTypes = StringUtils.isEmpty( typesValue ) ? new String[0] : typesValue.split(",");
        for ( String regionType : regionTypes ) {
            String[] parts = regionType.toLowerCase().split(":");
            String typeRegion = parts[0];
            String type = parts[1];

            if ( !regionList.contains( typeRegion) ) {
                throw new RuntimeException(
                    "'collection.akka.region.seeds' references unknown region: " + typeRegion );
            }
            this.regionsByType.put( type, typeRegion );
        }

        final Map<String, ActorSystem> systemMap = new HashMap<>();

        ActorSystem localSystem = createClusterSingletonProxies( readClusterSingletonConfigs(), systemMap );

        createRequestActors( systemMap );

        subscribeToReservations( localSystem, systemMap );
    }


    private void subscribeToReservations( ActorSystem localSystem, Map<String, ActorSystem> systemMap ) {

        for ( String region : systemMap.keySet() ) {
            ActorSystem actorSystem = systemMap.get( region );
            if ( !actorSystem.equals( localSystem ) ) {
                logger.info("Starting ReservationCacheUpdater for {}", region );
                actorSystem.actorOf( Props.create( ReservationCacheActor.class, region ), "subscriber");
            }
        }
    }


    /**
     * Create ActorSystem and ClusterSingletonProxy for every region.
     * Create ClusterSingletonManager for the current region.
     *
     * @param configMap Configurations to be used to create ActorSystems
     * @param systemMap Map of ActorSystems created by this method
     *
     * @return ActorSystem for this region.
     */
    private ActorSystem createClusterSingletonProxies(
            Map<String, Config> configMap, Map<String, ActorSystem> systemMap ) {

        ActorSystem localSystem = null;

        for ( String region : configMap.keySet() ) {
            Config config = configMap.get( region );

            ActorSystem system = ActorSystem.create( "ClusterSystem", config );
            systemMap.put( region, system );

            // cluster singletons only run role "io" nodes and NOT on "client" nodes of other regions
            if ( currentRegion.equals( region ) ) {

                localSystem = system;

                // create cluster singleton supervisor for actor system
                ClusterSingletonManagerSettings settings =
                        ClusterSingletonManagerSettings.create( system ).withRole("io");
                system.actorOf( ClusterSingletonManager.props(
                        Props.create( ClusterSingletonRouter.class, table ),
                        PoisonPill.getInstance(), settings ), "uvRouter");
            }

            // create proxy for sending messages to singleton
            ClusterSingletonProxySettings proxySettings =
                    ClusterSingletonProxySettings.create( system ).withRole("io");
            system.actorOf( ClusterSingletonProxy.props( "/user/uvRouter", proxySettings ), "uvProxy" );
        }

        return localSystem;
    }


    /**
     * Create RequestActor for each region.
     *
     * @param systemMap Map of regions to ActorSystems.
     */
    private void createRequestActors( Map<String, ActorSystem> systemMap ) {

        requestActorsByRegion = new HashMap<>();

        for ( String region : systemMap.keySet() ) {

            // Each RequestActor needs to know path to ClusterSingletonProxy and region
            ActorRef requestActor = systemMap.get( region ).actorOf(
                    Props.create( RequestActor.class, "/user/uvProxy" ), "requestActor" );

            requestActorsByRegion.put( region, requestActor );
        }
    }


    public void waitForRequestActors() {

        for ( String region : requestActorsByRegion.keySet() ) {
            ActorRef ra = requestActorsByRegion.get( region );
            waitForRequestActor( ra );
        }
    }


    private void waitForRequestActor( ActorRef ra ) {

        logger.info( "Waiting on request actor {}...", ra.path() );

        boolean started = false;
        int retries = 0;
        int maxRetries = 60;
        while (retries < maxRetries) {
            Timeout t = new Timeout( 10, TimeUnit.SECONDS );

            Future<Object> fut = Patterns.ask( ra, new RequestActor.StatusRequest(), t );
            try {
                RequestActor.StatusMessage result = (RequestActor.StatusMessage) Await.result( fut, t.duration() );

                if (result.status.equals( RequestActor.StatusMessage.Status.READY )) {
                    started = true;
                    break;
                }
                logger.info( "Waiting for request actor {} region {} ({}s)", ra.path(), currentRegion, retries );
                Thread.sleep( 1000 );

            } catch (Exception e) {
                logger.error( "Error: Timeout waiting for requestActor" );
            }
            retries++;
        }

        if (started) {
            logger.info( "RequestActor has started" );
        } else {
            throw new RuntimeException( "RequestActor did not start in time" );
        }
    }


    /**
     * Read configuration and create a Config for each region.
     *
     * @return Map of regions to Configs.
     */
    private Map<String, Config> readClusterSingletonConfigs() {

        Map<String, Config> configs = new HashMap<>();

        ListMultimap<String, String> seedsByRegion = ArrayListMultimap.create();

        String[] regionSeeds = akkaFig.getRegionSeeds().split( "," );

        try {

            if ( port != null ) {

                // we are testing
                String seed = "akka.tcp://ClusterSystem@" + hostname + ":" + port;
                seedsByRegion.put( currentRegion, seed );

            } else {

                for (String regionSeed : regionSeeds) {

                    String[] parts = regionSeed.split( ":" );
                    String region = parts[0];
                    String hostname = parts[1];
                    String regionPortString = parts[2];

                    // all seeds in same region must use same port
                    // we assume 0th seed has the right port
                    final Integer regionPort;

                    if (port == null) {
                        // we assume 0th seed has the right port
                        regionPort = Integer.parseInt( regionPortString );
                    } else {
                        regionPort = port; // unless we are testing
                    }

                    String seed = "akka.tcp://ClusterSystem@" + hostname + ":" + regionPort;

                    seedsByRegion.put( region, seed );
                }

                if (seedsByRegion.keySet().isEmpty()) {
                    throw new RuntimeException(
                        "No seeds listed in 'parsing collection.akka.region.seeds' property." );
                }
            }

            int numInstancesPerNode = akkaFig.getUniqueValueActors();

            for ( String region : seedsByRegion.keySet() ) {

                List<String> seeds = seedsByRegion.get( region );

                final Integer regionPort;

                if (port == null) {
                    // we assume 0th seed has the right port
                    int lastColon = seeds.get(0).lastIndexOf(":") + 1;
                    regionPort = Integer.parseInt( seeds.get(0).substring( lastColon ));
                } else {
                    regionPort = port; // unless we are testing
                }

                // cluster singletons only run role "io" nodes and NOT on "client" nodes of other regions
                String clusterRole = currentRegion.equals( region ) ? "io" : "client";

                logger.info( "Config for region {} is:\npoc Akka Hostname {}\npoc Akka Seeds {}\n" +
                                "poc Akka Port {}\npoc UniqueValueActors per node {}",
                        region, hostname, seeds, port, numInstancesPerNode );

                Map<String, Object> configMap = new HashMap<String, Object>() {{
                    put( "akka", new HashMap<String, Object>() {{
                        put( "remote", new HashMap<String, Object>() {{
                            put( "netty.tcp", new HashMap<String, Object>() {{
                                put( "hostname", hostname );
                                put( "bind-hostname", hostname );
                                put( "port", regionPort );
                            }} );
                        }} );
                        put( "cluster", new HashMap<String, Object>() {{
                            put( "max-nr-of-instances-per-node", numInstancesPerNode );
                            put( "roles", Collections.singletonList(clusterRole) );
                            put( "seed-nodes", new ArrayList<String>() {{
                                for (String seed : seeds) {
                                    add( seed );
                                }
                            }} );
                        }} );
                        put( "actor", new HashMap<String, Object>() {{
                            put( "deployment", new HashMap<String, Object>() {{
                                put( "/uvRouter/singleton/router", new HashMap<String, Object>() {{
                                    put( "cluster", new HashMap<String, Object>() {{
                                        //put( "roles", Collections.singletonList(role) );
                                        put( "max-nr-of-instances-per-node", numInstancesPerNode );
                                    }} );
                                }} );
                            }} );
                        }} );
                    }} );
                }};

                Config config = ConfigFactory
                        .parseMap( configMap )
                        .withFallback( ConfigFactory.parseString( "akka.cluster.roles = [io]" ) )
                        .withFallback( ConfigFactory.load( "cluster-singleton" ) );

                configs.put( region, config );
            }

        } catch ( Exception e ) {
            throw new RuntimeException("Error 'parsing collection.akka.region.seeds' property", e );
        }

        return configs;
    }


    @Override
    public void reserveUniqueValues(
        ApplicationScope scope, Entity entity, UUID version, String region ) throws UniqueValueException {

        if ( this.getRequestActorsByRegion().isEmpty() ) {
            throw new RuntimeException("Unique values service not initialized, no request actors ready");
        }

        try {
            for (Field field : entity.getFields()) {
                if (field.isUnique()) {
                    reserveUniqueField( scope, entity, version, field, region );
                }
            }

        } catch ( UniqueValueException e ) {

            for (Field field : entity.getFields()) {
                try {
                    cancelUniqueField( scope, entity, version, field, region );
                } catch (Throwable ignored) {
                    logger.debug( "Error canceling unique field", ignored );
                }
            }
            throw e;
        }

    }


    @Override
    public void confirmUniqueValues(
        ApplicationScope scope, Entity entity, UUID version, String region ) throws UniqueValueException {

        if ( this.getRequestActorsByRegion().isEmpty() ) {
            throw new RuntimeException("Unique values service not initialized, no request actors ready");
        }

        try {
            for (Field field : entity.getFields()) {
                if (field.isUnique()) {
                    confirmUniqueField( scope, entity, version, field, region );
                }
            }

        } catch ( UniqueValueException e ) {

            for (Field field : entity.getFields()) {
                try {
                    cancelUniqueField( scope, entity, version, field, region) ;
                } catch (Throwable ignored) {
                    logger.debug( "Error canceling unique field", ignored );
                }
            }
            throw e;
        }

    }


    private void reserveUniqueField(
        ApplicationScope scope, Entity entity, UUID version, Field field, String region ) throws UniqueValueException {

        final ActorRef requestActor;
        if ( region != null ) {
            requestActor = getRequestActorsByRegion().get( region );
        } else {
            requestActor = lookupRequestActorForType( entity.getId().getType() );
        }

        if ( requestActor == null ) {
            throw new RuntimeException( "No request actor for region or type, cannot verify unique fields!" );
        }

        UniqueValueActor.Request request = new UniqueValueActor.Reservation(
            scope, entity.getId(), version, field );

        UniqueValueActor.Reservation res = reservationCache.get( request.getConsistentHashKey() );
//        if ( res != null ) {
//            getCacheCounter().inc();
//        }
        if ( res != null && !res.getOwner().equals( request.getOwner() )) {
            throw new UniqueValueException( "Error property not unique (cache)", field);
        }

        sendUniqueValueRequest( entity, requestActor, request );
    }


    private void confirmUniqueField(
        ApplicationScope scope, Entity entity, UUID version, Field field, String region) throws UniqueValueException {

        ActorRef requestActor = lookupRequestActorForType( entity.getId().getType() );

        if ( requestActor == null ) {
            throw new RuntimeException( "No request actor for type, cannot verify unique fields!" );
        }

        UniqueValueActor.Confirmation request = new UniqueValueActor.Confirmation(
            scope, entity.getId(), version, field );

        sendUniqueValueRequest( entity, requestActor, request );
    }


    private void cancelUniqueField(
        ApplicationScope scope, Entity entity, UUID version, Field field, String region ) throws UniqueValueException {

        ActorRef requestActor = lookupRequestActorForType( entity.getId().getType() );

        if ( requestActor == null ) {
            throw new RuntimeException( "No request actor for type, cannot verify unique fields!" );
        }

        UniqueValueActor.Confirmation request = new UniqueValueActor.Confirmation(
            scope, entity.getId(), version, field );

        requestActor.tell( request, null );
    }


    private ActorRef lookupRequestActorForType( String type ) {
        final String region = getRegionsByType().get( type );
        ActorRef requestActor = getRequestActorsByRegion().get( region == null ? currentRegion : region );
        if ( requestActor == null ) {
            throw new RuntimeException( "No request actor available for region: " + region );
        }
        return requestActor;
    }


    private void sendUniqueValueRequest(
        Entity entity, ActorRef requestActor, UniqueValueActor.Request request ) throws UniqueValueException {

        int maxRetries = 5;
        int retries = 0;

        UniqueValueActor.Response response = null;
        while ( retries++ < maxRetries ) {
            try {
                Timeout t = new Timeout( 1, TimeUnit.SECONDS );

                // ask RequestActor and wait (up to timeout) for response

                Future<Object> fut = Patterns.ask( requestActor, request, t );
                response = (UniqueValueActor.Response) Await.result( fut, t.duration() );

                if ( response != null && (
                        response.getStatus().equals( UniqueValueActor.Response.Status.IS_UNIQUE )
                                || response.getStatus().equals( UniqueValueActor.Response.Status.NOT_UNIQUE ))) {
                    if ( retries > 1 ) {
                        logger.debug("IS_UNIQUE after retrying {} for entity {} rowkey {}",
                                retries, entity.getId().getUuid(), request.getConsistentHashKey());
                    }
                    break;

                } else if ( response != null  ) {
                    logger.debug("ERROR status retrying {} entity {} rowkey {}",
                            retries, entity.getId().getUuid(), request.getConsistentHashKey());
                } else {
                    logger.debug("Timed-out retrying {} entity {} rowkey",
                            retries, entity.getId().getUuid(), request.getConsistentHashKey());
                }

            } catch ( Exception e ) {
                logger.debug("{} caused retry {} for entity {} rowkey {}",
                        e.getClass().getSimpleName(), retries, entity.getId().getUuid(), request.getConsistentHashKey());
            }
        }

        if ( response == null || response.getStatus().equals( UniqueValueActor.Response.Status.ERROR )) {
            logger.debug("ERROR after retrying {} for entity {} rowkey {}",
                    retries, entity.getId().getUuid(), request.getConsistentHashKey());

            // should result in an HTTP 503
            throw new RuntimeException( "Error verifying unique value after " + retries + " retries");
        }

        if ( response.getStatus().equals( UniqueValueActor.Response.Status.NOT_UNIQUE )) {

            // should result in an HTTP 409 (conflict)
            throw new UniqueValueException( "Error property not unique", request.getField() );
        }
    }
}
