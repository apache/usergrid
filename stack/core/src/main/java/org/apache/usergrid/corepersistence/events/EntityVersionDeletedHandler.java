/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.corepersistence.events;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.CpEntityManagerFactory;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.event.EntityVersionDeleted;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.serialization.EdgesObservable;
import org.apache.usergrid.persistence.index.ApplicationEntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.IndexBatchBuffer;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.impl.IndexScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import rx.Observable;

import static org.apache.usergrid.corepersistence.CoreModule.EVENTS_DISABLED;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.generateScopeFromSource;
import static org.apache.usergrid.corepersistence.util.CpNamingUtils.generateScopeToTarget;


/**
 * Remove Entity index when specific version of Entity is deleted. TODO: do we need this? Don't our version-created and
 * entity-deleted handlers take care of this? If we do need it then it should be wired in via GuiceModule in the
 * corepersistence package.
 */
@Singleton
public class EntityVersionDeletedHandler implements EntityVersionDeleted {
    private static final Logger logger = LoggerFactory.getLogger( EntityVersionDeletedHandler.class );


    private final EntityManagerFactory emf;
    private final EdgesObservable edgesObservable;
    private final SerializationFig serializationFig;


    @Inject
    public EntityVersionDeletedHandler( final EntityManagerFactory emf, final EdgesObservable edgesObservable,
                                        final SerializationFig serializationFig ) {
        this.emf = emf;
        this.edgesObservable = edgesObservable;
        this.serializationFig = serializationFig;
    }


    @Override
    public void versionDeleted( final ApplicationScope scope, final Id entityId,
                                final List<MvccLogEntry> entityVersions ) {


        // This check is for testing purposes and for a test that to be able to dynamically turn
        // off and on delete previous versions so that it can test clean-up on read.
        if ( System.getProperty( EVENTS_DISABLED, "false" ).equals( "true" ) ) {
            return;
        }

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Handling versionDeleted count={} event for entity {}:{} v {} " + "  app: {}", new Object[] {
                entityVersions.size(), entityId.getType(), entityId.getUuid(), scope.getApplication()
            } );
        }

        CpEntityManagerFactory cpemf = ( CpEntityManagerFactory ) emf;

        final ApplicationEntityIndex ei = cpemf.getManagerCache().getEntityIndex( scope );
        final GraphManager gm = cpemf.getManagerCache().getGraphManager( scope );


        //create an observable of all scopes to deIndex
        //remove all indexes pointing to this
        final Observable<IndexScope> targetScopes =  edgesObservable.edgesToTarget( gm, entityId ).map(
            edge -> generateScopeFromSource( edge) );


        //Remove all double indexes
        final Observable<IndexScope> sourceScopes = edgesObservable.edgesFromSource( gm, entityId ).map(
                    edge -> generateScopeToTarget( edge ) );


        //create a stream of scopes
        final Observable<IndexScopeVersion> versions = Observable.merge( targetScopes, sourceScopes ).flatMap(
            indexScope -> Observable.from( entityVersions )
                                    .map( version -> new IndexScopeVersion( indexScope, version ) ) );

        //create a set of batches
        final Observable<EntityIndexBatch> batches = versions.buffer( serializationFig.getBufferSize() ).flatMap(
            bufferedVersions -> Observable.from( bufferedVersions ).collect( () -> ei.createBatch(),
                ( EntityIndexBatch batch, IndexScopeVersion version ) -> {
                    //deindex in this batch
                    batch.deindex( version.scope, version.version.getEntityId(), version.version.getVersion() );
                } ) );



        //execute the batches
        batches.doOnNext( batch -> batch.execute() ).toBlocking().last();

    }





    private static final class IndexScopeVersion{
        private final IndexScope scope;
        private final MvccLogEntry version;


        private IndexScopeVersion( final IndexScope scope, final MvccLogEntry version ) {
            this.scope = scope;
            this.version = version;
        }
    }
}
