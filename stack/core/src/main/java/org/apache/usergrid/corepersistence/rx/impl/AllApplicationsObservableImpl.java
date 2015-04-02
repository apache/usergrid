/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.rx.impl;


import java.util.Arrays;
import java.util.UUID;

import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.utils.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.AllApplicationsObservable;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.getApplicationScope;


/**
 * An observable that will emit all application stored in the system.
 */
public class AllApplicationsObservableImpl implements AllApplicationsObservable {

    private static final Logger logger = LoggerFactory.getLogger( AllApplicationsObservableImpl.class );
    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final GraphManagerFactory graphManagerFactory;

    @Inject
    public AllApplicationsObservableImpl( EntityCollectionManagerFactory entityCollectionManagerFactory,
                                          GraphManagerFactory graphManagerFactory ){

        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.graphManagerFactory = graphManagerFactory;
    }



    @Override
    public Observable<ApplicationScope> getData() {

        //emit our hard coded applications that are used the manage the system first.
        //this way consumers can perform whatever work they need to on the root system first
        final Observable<ApplicationScope> systemIds = Observable.from(
            Arrays.asList(
                getApplicationScope( CpNamingUtils.MANAGEMENT_APPLICATION_ID ),
                getApplicationScope( CpNamingUtils.SYSTEM_APP_ID ))); // still need deprecated system app here

        final ApplicationScope appScope = getApplicationScope( CpNamingUtils.MANAGEMENT_APPLICATION_ID );

        final EntityCollectionManager collectionManager =
                entityCollectionManagerFactory.createCollectionManager(  appScope );

        final GraphManager gm = graphManagerFactory.createEdgeManager(appScope);

        String edgeType = CpNamingUtils.getEdgeTypeFromCollectionName( CpNamingUtils.APPLICATION_INFOS );

        Id rootAppId = appScope.getApplication();


        //we have app infos.  For each of these app infos, we have to load the application itself
        Observable<ApplicationScope> appIds = gm.loadEdgesFromSource(
                new SimpleSearchByEdgeType( rootAppId, edgeType, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
                        null ) ).flatMap( new Func1<Edge, Observable<ApplicationScope>>() {
            @Override
            public Observable<ApplicationScope> call( final Edge edge ) {

                //get the app info and load it
                final Id appInfo = edge.getTargetNode();

                return collectionManager.load( appInfo )
                    //filter out null entities
                    .filter( new Func1<Entity, Boolean>() {
                        @Override
                        public Boolean call( final Entity entity ) {
                            if ( entity == null ) {
                                logger.warn( "Encountered a null application info for id {}", appInfo );
                                return false;
                            }

                            return true;
                        }
                    } )
                        //get the id from the entity
                    .map( new Func1<Entity, ApplicationScope>() {

                        @Override
                        public ApplicationScope call( final Entity entity ) {
                            final UUID uuid = UUIDUtils.tryExtractUUID(
                                entity.getField( Schema.PROPERTY_APPLICATION_ID ).getValue().toString());
                            return getApplicationScope( uuid );
                        }
                    } );
            }
        } );

        return Observable.merge( systemIds, appIds );
    }



}
