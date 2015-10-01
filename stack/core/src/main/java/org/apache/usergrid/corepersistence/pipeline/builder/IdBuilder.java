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

package org.apache.usergrid.corepersistence.pipeline.builder;


import org.apache.usergrid.corepersistence.pipeline.PipelineOperation;
import org.apache.usergrid.corepersistence.pipeline.read.FilterFactory;
import org.apache.usergrid.corepersistence.pipeline.Pipeline;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.corepersistence.pipeline.read.ResultsPage;
import org.apache.usergrid.corepersistence.pipeline.read.collect.ConnectionRefFilter;
import org.apache.usergrid.corepersistence.pipeline.read.collect.ConnectionRefResumeFilter;
import org.apache.usergrid.corepersistence.pipeline.read.collect.IdResumeFilter;
import org.apache.usergrid.corepersistence.pipeline.read.collect.ResultsPageCollector;
import org.apache.usergrid.corepersistence.pipeline.read.search.Candidate;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;

import rx.Observable;


/**
 * A builder to transition from emitting Ids in the pipeline into other operations
 */
public class IdBuilder {


    private final FilterFactory filterFactory;
    private final Pipeline<FilterResult<Id>> pipeline;


    public IdBuilder( final Pipeline<FilterResult<Id>> pipeline, final FilterFactory filterFactory ) {
        this.pipeline = pipeline;
        this.filterFactory = filterFactory;
    }


    /**
     * Load all the ids we encounter when traversing the graph as entities
     * @return
     */
    public EntityBuilder loadEntities() {
        final Pipeline<FilterResult<Entity>> pipeline =
            this.pipeline.withFilter( filterFactory.entityLoadFilter() );

        return new EntityBuilder( pipeline );
    }


    /**
     * Traverse all the collection edges from our input Id
     * @param collectionName
     * @return
     */
    public IdBuilder traverseCollection( final String collectionName ) {
        final Pipeline<FilterResult<Id>> newFilter =
            pipeline.withFilter( filterFactory.readGraphCollectionFilter( collectionName ) );

        return new IdBuilder( newFilter, filterFactory );
    }


    /**
     * Traverse all connection edges from our input Id
     * @param connectionName The name of the connection
     * @param entityType The optional type of the entity
     * @return
     */
    public IdBuilder traverseConnection( final String connectionName, final Optional<String> entityType ) {

        final PipelineOperation<FilterResult<Id>, FilterResult<Id>> filter;

        if(entityType.isPresent()){
            filter = filterFactory.readGraphConnectionByTypeFilter( connectionName, entityType.get() );
        }else{
            filter = filterFactory.readGraphConnectionFilter( connectionName );
        }


        return new IdBuilder( pipeline.withFilter(filter ), filterFactory );
    }


    /**
     * Search all collections from our inputId with the specified criteria
     * @param collectionName  The name of the collection
     * @param ql The user's query to execute
     * @param entityType The type of the entity
     * @return  Candidate results
     */
    public CandidateBuilder searchCollection( final String collectionName, final String ql, final String entityType  ) {

        final Pipeline<FilterResult<Candidate>> newFilter = pipeline.withFilter( filterFactory.searchCollectionFilter(
            ql, collectionName, entityType ) );

        return new CandidateBuilder( newFilter, filterFactory );
    }


    /**
     * Search all connections from our input Id and search their connections
     * @param connectionName The connection name to search
     * @param ql The query to execute
     * @param entityType The optional type of entity.  If this is absent, all entity types in the connection will be searched
     * @return  Candidate results
     */
    public CandidateBuilder searchConnection( final String connectionName, final String ql ,  final Optional<String> entityType) {


        final Pipeline<FilterResult<Candidate>> newFilter = pipeline.withFilter( filterFactory.searchConnectionFilter(
            ql, connectionName, entityType ) );

        return new CandidateBuilder( newFilter, filterFactory );
    }


    /**
     * Create connection refs from our ids.  This is a legacy operation
     * @param sourceId
     * @param connectionType
     * @return
     */
    @Deprecated
    public ConnectionRefBuilder loadConnectionRefs(final Id sourceId, final String connectionType){

        final Pipeline<FilterResult<ConnectionRef>> connectionRefFilter = pipeline.withFilter( new ConnectionRefFilter(sourceId, connectionType  ) ).withFilter(
            new ConnectionRefResumeFilter() );
        return new ConnectionRefBuilder(connectionRefFilter);
    }

}
