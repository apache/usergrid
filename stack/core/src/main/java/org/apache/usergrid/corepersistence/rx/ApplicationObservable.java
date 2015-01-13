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

package org.apache.usergrid.corepersistence.rx;


import java.util.Arrays;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.ManagerCache;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;
import rx.functions.Func1;

import static org.apache.usergrid.corepersistence.util.CpNamingUtils.generateApplicationId;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.getApplicationScope;


/**
 * An observable that will emit all application stored in the system.
 */
public class ApplicationObservable {

    private static final Logger logger = LoggerFactory.getLogger( ApplicationObservable.class );

    /**
     * Get all applicationIds as an observable
     */
    public static Observable<Id> getAllApplicationIds( final ManagerCache managerCache ) {

        //emit our 3 hard coded applications that are used the manage the system first.
        //this way consumers can perform whatever work they need to on the root system first


        final Observable<Id> systemIds = Observable.from( Arrays
                .asList( generateApplicationId( CpNamingUtils.DEFAULT_APPLICATION_ID ),
                        generateApplicationId( CpNamingUtils.MANAGEMENT_APPLICATION_ID ),
                        generateApplicationId( CpNamingUtils.SYSTEM_APP_ID ) ) );


        final ApplicationScope appScope = getApplicationScope( CpNamingUtils.SYSTEM_APP_ID );

        final CollectionScope appInfoCollectionScope =
                new CollectionScopeImpl( appScope.getApplication(), appScope.getApplication(),
                        CpNamingUtils.getCollectionScopeNameFromCollectionName( CpNamingUtils.APPINFOS ) );

        final EntityCollectionManager collectionManager =
                managerCache.getEntityCollectionManager( appInfoCollectionScope );


        final GraphManager gm = managerCache.getGraphManager( appScope );


        String edgeType = CpNamingUtils.getEdgeTypeFromCollectionName( CpNamingUtils.APPINFOS );

        Id rootAppId = appScope.getApplication();


        //we have app infos.  For each of these app infos, we have to load the application itself
        Observable<Id> appIds = gm.loadEdgesFromSource(
                new SimpleSearchByEdgeType( rootAppId, edgeType, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
                        null ) ).flatMap( new Func1<Edge, Observable<Id>>() {
            @Override
            public Observable<Id> call( final Edge edge ) {
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
                        .map( new Func1<org.apache.usergrid.persistence.model.entity.Entity, Id>() {


                            @Override
                            public Id call( final org.apache.usergrid.persistence.model.entity.Entity entity ) {

                                final UUID uuid = ( UUID ) entity.getField( "applicationUuid" ).getValue();

                                return CpNamingUtils.generateApplicationId( uuid );
                            }
                        } );
            }
        } );

        return Observable.merge( systemIds, appIds );
    }
}
