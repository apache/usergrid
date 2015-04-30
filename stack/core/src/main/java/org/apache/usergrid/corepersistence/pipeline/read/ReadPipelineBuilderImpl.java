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


import java.util.ArrayList;
import java.util.List;

import org.apache.usergrid.corepersistence.pipeline.Pipeline;
import org.apache.usergrid.corepersistence.pipeline.read.elasticsearch.CandidateEntityFilter;
import org.apache.usergrid.corepersistence.pipeline.read.graph.EntityLoadFilter;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import rx.Observable;


/**
 * An implementation of our builder for piplines
 */
public class ReadPipelineBuilderImpl implements ReadPipelineBuilder {

    private static final int DEFAULT_LIMIT = 10;

    private final FilterFactory filterFactory;

    private final CollectorState collectorState;

    private final ApplicationScope applicationScope;

    private final CollectorFactory collectorFactory;


    /**
     * Our pointer to our collect filter. Set or cleared with each operation that's performed so the correct results are
     * rendered
     */
    private List<Filter> filters;


    private Optional<String> cursor;
    private int limit;


    @Inject
    public ReadPipelineBuilderImpl( final FilterFactory filterFactory, final CollectorFactory collectorFactory,
                                    @Assisted final ApplicationScope applicationScope ) {
        this.filterFactory = filterFactory;

        this.applicationScope = applicationScope;
        this.collectorFactory = collectorFactory;

        //init our cursor to empty
        this.cursor = Optional.absent();

        //set the default limit
        this.limit = DEFAULT_LIMIT;


        this.collectorState = new CollectorState( );

        this.filters = new ArrayList<>();
    }


    @Override
    public ReadPipelineBuilder withCursor( final Optional<String> cursor ) {
        Preconditions.checkNotNull( cursor, "cursor must not be null" );
        this.cursor = cursor;
        return this;
    }


    @Override
    public ReadPipelineBuilder withLimit( final Optional<Integer> limit ) {
        Preconditions.checkNotNull( limit, "limit must not be null" );
        this.limit = limit.or( DEFAULT_LIMIT );
        return this;
    }


    @Override
    public ReadPipelineBuilder setStartId( final Id id ) {
        ValidationUtils.verifyIdentity( id );

        filters.add( filterFactory.getEntityIdFilter( id ) );

        this.collectorState.clear();


        return this;
    }


    @Override
    public ReadPipelineBuilder getEntityViaCollection( final String collectionName, final Id entityId ) {
        Preconditions.checkNotNull( collectionName, "collectionName must not be null" );
        ValidationUtils.verifyIdentity( entityId );

        filters.add( filterFactory.readGraphCollectionByIdFilter( collectionName, entityId ) );

        this.collectorState.setIdEntityLoaderFilter();

        return this;
    }


    @Override
    public ReadPipelineBuilder getCollection( final String collectionName ) {
        Preconditions.checkNotNull( collectionName, "collectionName must not be null" );

        filters.add( filterFactory.readGraphCollectionFilter( collectionName ) );

        this.collectorState.setIdEntityLoaderFilter();

        return this;
    }


    @Override
    public ReadPipelineBuilder getCollectionWithQuery( final String collectionName, final String entityType,  final String query ) {
        Preconditions.checkNotNull( collectionName, "collectionName must not be null" );
        Preconditions.checkNotNull( query, "query must not be null" );

        //TODO, this should really be 2 a TraverseFilter with an entityLoad collector

        filters.add( filterFactory.elasticSearchCollectionFilter( query, collectionName, entityType ) );

        this.collectorState.setCandidateEntityFilter();

        return this;
    }


    @Override
    public ReadPipelineBuilder getEntityViaConnection( final String connectionName, final Id entityId ) {
        Preconditions.checkNotNull( connectionName, "connectionName must not be null" );
        ValidationUtils.verifyIdentity( entityId );

        filters.add( filterFactory.readGraphConnectionByIdFilter( connectionName, entityId ) );
        collectorState.setIdEntityLoaderFilter();

        return this;
    }


    @Override
    public ReadPipelineBuilder getConnection( final String connectionName ) {
        Preconditions.checkNotNull( connectionName, "connectionName must not be null" );
        filters.add( filterFactory.readGraphConnectionFilter( connectionName ) );
        collectorState.setIdEntityLoaderFilter();

        return this;
    }


    @Override
    public ReadPipelineBuilder getConnection( final String connectionName, final String entityType ) {
        Preconditions.checkNotNull( connectionName, "connectionName must not be null" );
        Preconditions.checkNotNull( connectionName, "entityType must not be null" );

        filters.add( filterFactory.readGraphConnectionByTypeFilter( connectionName, entityType ) );

        collectorState.setIdEntityLoaderFilter();
        return this;
    }


    @Override
    public ReadPipelineBuilder getConnectionWithQuery( final String connectionName, final Optional<String> entityType,
                                                       final String query ) {

        Preconditions.checkNotNull( connectionName, "connectionName must not be null" );
        Preconditions.checkNotNull( connectionName, "entityType must not be null" );
        Preconditions.checkNotNull( query, "query must not be null" );

        filters.add( filterFactory.elasticSearchConnectionFilter( query, connectionName, entityType ) );
        collectorState.setCandidateEntityFilter();
        return this;
    }


    @Override
    public Observable<ResultsPage> execute() {

        ValidationUtils.validateApplicationScope( applicationScope );


        //add our last filter that will generate entities
        final Filter<?, Entity> finalFilter = collectorState.getFinalFilter();

        filters.add( finalFilter );


        //execute our collector
        final Collector<?, ResultsPage> collector = collectorFactory.getResultsPageCollector();

        Preconditions.checkNotNull( collector,
            "You have not specified an operation that creates a collection filter.  This is required for loading "
                + "results" );


        Preconditions.checkNotNull( cursor, "A cursor should be initialized even if absent" );

        Preconditions.checkArgument( limit > 0, "limit must be > than 0" );


        Pipeline pipeline = new Pipeline( applicationScope, filters, collector, cursor, limit );


        return pipeline.execute();
    }


    /**
     * A mutable state for our collectors.  Rather than create a new instance each time, we create a singleton
     * collector
     */
    private final class CollectorState {


        private EntityLoadFilter entityLoadCollector;

        private CandidateEntityFilter candidateEntityFilter;

        private Filter entityLoadFilter;



        private CollectorState( ){}


        /**
         * Set our final filter to be a load entity by Id filter
         */
        public void setIdEntityLoaderFilter() {
            if ( entityLoadCollector == null ) {
                entityLoadCollector = filterFactory.entityLoadFilter();
            }


            entityLoadFilter = entityLoadCollector;
        }


        /**
         * Set our final filter to be a load entity by candidate filter
         */
        public void setCandidateEntityFilter() {
            if ( candidateEntityFilter == null ) {
                candidateEntityFilter = filterFactory.candidateEntityFilter();
            }

            entityLoadFilter = candidateEntityFilter;
        }


        public void clear() {
            entityLoadFilter = null;
        }


        public Filter<?, Entity> getFinalFilter() {
            return entityLoadFilter;
        }
    }
}
