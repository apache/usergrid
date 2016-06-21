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
package org.apache.usergrid.persistence.actorsystem;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.*;
import java.util.concurrent.TimeUnit;


@Singleton
public class ActorSystemManagerImpl implements ActorSystemManager {
    private static final Logger logger = LoggerFactory.getLogger( ActorSystemManagerImpl.class );

    private String  hostname;
    private Integer port;
    private String  currentRegion;

    private static Injector             injector;
    private final ActorSystemFig        actorSystemFig;
    private final Map<String, ActorRef> requestActorsByRegion;
    private final List<RouterProducer>  routerProducers = new ArrayList<>();
    private final Map<Class, String>    routersByMessageType = new HashMap<>();


    @Inject
    public ActorSystemManagerImpl(Injector inj, ActorSystemFig actorSystemFig) {
        injector = inj;
        this.actorSystemFig = actorSystemFig;
        this.requestActorsByRegion = new HashMap<>();
    }


    /**
     * Init Akka ActorSystems and wait for request actors to start.
     */
    @Override
    public void start() {

        this.hostname = actorSystemFig.getHostname();
        this.currentRegion = actorSystemFig.getRegion();
        this.port = null;

        initAkka();
        waitForRequestActors();
    }


    /**
     * For testing purposes only; does not wait for request actors to start.
     */
    @Override
    public void start(String hostname, Integer port, String currentRegion) {

        this.hostname = hostname;
        this.currentRegion = currentRegion;
        this.port = port;

        initAkka();
    }


    @Override
    public boolean isReady() {
        return !getRequestActorsByRegion().isEmpty();
    }


    @Override
    public void registerRouterProducer(RouterProducer routerProducer) {
        routerProducers.add( routerProducer );
    }


    @Override
    public void registerMessageType(Class messageType, String routerPath) {
        routersByMessageType.put( messageType, routerPath );
    }


    @Override
    public ActorRef getClientActor(String region) {
        return getRequestActorsByRegion().get( region );
    }


    private Map<String, ActorRef> getRequestActorsByRegion() {
        return requestActorsByRegion;
    }


    private void initAkka() {
        logger.info("Initializing Akka");

        // Create one actor system with request actor for each region

        if ( StringUtils.isEmpty( hostname )) {
            throw new RuntimeException( "No value specified for " + ActorSystemFig.AKKA_HOSTNAME );
        }

        if ( StringUtils.isEmpty( currentRegion )) {
            throw new RuntimeException( "No value specified for " + ActorSystemFig.AKKA_REGION );
        }

        if ( StringUtils.isEmpty( actorSystemFig.getRegionList() )) {
            throw new RuntimeException( "No value specified for " + ActorSystemFig.AKKA_REGION_LIST );
        }

        if ( StringUtils.isEmpty( actorSystemFig.getRegionSeeds() )) {
            throw new RuntimeException( "No value specified for " + ActorSystemFig.AKKA_REGION_SEEDS);
        }

        if ( StringUtils.isEmpty( actorSystemFig.getAkkaAuthoritativeRegion() )) {
            throw new RuntimeException( "No value specified for " + ActorSystemFig.AKKA_AUTHORITATIVE_REGION);
        }

        List regionList = Arrays.asList( actorSystemFig.getRegionList().toLowerCase().split(",") );

        logger.info("Initializing Akka for hostname {} region {} regionList {} seeds {}",
            hostname, currentRegion, regionList, actorSystemFig.getRegionSeeds() );

        final Map<String, ActorSystem> systemMap = new HashMap<>();

        Map<String, Config> configMap = readClusterSingletonConfigs();

        ActorSystem localSystem = createClusterSingletonProxies( configMap, systemMap );

        createRequestActors( systemMap );

        for ( RouterProducer routerProducer : routerProducers ) {
            routerProducer.createLocalSystemActors( localSystem, systemMap );
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

        String[] regionSeeds = actorSystemFig.getRegionSeeds().split( "," );

        logger.info("Found region {} seeds {}", regionSeeds.length, regionSeeds);

        try {

            if ( port != null ) {

                // we are testing, create seeds-by-region map for one region, one seed

                String seed = "akka.tcp://ClusterSystem@" + hostname + ":" + port;
                seedsByRegion.put( currentRegion, seed );
                logger.info("Akka testing, only starting one seed");

            } else { // create seeds-by-region map

                for (String regionSeed : regionSeeds) {

                    String[] parts = regionSeed.split( ":" );
                    String region = parts[0];
                    String hostname = parts[1];
                    String regionPortString = parts[2];

                    // all seeds in same region must use same port
                    // we assume 0th seed has the right port
                    final Integer regionPort;

                    if (port == null) {
                        regionPort = Integer.parseInt( regionPortString );
                    } else {
                        regionPort = port; // unless we are testing
                    }

                    String seed = "akka.tcp://ClusterSystem@" + hostname + ":" + regionPort;

                    logger.info("Adding seed {} for region {}", seed, region );

                    seedsByRegion.put( region, seed );
                }

                if (seedsByRegion.keySet().isEmpty()) {
                    throw new RuntimeException(
                        "No seeds listed in 'parsing collection.akka.region.seeds' property." );
                }
            }

            int numInstancesPerNode = actorSystemFig.getUniqueValueActors();

            // read config file once for each region

            for ( String region : seedsByRegion.keySet() ) {

                List<String> seeds = seedsByRegion.get( region );
                int lastColon = seeds.get(0).lastIndexOf(":") + 1;
                final Integer regionPort = Integer.parseInt( seeds.get(0).substring( lastColon ));

                // cluster singletons only run role "io" nodes and NOT on "client" nodes of other regions
                String clusterRole = currentRegion.equals( region ) ? "io" : "client";

                logger.info( "Config for region {} is:\n" +
                        "   Akka Hostname {}\n" +
                        "   Akka Seeds {}\n" +
                        "   Akka Port {}\n" +
                        "   Akka UniqueValueActors per node {}\n" +
                        "   Akka Authoritative Region {}",
                    region, hostname, seeds, port, numInstancesPerNode, actorSystemFig.getAkkaAuthoritativeRegion() );

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

                for ( RouterProducer routerProducer : routerProducers ) {
                    routerProducer.createClusterSingletonManager( system );
                }
            }

            for ( RouterProducer routerProducer : routerProducers ) {
                routerProducer.createClusterSingletonProxy( system );
            }
        }

        return localSystem;
    }


    /**
     * Create RequestActor for each region.
     *
     * @param systemMap Map of regions to ActorSystems.
     */
    private void createRequestActors( Map<String, ActorSystem> systemMap ) {

        for ( String region : systemMap.keySet() ) {

            logger.info("Creating request actor for region {}", region);

            // Each RequestActor needs to know path to ClusterSingletonProxy and region
            ActorRef requestActor = systemMap.get( region ).actorOf(
                //Props.create( ClientActor.class, "/user/uvProxy" ), "requestActor" );
                Props.create( ClientActor.class, routersByMessageType ), "requestActor" );

            requestActorsByRegion.put( region, requestActor );
        }
    }


    @Override
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

            Future<Object> fut = Patterns.ask( ra, new ClientActor.StatusRequest(), t );
            try {
                ClientActor.StatusMessage result = (ClientActor.StatusMessage) Await.result( fut, t.duration() );

                if (result.getStatus().equals( ClientActor.StatusMessage.Status.READY )) {
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

}
