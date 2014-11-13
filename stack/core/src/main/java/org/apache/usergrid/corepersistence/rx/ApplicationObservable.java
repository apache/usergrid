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
import rx.functions.Func1;

import static org.apache.usergrid.corepersistence.NamingUtils.generateApplicationId;
import static org.apache.usergrid.corepersistence.NamingUtils.getApplicationScope;


/**
 * An observable that will emit all application stored in the system.
 */
public class ApplicationObservable  {



    /**
     * Get all applicationIds as an observable
     * @param graphManagerFactory
     * @return
     */
    public static Observable<Id> getAllApplicationIds( final GraphManagerFactory graphManagerFactory ) {

        //emit our 3 hard coded applications that are used the manage the system first.
        //this way consumers can perform whatever work they need to on the root system first


       final Observable<Id> systemIds =  Observable.from( Arrays.asList( generateApplicationId( NamingUtils.DEFAULT_APPLICATION_ID ),
                generateApplicationId( NamingUtils.MANAGEMENT_APPLICATION_ID ),
                generateApplicationId( NamingUtils.SYSTEM_APP_ID ) ) );




        ApplicationScope appScope = getApplicationScope( NamingUtils.SYSTEM_APP_ID );
        GraphManager gm = graphManagerFactory.createEdgeManager( appScope );

        String edgeType = CpNamingUtils.getEdgeTypeFromCollectionName( "appinfos" );

        Id rootAppId = appScope.getApplication();


        Observable<Id> appIds = gm.loadEdgesFromSource(
                new SimpleSearchByEdgeType( rootAppId, edgeType, Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
                        null ) ).map( new Func1<Edge, Id>() {
            @Override
            public Id call( final Edge edge ) {
                return edge.getTargetNode();
            }
        } );

        return Observable.merge( systemIds, appIds);
    }


}
