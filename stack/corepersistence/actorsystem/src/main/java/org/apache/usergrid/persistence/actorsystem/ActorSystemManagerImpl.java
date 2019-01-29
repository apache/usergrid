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
import akka.cluster.Cluster;
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

import java.net.InetAddress;
import java.net.UnknownHostException;
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

    private ActorSystem clusterSystem = null;


    @Inject
    public ActorSystemManagerImpl( ActorSystemFig actorSystemFig ) {
        this.actorSystemFig = actorSystemFig;
    }


    public Set<String> getRegions() {
        return getSeedsByRegion().keySet();
    }


    /**
     * Init Akka ActorSystems and wait for request actors to start.
     */
    @Override
    public void start() {

        if ( !StringUtils.isEmpty( actorSystemFig.getHostname()) ) {
            this.hostname = actorSystemFig.getHostname();
        } else {
            try {
                this.hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                logger.error("Cannot get hostname, defaulting to 'localhost': " + e.getMessage());
            }
        }

        this.currentRegion = actorSystemFig.getRegionLocal();
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

        publishToLocalRegion(topic, message, sender);
        publishToRemoteRegions(topic, message, sender);
    }
    
    @Override
    public void publishToLocalRegion( String topic, Object message, ActorRef sender  ) {

        // send to local subscribers to topic
        mediator.tell( new DistributedPubSubMediator.Publish( topic, message ), sender );

    }
    
    @Override
    public void publishToRemoteRegions( String topic, Object message, ActorRef sender  ) {

        // send to each ClusterClient
        for ( ActorRef clusterClient : clusterClientsByRegion.values() ) {
            clusterClient.tell( new ClusterClient.Publish( topic, message ), sender );
        }

    }

    private String getClusterName(){
        // better to not change this so rolling restarts of usergrid are unaffected
        return "ClusterSystem";
    }


    private void initAkka() {
        logger.info("Initializing Akka");

        // Create one actor system with request actor for each region

        if ( StringUtils.isEmpty( currentRegion )) {
            throw new RuntimeException( "No value specified for: " + ActorSystemFig.CLUSTER_REGIONS_LOCAL );
        }

        if ( StringUtils.isEmpty( actorSystemFig.getRegionsList() )) {
            throw new RuntimeException( "No value specified for: " + ActorSystemFig.CLUSTER_REGIONS_LIST );
        }

        if ( StringUtils.isEmpty( actorSystemFig.getSeeds() )) {
            throw new RuntimeException( "No value specified for: " + ActorSystemFig.CLUSTER_SEEDS );
        }

        List regionList = Arrays.asList( actorSystemFig.getRegionsList().toLowerCase().split(",") );

        logger.info("Initializing Akka for hostname {} region {} regionList {} seeds {}",
            hostname, currentRegion, regionList, actorSystemFig.getSeeds() );

        Config config = createConfiguration();

        clusterSystem = createClusterSystem( config );

        // register our cluster listener
        clusterSystem.actorOf(Props.create(ClusterListener.class, getSeedsByRegion(), getCurrentRegion()),
            "clusterListener" );

        createClientActors( clusterSystem );

        mediator = DistributedPubSub.get( clusterSystem ).mediator();
    }


    /**
     * Read Usergrid's list of seeds, put them in handy multi-map.
     */
    private ListMultimap<String, String> getSeedsByRegion() {

        if ( seedsByRegion == null ) {

            seedsByRegion = ArrayListMultimap.create();

            String[] regionSeeds = actorSystemFig.getSeeds().split( "," );

            logger.info( "Found region [{}] seeds [{}]", regionSeeds.length, regionSeeds );

            try {

                if (port != null) {

                    // we are testing, create seeds-by-region map for one region, one seed

                    String seed = "akka.tcp://"+getClusterName()+ "@" + hostname + ":" + port;
                    seedsByRegion.put( currentRegion, seed );
                    logger.info( "Akka testing, only starting one seed" );

                } else { // create seeds-by-region map

                    for (String regionSeed : regionSeeds) {

                        String[] parts = regionSeed.split( ":" );
                        String region = parts[0];
                        String hostname = parts[1];

                        String regionPortString = parts.length > 2 ? parts[2] : actorSystemFig.getPort();

                        // all seeds in same region must use same port
                        // we assume 0th seed has the right port
                        final Integer regionPort;

                        if (port == null) {
                            regionPort = Integer.parseInt( regionPortString );
                        } else {
                            regionPort = port; // unless we are testing
                        }

                        String seed = "akka.tcp://"+getClusterName()+ "@" + hostname + ":" + regionPort;

                        logger.info( "Adding seed [{}] for region [{}]", seed, region );

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
    private Config createConfiguration() {

        Config config = null;

        try {

            int numInstancesPerNode = 300; // expect this to be overridden by RouterProducers

            String region = currentRegion;

            List<String> seeds = getSeedsByRegion().get( region );

            logger.info( "Akka Config for region [{}] is:\n" + "   Hostname [{}]\n" + "   Seeds [{}]\n",
                region, hostname, seeds );

            int lastColon = seeds.get(0).lastIndexOf(":") + 1;
            final Integer regionPort = Integer.parseInt( seeds.get(0).substring( lastColon ));

            Map<String, Object> configMap = new HashMap<String, Object>() {{

                put( "akka", new HashMap<String, Object>() {{

                    put( "blocking-io-dispatcher", new HashMap<String, Object>() {{
                        put( "type", "Dispatcher" );
                        put( "executor", actorSystemFig.getClusterIoExecutorType() );
                        put( actorSystemFig.getClusterIoExecutorType() , new HashMap<String, Object>() {{
                            put( "fixed-pool-size", actorSystemFig.getClusterIoExecutorThreadPoolSize() );
                            put( "rejection-policy",actorSystemFig.getClusterIoExecutorRejectionPolicy() );
                        }} );
                    }} );


                    put( "remote", new HashMap<String, Object>() {{
                        put( "netty.tcp", new HashMap<String, Object>() {{
                            put( "hostname", hostname );
                            put( "bind-hostname", hostname );
                            put( "port", regionPort );
                        }} );
                    }} );

                    put( "cluster", new HashMap<String, Object>() {{

                        // this sets default if router does not set
                        put( "max-nr-of-instances-per-node", numInstancesPerNode);

                        put( "roles", Collections.singletonList("io") );
                        put( "seed-nodes", new ArrayList<String>() {{
                            for (String seed : seeds) {
                                add( seed );
                            }
                        }} );
                        put( "failure-detector", new HashMap<String, Object>() {{
                            put( "threshold", "20" );
                            put( "acceptable-heartbeat-pause", "6 s" );
                            put( "heartbeat-interval", "1 s" );
                            put( "heartbeat-request", new HashMap<String, Object>() {{
                                put( "expected-response-after", "3 s" );
                            }} );
                        }} );
                    }} );

                }} );
            }};

            for ( RouterProducer routerProducer : routerProducers ) {
                routerProducer.addConfiguration( configMap );
            }

            logger.debug("Actor system configMap: " + configMap );

            config = ConfigFactory.parseMap( configMap )
                .withFallback( ConfigFactory.load( "application.conf" ) );


        } catch ( Exception e ) {
            throw new RuntimeException("Error reading and adding to cluster config", e );
        }

        return config;
    }


    /**
     * Create cluster system for this the current region
     */
    private synchronized ActorSystem createClusterSystem( Config config ) {

        // there is only 1 akka system for a Usergrid cluster
        final String clusterName = getClusterName();

        if ( clusterSystem == null) {

            logger.info("Class: {}. ActorSystem [{}] not initialized, creating...", this, clusterName);

            clusterSystem = ActorSystem.create( clusterName, config );

            for ( RouterProducer routerProducer : routerProducers ) {
                logger.info("Creating router [{}] for region [{}]", routerProducer.getRouterPath(), currentRegion );
                routerProducer.produceRouter( clusterSystem, "io" );
            }

            for ( RouterProducer routerProducer : routerProducers ) {
                Iterator<Class> messageTypes = routerProducer.getMessageTypes().iterator();
                while ( messageTypes.hasNext() ) {
                    Class messageType = messageTypes.next();
                    logger.info("createClusterSystem: routerProducer {}: message type={}", routerProducer.getRouterPath(), messageType.getName());
                    routersByMessageType.put( messageType, routerProducer.getRouterPath() );
                }
            }

            //add a shutdown hook to clean all actor systems if the JVM exits without the servlet container knowing
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    leaveCluster();
                }
            });

        }

        return clusterSystem;
    }


    /**
     * Create ClientActor for each region.
     */
    private void createClientActors( ActorSystem system ) {

        for ( String region : getSeedsByRegion().keySet() ) {

            if ( currentRegion.equals( region )) {

                logger.info( "Creating clientActor for region [{}]", region );

                // Each clientActor needs to know path to ClusterSingletonProxy and region
                clientActor = system.actorOf(
                    Props.create( ClientActor.class, routersByMessageType ), "clientActor" );

                ClusterClientReceptionist.get(system).registerService( clientActor );

            } else {

                logger.info( "Creating clusterClient for region [{}]", region );

                Set<ActorPath> seedPaths = new HashSet<>(20);
                for ( String seed : getSeedsByRegion().get( region ) ) {
                    seedPaths.add( ActorPaths.fromString( seed + "/system/receptionist") );
                }

                ActorRef clusterClient = system.actorOf( ClusterClient.props(
                    ClusterClientSettings.create(system).withInitialContacts( seedPaths )), "client-"+region);

                clusterClientsByRegion.put( region, clusterClient );
            }
        }
    }


    @Override
    public void waitForClientActor() {

        waitForClientActor( clientActor );
    }

    private void waitForClientActor( ActorRef ra ) {

        logger.info( "Waiting on ClientActor [{}]...", ra.path() );

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
                logger.info( "Waiting for ClientActor [{}] region [{}] for [{}s]", ra.path(), currentRegion, retries );
                Thread.sleep( 1000 );

            } catch (Exception e) {
                logger.error( "Error: Timeout waiting for ClientActor [{}]", ra.path() );
            }
            retries++;
        }

        if (started) {
            logger.info( "ClientActor [{}] has started", ra.path() );
        } else {
            throw new RuntimeException( "ClientActor ["+ra.path()+"] did not start in time, validate that akka seeds are configured properly" );
        }
    }

    @Override
    public void leaveCluster(){

        Cluster cluster = Cluster.get(clusterSystem);
        logger.info("Downing self: {} from cluster: {}", cluster.selfAddress(), clusterSystem.name());
        cluster.leave(cluster.selfAddress());
    }

}
