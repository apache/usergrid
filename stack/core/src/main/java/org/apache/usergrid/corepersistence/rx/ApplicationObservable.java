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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.NamingUtils;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;

import static org.apache.usergrid.corepersistence.NamingUtils.generateApplicationId;
import static org.apache.usergrid.corepersistence.NamingUtils.getApplicationScope;


/**
 * An observable that will emit all application stored in the system.
 */
public class ApplicationObservable implements Observable.OnSubscribe<Id> {

    private static final Logger logger = LoggerFactory.getLogger( ApplicationObservable.class );

    private final GraphManagerFactory graphManagerFactory;


    public ApplicationObservable( final GraphManagerFactory graphManagerFactory ) {
        this.graphManagerFactory = graphManagerFactory;
    }


    @Override
    public void call( final Subscriber<? super Id> subscriber ) {


        //emit our 3 hard coded applications that are used the manage the system first.
        //this way consumers can perform whatever work they need to on the root system first
        emit( generateApplicationId( NamingUtils.DEFAULT_APPLICATION_ID ), subscriber );
        emit( generateApplicationId( NamingUtils.MANAGEMENT_APPLICATION_ID ), subscriber );
        emit( generateApplicationId( NamingUtils.SYSTEM_APP_ID ), subscriber );


        ApplicationScope appScope = getApplicationScope( NamingUtils.SYSTEM_APP_ID );
        GraphManager gm = graphManagerFactory.createEdgeManager( appScope );

        String edgeType = CpNamingUtils.getEdgeTypeFromCollectionName( "appinfos" );

        Id rootAppId = appScope.getApplication();


        Observable<Edge> edges = gm.loadEdgesFromSource(
                new SimpleSearchByEdgeType( rootAppId, edgeType, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
                        null ) );


        final int count = edges.doOnNext( new Action1<Edge>() {
            @Override
            public void call( final Edge edge ) {
                Id applicationId = edge.getTargetNode();


                logger.debug( "Emitting applicationId of {}", applicationId );

                emit( applicationId, subscriber );
            }
        } )
                //if we don't want the count, not sure we need to block.  We may just need to subscribe
                .count().toBlocking().last();

        logger.debug( "Emitted {} application ids", count );
    }


    /**
     * Return false if no more items should be emitted, true otherwise
     */
    private boolean emit( final Id appId, final Subscriber<? super Id> subscriber ) {

        if ( subscriber.isUnsubscribed() ) {
            return false;
        }

        try {
            subscriber.onNext( appId );
        }
        catch ( Throwable t ) {
            subscriber.onError( t );
        }

        return true;
    }
}
