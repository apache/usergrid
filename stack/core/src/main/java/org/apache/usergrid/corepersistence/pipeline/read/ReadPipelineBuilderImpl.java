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


import org.apache.usergrid.corepersistence.pipeline.DataPipeline;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
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


    private final ReadFilterFactory readFilterFactory;

    private final DataPipeline pipeline;

    /**
     * Our pointer to our collect filter. Set or cleared with each operation that's performed so the correct results are
     * rendered
     */
    private CollectorFilter<Results> collectorFilter;

    private Optional<String> cursor;
    private Optional<Integer> limit;


    @Inject
    public ReadPipelineBuilderImpl( final ReadFilterFactory readFilterFactory,
                                    @Assisted final ApplicationScope applicationScope ) {
        this.readFilterFactory = readFilterFactory;

        //set up our pipeline with our application scope
        this.pipeline = new DataPipeline( applicationScope );

        //init our cursor to empty
        this.cursor = Optional.absent();

        //set the default limit
        this.limit = Optional.absent();
    }


    @Override
    public ReadPipelineBuilder withCursor( final String cursor ) {
        this.cursor = Optional.fromNullable( cursor );
        pipeline.setCursor( this.cursor );
        return this;
    }


    @Override
    public ReadPipelineBuilder withLimit( final int limit ) {
        Preconditions.checkArgument( limit > 0, "You must set the limit > 0" );
        this. limit = Optional.of( limit );
        //set the default value
        pipeline.setLimit( this.limit.or( 10 ) );
        return this;
    }


    @Override
    public ReadPipelineBuilder setStartId( final Id id ) {
        pipeline.withTraverseCommand( readFilterFactory.getEntityIdFilter( id ) );

        this.collectorFilter = null;


        return this;
    }


    @Override
    public ReadPipelineBuilder getEntityViaCollection( final String collectionName, final Id entityId ) {

        pipeline.withTraverseCommand( readFilterFactory.readGraphCollectionByIdFilter( collectionName, entityId ) );

        setEntityLoaderFilter();

        return this;
    }


    @Override
    public ReadPipelineBuilder getCollection( final String collectionName ) {


        pipeline.withTraverseCommand( readFilterFactory.readGraphCollectionCommand( collectionName ) );

        setEntityLoaderFilter();

        return this;
    }


    @Override
    public ReadPipelineBuilder getCollectionWithQuery( final String collectionName, final String query ) {

        //TODO, this should really be 2 a TraverseFilter with an entityLoad collector
        collectorFilter = readFilterFactory.queryCollectionElasticSearchCollector( collectionName, query );
        return this;
    }


    @Override
    public ReadPipelineBuilder getEntityViaConnection( final String connectionName, final Id entityId ) {

        pipeline.withTraverseCommand( readFilterFactory.readGraphConnectionByIdFilter( connectionName, entityId ) );
        setEntityLoaderFilter();

        return this;
    }


    @Override
    public ReadPipelineBuilder getConnection( final String connectionName ) {

        pipeline.withTraverseCommand( readFilterFactory.readGraphConnectionCommand( connectionName ) );
        setEntityLoaderFilter();

        return this;
    }


    @Override
    public ReadPipelineBuilder getConnection( final String connectionName, final String entityType ) {
        pipeline.withTraverseCommand( readFilterFactory.readGraphConnectionCommand( connectionName, entityType ) );
        setEntityLoaderFilter();

        return this;
    }


    /**
     *
     * @param connectionName
     * @param query
     * @return
     */
    @Override
    public ReadPipelineBuilder connectionWithQuery( final String connectionName, final String query ) {

        //TODO, this should really be 2 a TraverseFilter with an entityLoad collector
        collectorFilter = readFilterFactory.queryConnectionElasticSearchCollector( connectionName, query );

        return this;
    }


    @Override
    public ReadPipelineBuilder connectionWithQuery( final String connectionName, final String entityType,
                                                    final String query ) {

        //TODO, this should really be 2 a TraverseFilter with an entityLoad collector
        collectorFilter =
            readFilterFactory.queryConnectionElasticSearchCollector( connectionName, entityType, query);
        return this;
    }


    @Override
    public Observable<Results> build() {
        Preconditions.checkNotNull( collectorFilter,
            "You have not specified an operation that creates a collection filter.  This is required for loading "
                + "results" );

        return pipeline.build( collectorFilter );
    }


    private void setEntityLoaderFilter() {
        collectorFilter = readFilterFactory.entityLoadCollector();
    }
}
