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
import akka.cluster.client.ClusterClient;
import akka.cluster.singleton.ClusterSingletonManager;
import akka.cluster.singleton.ClusterSingletonManagerSettings;
import akka.cluster.singleton.ClusterSingletonProxy;
import akka.cluster.singleton.ClusterSingletonProxySettings;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.usergrid.persistence.actorsystem.ActorSystemManager;
import org.apache.usergrid.persistence.actorsystem.GuiceActorProducer;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
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

    static Injector          injector;
    UniqueValuesFig          uniqueValuesFig;
    ActorSystemManager       actorSystemManager;
    UniqueValuesTable        table;
    private ReservationCache reservationCache;


    @Inject
    public UniqueValuesServiceImpl(
        Injector inj,
        UniqueValuesFig uniqueValuesFig,
        ActorSystemManager actorSystemManager,
        UniqueValuesTable table ) {

        injector = inj;
        this.actorSystemManager = actorSystemManager;
        this.uniqueValuesFig = uniqueValuesFig;
        this.table = table;

        ReservationCache.init( uniqueValuesFig.getUniqueValueCacheTtl() );
        this.reservationCache = ReservationCache.getInstance();
    }


    @Override
    public String getName() {
        return "UniqueValues ClusterSingleton Router";
    }


    @Override
    public String getRouterPath() {
        return "/user/uvProxy";
    }


    private void subscribeToReservations( ActorSystem localSystem ) {
        logger.info("Starting ReservationCacheUpdater");
        localSystem.actorOf( Props.create( ReservationCacheActor.class ), "subscriber");
    }


    @Override
    public void reserveUniqueValues(
        ApplicationScope scope, Entity entity, UUID version, String region ) throws UniqueValueException {

        ready();

        try {
            for (Field field : entity.getFields()) {
                if (field.isUnique()) {
                    reserveUniqueField( scope, entity, version, field, region );
                }
            }

        } catch ( UniqueValueException e ) {

            for (Field field : entity.getFields()) {
                if (field.isUnique()) {
                    try {
                        cancelUniqueField( scope, entity, version, field, region );
                    } catch (Throwable ignored) {
                        logger.error( "Error canceling unique field", ignored );
                    }
                }
            }
            throw e;
        }
    }


    @Override
    public void confirmUniqueValues(
        ApplicationScope scope, Entity entity, UUID version, String region ) throws UniqueValueException {

        ready();

        try {
            for (Field field : entity.getFields()) {
                if (field.isUnique()) {
                    confirmUniqueField( scope, entity, version, field, region );
                }
            }

        } catch ( UniqueValueException e ) {

            for (Field field : entity.getFields()) {
                if (field.isUnique()) {
                    try {
                        cancelUniqueField( scope, entity, version, field, region );
                    } catch (Throwable ex ) {
                        logger.error( "Error canceling unique field", ex );
                    }
                }
            }
            throw e;
        }

    }


    // TODO: do we need this or can we rely on UniqueCleanup + Cassandra replication?

//    @Override
//    public void releaseUniqueValues(ApplicationScope scope, Id entityId, UUID version, String region)
//        throws UniqueValueException {
//
//        ready();
//
//        TODO: need to replicate logic from UniqueCleanup and make sure it happens in Authoritative Region
//
//        Iterator<UniqueValue> iterator = table.getUniqueValues( scope, entityId );
//
//        while ( iterator.hasNext() ) {
//            UniqueValue uniqueValue = iterator.next();
//            cancelUniqueField( scope, entityId, uniqueValue.getEntityVersion(), uniqueValue.getField(), region );
//        }
//    }


    private void reserveUniqueField(
        ApplicationScope scope, Entity entity, UUID version, Field field, String region ) throws UniqueValueException {

        UniqueValueActor.Request request = new UniqueValueActor.Reservation(
            scope, entity.getId(), version, field );

        UniqueValueActor.Reservation res = reservationCache.get( request.getConsistentHashKey() );
        // if ( res != null ) {
        //    getCacheCounter().inc();
        // }
        if ( res != null && !res.getOwner().equals( request.getOwner() )) {
            throw new UniqueValueException( "Error property not unique (cache)", field);
        }

        sendUniqueValueRequest( entity, region, request );
    }


    private void confirmUniqueField(
        ApplicationScope scope, Entity entity, UUID version, Field field, String region) throws UniqueValueException {

        UniqueValueActor.Confirmation request = new UniqueValueActor.Confirmation(
            scope, entity.getId(), version, field );

        sendUniqueValueRequest( entity, region, request );
    }


    private void cancelUniqueField( ApplicationScope scope,
        Entity entity, UUID version, Field field, String region ) throws UniqueValueException {

        cancelUniqueField( scope, entity.getId(), version, field, region );
    }


    private void cancelUniqueField( ApplicationScope scope,
        Id entityId, UUID version, Field field, String region ) throws UniqueValueException {

        UniqueValueActor.Cancellation request = new UniqueValueActor.Cancellation(
            scope, entityId, version, field );

        if ( actorSystemManager.getCurrentRegion().equals( region ) ) {

            // sending to current region, use local clientActor
            ActorRef clientActor = actorSystemManager.getClientActor();
            clientActor.tell( request, null );

        } else {

            // sending to remote region, send via cluster client for that region
            ActorRef clusterClient = actorSystemManager.getClusterClient( region );
            clusterClient.tell( new ClusterClient.Send("/user/clientActor", request), null );
        }

    }


    private void ready() {
        if ( !actorSystemManager.isReady() ) {
            throw new RuntimeException("Unique values service not initialized, no request actors ready");
        }

        if ( !StringUtils.isEmpty( uniqueValuesFig.getAuthoritativeRegion() )) {
            if ( !actorSystemManager.getRegions().contains( uniqueValuesFig.getAuthoritativeRegion() ) ) {
                throw new RuntimeException( "Authoritative region not in region list" );
            }
        }
    }


    private void sendUniqueValueRequest(
        Entity entity, String region, UniqueValueActor.Request request ) throws UniqueValueException {

        int maxRetries = 5;
        int retries = 0;

        UniqueValueActor.Response response = null;
        while ( retries++ < maxRetries ) {
            try {
                Timeout t = new Timeout( 1, TimeUnit.SECONDS );

                Future<Object> fut;

                if ( actorSystemManager.getCurrentRegion().equals( region ) ) {

                    // sending to current region, use local clientActor
                    ActorRef clientActor = actorSystemManager.getClientActor();
                    fut = Patterns.ask( clientActor, request, t );

                } else {

                    // sending to remote region, send via cluster client for that region
                    ActorRef clusterClient = actorSystemManager.getClusterClient( region );
                    fut = Patterns.ask( clusterClient, new ClusterClient.Send("/user/clientActor", request), t );
                }

                // wait (up to timeout) for response
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


    @Override
    public void createClusterSingletonManager(ActorSystem system) {

        // create cluster singleton supervisor for actor system
        ClusterSingletonManagerSettings settings =
            ClusterSingletonManagerSettings.create( system ).withRole("io");

        system.actorOf( ClusterSingletonManager.props(
            Props.create( GuiceActorProducer.class, injector, UniqueValuesRouter.class ),
            PoisonPill.getInstance(), settings ), "uvRouter" );

    }


    @Override
    public void createClusterSingletonProxy( ActorSystem system, String role ) {

        ClusterSingletonProxySettings proxySettings =
            ClusterSingletonProxySettings.create( system ).withRole( role );

        system.actorOf( ClusterSingletonProxy.props( "/user/uvRouter", proxySettings ), "uvProxy" );
    }


    @Override
    public void createLocalSystemActors( ActorSystem localSystem ) {
        subscribeToReservations( localSystem );
    }


    @Override
    public void addConfiguration( Map<String, Object> configMap ) {

        int numInstancesPerNode = uniqueValuesFig.getUniqueValueInstancesPerNode();

        // TODO: replace this configuration stuff with equivalent Java code in the above "create" methods?

        // be careful not to overwrite configurations that other router producers may have added

        Map<String, Object> akka = (Map<String, Object>) configMap.get( "akka" );
        final Map<String, Object> deploymentMap;

        if ( akka.get( "actor" ) == null ) {

            // nobody has created anything under "actor" yet, so create it now
            deploymentMap = new HashMap<>();
            akka.put( "actor", new HashMap<String, Object>() {{
                put( "deployment", deploymentMap );
            }} );

        } else if (((Map) akka.get( "actor" )).get( "deployment" ) == null) {

            // nobody has created anything under "actor/deployment" yet, so create it now
            deploymentMap = new HashMap<>();
            ((Map) akka.get( "actor" )).put( "deployment", deploymentMap );

        } else {

            // somebody else already created "actor/deployment" config so use it
            deploymentMap = (Map<String, Object>) ((Map) akka.get( "actor" )).get( "deployment" );
        }

        deploymentMap.put( "/uvRouter/singleton/router", new HashMap<String, Object>() {{
            put( "router", "consistent-hashing-pool" );
            put( "cluster", new HashMap<String, Object>() {{
                put( "enabled", "on" );
                put( "allow-local-routees", "on" );
                put( "use-role", "io" );
                put( "max-nr-of-instances-per-node", numInstancesPerNode );
                put( "failure-detector", new HashMap<String, Object>() {{
                    put( "threshold", "10" );
                    put( "acceptable-heartbeat-pause", "3 s" );
                    put( "heartbeat-interval", "1 s" );
                    put( "heartbeat-request", new HashMap<String, Object>() {{
                        put( "expected-response-after", "3 s" );
                    }} );
                }} );
            }} );
        }} );

    }


    @Override
    public Collection<Class> getMessageTypes() {
        List<Class> messageTypes = new ArrayList<>();
        messageTypes.add( UniqueValueActor.Request.class);
        messageTypes.add( UniqueValueActor.Reservation.class);
        messageTypes.add( UniqueValueActor.Cancellation.class);
        messageTypes.add( UniqueValueActor.Confirmation.class);
        return messageTypes;
    }
}
