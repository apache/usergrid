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

package org.apache.usergrid.corepersistence.pipeline.read;


import com.google.inject.Singleton;


@Singleton
public class ReadFilterFactoryImpl { //implements ReadFilterFactory {

//
//    private final GraphManagerFactory graphManagerFactory;
//    private final EntityIndexFactory entityIndexFactory;
//    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
//
//
//    @Inject
//    public ReadFilterFactoryImpl( final GraphManagerFactory graphManagerFactory,
//                                  final EntityIndexFactory entityIndexFactory,
//                                  final EntityCollectionManagerFactory entityCollectionManagerFactory ) {
//
//
//        this.graphManagerFactory = graphManagerFactory;
//        this.entityIndexFactory = entityIndexFactory;
//        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
//    }
//
//
//    @Override
//    public ReadGraphCollectionFilter readGraphCollectionCommand( final String collectionName ) {
//        return new ReadGraphCollectionFilter( graphManagerFactory, collectionName );
//    }
//
//
//    @Override
//    public ReadGraphCollectionByIdFilter readGraphCollectionByIdFilter( final String collectionName,
//                                                                        final Id targetId ) {
//        return new ReadGraphCollectionByIdFilter( graphManagerFactory, collectionName, targetId );
//    }
//
//
//    @Override
//    public ReadGraphConnectionFilter readGraphConnectionCommand( final String connectionName ) {
//        return new ReadGraphConnectionFilter( graphManagerFactory, connectionName );
//    }
//
//
//    @Override
//    public ReadGraphConnectionByTypeFilter readGraphConnectionCommand( final String connectionName,
//                                                                       final String entityType ) {
//        return new ReadGraphConnectionByTypeFilter( graphManagerFactory, connectionName, entityType );
//    }
//
//
//    @Override
//    public ReadGraphConnectionByIdFilter readGraphConnectionByIdFilter( final String connectionName,
//                                                                        final Id targetId ) {
//        return new ReadGraphConnectionByIdFilter( graphManagerFactory, connectionName, targetId );
//    }
//
//
//    @Override
//    public EntityLoadCollector entityLoadCollector() {
//        return new EntityLoadCollector( entityCollectionManagerFactory );
//    }
//
//
//    /**
//     * TODO refactor these impls to use RX internally, as well as remove the query object
//     */
//    @Override
//    public QueryCollectionElasticSearchCollectorFilter queryCollectionElasticSearchCollector(
//        final String collectionName, final String query ) {
//
//        final Query queryObject = Query.fromQL( query );
//
//        final QueryCollectionElasticSearchCollectorFilter filter =
//            new QueryCollectionElasticSearchCollectorFilter( entityCollectionManagerFactory, entityIndexFactory,
//                collectionName, queryObject );
//
//        return filter;
//    }
//
//
//    @Override
//    public QueryConnectionElasticSearchCollectorFilter queryConnectionElasticSearchCollector(
//        final String connectionName, final String query ) {
//
//        final Query queryObject = Query.fromQL( query );
//
//        final QueryConnectionElasticSearchCollectorFilter filter =
//            new QueryConnectionElasticSearchCollectorFilter( entityCollectionManagerFactory, entityIndexFactory,
//                connectionName, queryObject );
//
//        return filter;
//    }
//
//
//    @Override
//    public QueryConnectionElasticSearchCollectorFilter queryConnectionElasticSearchCollector(
//        final String connectionName, final String connectionEntityType, final String query ) {
//
//        final Query queryObject = Query.fromQL( query );
//        queryObject.setConnectionType( connectionEntityType );
//
//        final QueryConnectionElasticSearchCollectorFilter filter =
//            new QueryConnectionElasticSearchCollectorFilter( entityCollectionManagerFactory, entityIndexFactory,
//                connectionName, queryObject );
//
//        return filter;
//    }
//
//
//    @Override
//    public EntityIdFilter getEntityIdFilter( final Id entityId ) {
//        return new EntityIdFilter( entityId );
//    }
}
