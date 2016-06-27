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


import akka.actor.*;
import akka.cluster.client.ClusterClient;
import akka.cluster.client.ClusterClientReceptionist;
import akka.cluster.client.ClusterClientSettings;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.inject.Inject;
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

    private boolean started = false;

    private String  hostname;
    private Integer port;
    private String  currentRegion;

    private final ActorSystemFig        actorSystemFig;
    private final List<RouterProducer>  routerProducers = new ArrayList<>();
    private final Map<Class, String>    routersByMessageType = new HashMap<>();
    private final Map<String, ActorRef> clusterClientsByRegion = new HashMap<String, ActorRef>(20);

    private ActorRef                    mediator;

    private ActorRef                    clientActor;

    private ListMultimap<String, String> seedsByRegion;


    @Inject
    public ActorSystemManagerImpl( ActorSystemFig actorSystemFig ) {
        this.actorSystemFig = actorSystemFig;
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
        waitForClientActor();
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
        return started;
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
    public ActorRef getClientActor() {
        return clientActor;
    }


    @Override
    public ActorRef getClusterClient(String region) {
        return clusterClientsByRegion.get( region );
    }


    @Override
    public String getCurrentRegion() {
        return currentRegion;
    }


    @Override
    public void publishToAllRegions( String topic, Object message, ActorRef sender  ) {

        // send to local subscribers to topic
        mediator.tell( new DistributedPubSubMediator.Publish( topic, message ), sender );

        // send to each ClusterClient
        for ( ActorRef clusterClient : clusterClientsByRegion.values() ) {
            clusterClient.tell( new ClusterClient.Publish( topic, message ), sender );
        }

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
            logger.warn("No value for {} specified, will use current region as authoriative region",
                ActorSystemFig.AKKA_AUTHORITATIVE_REGION);
            //throw new RuntimeException( "No value specified for " + ActorSystemFig.AKKA_AUTHORITATIVE_REGION);
        }

        List regionList = Arrays.asList( actorSystemFig.getRegionList().toLowerCase().split(",") );

        logger.info("Initializing Akka for hostname {} region {} regionList {} seeds {}",
            hostname, currentRegion, regionList, actorSystemFig.getRegionSeeds() );

        Config config = readClusterSystemConfig();

        ActorSystem localSystem = createClusterSystemsFromConfigs( config );

        createClientActors( localSystem );

        for ( RouterProducer routerProducer : routerProducers ) {
            routerProducer.createLocalSystemActors( localSystem );
        }

        mediator = DistributedPubSub.get( localSystem ).mediator();
    }


    /**
     * Read Usergrid's list of seeds, put them in handy multi-map.
     */
    private ListMultimap<String, String> getSeedsByRegion() {

        if ( seedsByRegion == null ) {

            seedsByRegion = ArrayListMultimap.create();

            String[] regionSeeds = actorSystemFig.getRegionSeeds().split( "," );

            logger.info( "Found region {} seeds {}", regionSeeds.length, regionSeeds );

            try {

                if (port != null) {

                    // we are testing, create seeds-by-region map for one region, one seed

                    String seed = "akka.tcp://ClusterSystem" + "@" + hostname + ":" + port;
                    seedsByRegion.put( currentRegion, seed );
                    logger.info( "Akka testing, only starting one seed" );

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

                        String seed = "akka.tcp://ClusterSystem" + "@" + hostname + ":" + regionPort;

                        logger.info( "Adding seed {} for region {}", seed, region );

                        seedsByRegion.put( region, seed );
                    }

                    if (seedsByRegion.keySet().isEmpty()) {
                        throw new RuntimeException(
                            "No seeds listed in 'parsing collection.akka.region.seeds' property." );
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException( "Error 'parsing collection.akka.region.seeds' property", e );
            }
        }

        return seedsByRegion;
    }


    /**
     * Read cluster config and add seed nodes to it.
     */
    private Config readClusterSystemConfig() {

        Config config = null;

        try {

            int numInstancesPerNode = actorSystemFig.getInstancesPerNode();

            String region = currentRegion;

            List<String> seeds = getSeedsByRegion().get( region );
            int lastColon = seeds.get(0).lastIndexOf(":") + 1;
            final Integer regionPort = Integer.parseInt( seeds.get(0).substring( lastColon ));

            logger.info( "Akka Config for region {} is:\n" +
                    "   Hostname {}\n" +
                    "   Seeds {}\n" +
                    "   Authoritative Region {}\n",
                region, hostname, seeds, actorSystemFig.getAkkaAuthoritativeRegion() );

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
                        put( "max-nr-of-instances-per-node", numInstancesPerNode);
                        put( "roles", Collections.singletonList("io") );
                        put( "seed-nodes", new ArrayList<String>() {{
                            for (String seed : seeds) {
                                add( seed );
                            }
                        }} );
                    }} );

                }} );
            }};

            for ( RouterProducer routerProducer : routerProducers ) {
                routerProducer.addConfiguration( configMap );
            }

            config = ConfigFactory.parseMap( configMap )
                .withFallback( ConfigFactory.load( "application.conf" ) );


        } catch ( Exception e ) {
            throw new RuntimeException("Error reading and adding to cluster config", e );
        }

        return config;
    }


    /**
     * Create actor system for this region, with cluster singleton manager & proxy.
     */
    private ActorSystem createClusterSystemsFromConfigs( Config config ) {

        ActorSystem system = ActorSystem.create( "ClusterSystem", config );

        for ( RouterProducer routerProducer : routerProducers ) {
            logger.info("Creating {} for region {}", routerProducer.getName(), currentRegion );
            routerProducer.createClusterSingletonManager( system );
        }

        for ( RouterProducer routerProducer : routerProducers ) {
            logger.info("Creating {} proxy for region {} role 'io'", routerProducer.getName(), currentRegion);
            routerProducer.createClusterSingletonProxy( system, "io" );
        }

        return system;
    }


    /**
     * Create RequestActor for each region.
     */
    private void createClientActors( ActorSystem system ) {

        for ( String region : getSeedsByRegion().keySet() ) {

            if ( currentRegion.equals( region )) {

                logger.info( "Creating clientActor for region {}", region );

                // Each clientActor needs to know path to ClusterSingletonProxy and region
                clientActor = system.actorOf(
                    Props.create( ClientActor.class, routersByMessageType ), "clientActor" );

                ClusterClientReceptionist.get(system).registerService( clientActor );

            } else {

                List<String> regionSeeds = getSeedsByRegion().get( region );
                Set<ActorPath> seedPaths = new HashSet<>(20);
                for ( String seed : getSeedsByRegion().get( region ) ) {
                    seedPaths.add( ActorPaths.fromString( seed + "/system/receptionist") );
                }

                ActorRef clusterClient = system.actorOf( ClusterClient.props(
                    ClusterClientSettings.create(system).withInitialContacts( seedPaths )), "client");

                clusterClientsByRegion.put( region, clusterClient );
            }

        }
    }


    @Override
    public void waitForClientActor() {

        waitForClientActor( clientActor );
    }

    private void waitForClientActor( ActorRef ra ) {

        logger.info( "Waiting on request actor {}...", ra.path() );

        started = false;

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
