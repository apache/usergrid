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
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;
import org.apache.usergrid.persistence.actorsystem.ActorSystemManager;
import org.apache.usergrid.persistence.actorsystem.GuiceActorProducer;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Singleton
public class UniqueValuesServiceImpl implements UniqueValuesService {
    private static final Logger logger = LoggerFactory.getLogger( UniqueValuesServiceImpl.class );

    static Injector          injector;
    ActorSystemFig           actorSystemFig;
    ActorSystemManager       actorSystemManager;
    UniqueValuesTable        table;
    private ReservationCache reservationCache;


    @Inject
    public UniqueValuesServiceImpl(
        Injector inj,
        ActorSystemFig actorSystemFig,
        ActorSystemManager actorSystemManager,
        UniqueValuesTable table ) {

        injector = inj;
        this.actorSystemManager = actorSystemManager;
        this.actorSystemFig = actorSystemFig;
        this.table = table;

        ReservationCache.init( actorSystemFig.getUniqueValueCacheTtl() );
        this.reservationCache = ReservationCache.getInstance();
    }


    private void subscribeToReservations( ActorSystem localSystem, Map<String, ActorSystem> systemMap ) {

        for ( String region : systemMap.keySet() ) {
            ActorSystem actorSystem = systemMap.get( region );
            if ( !actorSystem.equals( localSystem ) ) {
                logger.info("Starting ReservationCacheUpdater for {}", region );
                actorSystem.actorOf( Props.create( ReservationCacheActor.class ), "subscriber");
            }
        }
    }


    @Override
    public void reserveUniqueValues(
        ApplicationScope scope, Entity entity, UUID version, String region ) throws UniqueValueException {

        if ( !actorSystemManager.isReady() ) {
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
                if (field.isUnique()) {
                    try {
                        cancelUniqueField( scope, entity, version, field, region );
                    } catch (Throwable ignored) {
                        logger.debug( "Error canceling unique field", ignored );
                    }
                }
            }
            throw e;
        }

    }


    @Override
    public void confirmUniqueValues(
        ApplicationScope scope, Entity entity, UUID version, String region ) throws UniqueValueException {

        if ( !actorSystemManager.isReady() ) {
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


    private void reserveUniqueField(
        ApplicationScope scope, Entity entity, UUID version, Field field, String region ) throws UniqueValueException {

        final ActorRef requestActor = actorSystemManager.getClientActor( region );

        if ( requestActor == null ) {
            throw new RuntimeException( "No request actor for region " + region);
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

        final ActorRef requestActor = actorSystemManager.getClientActor( region );

        if ( requestActor == null ) {
            throw new RuntimeException( "No request actor for type, cannot verify unique fields!" );
        }

        UniqueValueActor.Confirmation request = new UniqueValueActor.Confirmation(
            scope, entity.getId(), version, field );

        sendUniqueValueRequest( entity, requestActor, request );
    }


    private void cancelUniqueField(
        ApplicationScope scope, Entity entity, UUID version, Field field, String region ) throws UniqueValueException {

        final ActorRef requestActor = actorSystemManager.getClientActor( region );

        if ( requestActor == null ) {
            throw new RuntimeException( "No request actor for type, cannot verify unique fields!" );
        }

        UniqueValueActor.Cancellation request = new UniqueValueActor.Cancellation(
            scope, entity.getId(), version, field );

        requestActor.tell( request, null );
    }


//    private ActorRef lookupRequestActorForType( String type ) {
//        final String region = getRegionsByType().get( type );
//        ActorRef requestActor = getRequestActorsByRegion().get( region == null ? currentRegion : region );
//        if ( requestActor == null ) {
//            throw new RuntimeException( "No request actor available for region: " + region );
//        }
//        return requestActor;
//    }


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


    @Override
    public void createClusterSingletonManager(ActorSystem system) {

        // create cluster singleton supervisor for actor system
        ClusterSingletonManagerSettings settings =
            ClusterSingletonManagerSettings.create( system ).withRole("io");

        system.actorOf( ClusterSingletonManager.props(
            //Props.create( ClusterSingletonRouter.class, table ),
            Props.create( GuiceActorProducer.class, injector, UniqueValuesRouter.class),
            PoisonPill.getInstance(), settings ), "uvRouter");
    }


    @Override
    public void createClusterSingletonProxy(ActorSystem system) {

        ClusterSingletonProxySettings proxySettings =
            ClusterSingletonProxySettings.create( system ).withRole("io");

        system.actorOf( ClusterSingletonProxy.props( "/user/uvRouter", proxySettings ), "uvProxy" );
    }


    @Override
    public void createLocalSystemActors( ActorSystem localSystem, Map<String, ActorSystem> systemMap ) {
        subscribeToReservations( localSystem, systemMap );
    }
}
