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

package org.apache.usergrid.corepersistence.pipeline.read.search;


import java.util.*;

import org.apache.usergrid.corepersistence.index.IndexLocationStrategyFactory;
import org.apache.usergrid.persistence.index.*;
import org.apache.usergrid.persistence.index.impl.IndexProducer;
import org.apache.usergrid.persistence.model.field.DistanceField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.EntityObjectField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.value.EntityObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.pipeline.read.AbstractFilter;
import org.apache.usergrid.corepersistence.pipeline.read.EdgePath;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.EntitySet;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.fasterxml.uuid.UUIDComparator;
import com.google.common.base.Optional;
import com.google.inject.Inject;

import rx.Observable;


/**
 * Loads entities from an incoming CandidateResult emissions into entities, then streams them on performs internal
 * buffering for efficiency.  Note that all entities may not be emitted if our load crosses page boundaries.
 * It is up to the collector to determine when to stop streaming entities.
 */
public class CandidateEntityFilter extends AbstractFilter<FilterResult<Candidate>, FilterResult<Entity>> {

    private final EntityCollectionManagerFactory entityCollectionManagerFactory;
    private final EntityIndexFactory entityIndexFactory;
    private final IndexLocationStrategyFactory indexLocationStrategyFactory;
    private final IndexProducer indexProducer;


    @Inject
    public CandidateEntityFilter( final EntityCollectionManagerFactory entityCollectionManagerFactory,
                                  final EntityIndexFactory entityIndexFactory,
                                  final IndexLocationStrategyFactory indexLocationStrategyFactory,
                                  final IndexProducer indexProducer
                                  ) {
        this.entityCollectionManagerFactory = entityCollectionManagerFactory;
        this.entityIndexFactory = entityIndexFactory;
        this.indexLocationStrategyFactory = indexLocationStrategyFactory;
        this.indexProducer = indexProducer;
    }


    @Override
       public Observable<FilterResult<Entity>> call(
           final Observable<FilterResult<Candidate>> candidateResultsObservable ) {


        /**
         * A bit kludgy from old 1.0 -> 2.0 apis.  Refactor this as we clean up our lower levels and create new results
         * objects
         */

        final ApplicationScope applicationScope = pipelineContext.getApplicationScope();

        final EntityCollectionManager entityCollectionManager =
            entityCollectionManagerFactory.createCollectionManager( applicationScope );


        final EntityIndex applicationIndex = entityIndexFactory
            .createEntityIndex(indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope) );

        //buffer them to get a page size we can make 1 network hop
        final Observable<FilterResult<Entity>> searchIdSetObservable =
            candidateResultsObservable.buffer( pipelineContext.getLimit() )

            //load them
            .flatMap( candidateResults -> {

                //flatten toa list of ids to load
                final Observable<List<Candidate>> candidates =
                    Observable.from(candidateResults)
                        .map(filterResultCandidate -> filterResultCandidate.getValue()).toList();
                //load the ids
                final Observable<FilterResult<Entity>> entitySetObservable =
                    candidates.flatMap(candidatesList -> {
                        Collection<SelectFieldMapping> mappings = candidatesList.get(0).getFields();
                        Observable<EntitySet> entitySets = Observable.from(candidatesList)
                            .map(candidateEntry -> candidateEntry.getCandidateResult().getId()).toList()
                            .flatMap(idList -> entityCollectionManager.load(idList));
                        //now we have a collection, validate our canidate set is correct.
                        return entitySets.map(
                            entitySet -> new EntityVerifier(
                                applicationIndex.createBatch(), entitySet, candidateResults,indexProducer)
                        )
                            .doOnNext(entityCollector -> entityCollector.merge())
                            .flatMap(entityCollector -> Observable.from(entityCollector.getResults()))
                            .map(entityFilterResult -> {
                                final Entity entity = entityFilterResult.getValue();
                                if (mappings.size() > 0) {
                                    Map<String,Field> fieldMap = new HashMap<String, Field>(mappings.size());
                                    rx.Observable.from(mappings)

                                        .filter(mapping -> {
                                            if ( entity.getFieldMap().containsKey(mapping.getSourceFieldName())) {
                                                return true;
                                            }
                                            String[] parts = mapping.getSourceFieldName().split("\\.");
                                            return nestedFieldCheck( parts, entity.getFieldMap() );
                                        })

                                        .doOnNext(mapping -> {
                                            Field field = entity.getField(mapping.getSourceFieldName());
                                            if ( field != null ) {
                                                field.setName( mapping.getTargetFieldName() );
                                                fieldMap.put( mapping.getTargetFieldName(), field );
                                            } else {
                                                String[] parts = mapping.getSourceFieldName().split("\\.");
                                                nestedFieldSet( fieldMap, parts, entity.getFieldMap() );
                                            }
                                        }).toBlocking().lastOrDefault(null);

                                    entity.setFieldMap(fieldMap);
                                }
                                return entityFilterResult;
                            });
                    });
                return entitySetObservable;


            } );

        //if we filter all our results, we want to continue to try the next page
        return searchIdSetObservable;
    }


    /**
     * Sets field in result map with support for nested fields via recursion.
     *
     * @param result The result map of filtered fields
     * @param parts The parts of the field name (more than one if field is nested)
     * @param fieldMap Map of fields of the object
     */
    private void nestedFieldSet( Map<String, Field> result, String[] parts, Map<String, Field> fieldMap) {
        if ( parts.length > 0 ) {

            if ( fieldMap.containsKey( parts[0] )) {
                Field field = fieldMap.get( parts[0] );
                if ( field instanceof EntityObjectField ) {
                    EntityObjectField eof = (EntityObjectField)field;
                    result.putIfAbsent( parts[0], new EntityObjectField( parts[0], new EntityObject() ) );

                    // recursion
                    nestedFieldSet(
                        ((EntityObjectField)result.get( parts[0] )).getValue().getFieldMap(),
                        Arrays.copyOfRange(parts, 1, parts.length),
                        eof.getValue().getFieldMap());

                } else {
                    result.put( parts[0], field );
                }
            }
        }
    }


    /**
     * Check to see if field should be included in filtered result with support for nested fields via recursion.
     *
     * @param parts The parts of the field name (more than one if field is nested)
     * @param fieldMap Map of fields of the object
     */
    private boolean nestedFieldCheck( String[] parts, Map<String, Field> fieldMap) {
        if ( parts.length > 0 ) {

            if ( fieldMap.containsKey( parts[0] )) {
                Field field = fieldMap.get( parts[0] );
                if ( field instanceof EntityObjectField ) {
                    EntityObjectField eof = (EntityObjectField)field;

                    // recursion
                    return nestedFieldCheck( Arrays.copyOfRange(parts, 1, parts.length), eof.getValue().getFieldMap());

                } else {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Our collector to collect entities.  Not quite a true collector, but works within our operational
     * flow as this state is mutable and difficult to represent functionally
     */
    private static final class EntityVerifier {

        private static final Logger logger = LoggerFactory.getLogger( EntityVerifier.class );
        private List<FilterResult<Entity>> results = new ArrayList<>();

        private final EntityIndexBatch batch;
        private final List<FilterResult<Candidate>> candidateResults;
        private final IndexProducer indexProducer;
        private final EntitySet entitySet;


        public EntityVerifier( final EntityIndexBatch batch, final EntitySet entitySet,
                               final List<FilterResult<Candidate>> candidateResults,
                               final IndexProducer indexProducer) {
            this.batch = batch;
            this.entitySet = entitySet;
            this.candidateResults = candidateResults;
            this.indexProducer = indexProducer;
            this.results = new ArrayList<>( entitySet.size() );
        }


        /**
         * Merge our candidates and our entity set into results
         */
        public void merge() {

            for ( final FilterResult<Candidate> candidateResult : candidateResults ) {
                validate( candidateResult );
            }

            indexProducer.put(batch.build()).toBlocking().lastOrDefault(null); // want to rethrow if batch fails

        }


        public List<FilterResult<Entity>> getResults() {
            return results;
        }


        public EntityIndexBatch getBatch() {
            return batch;
        }


        private void validate( final FilterResult<Candidate> filterResult ) {

            final Candidate candidate = filterResult.getValue();
            final CandidateResult candidateResult = candidate.getCandidateResult();
            final boolean isGeo = candidateResult instanceof GeoCandidateResult;
            final SearchEdge searchEdge = candidate.getSearchEdge();
            final Id candidateId = candidateResult.getId();
            final UUID candidateVersion = candidateResult.getVersion();


            final MvccEntity entity = entitySet.getEntity( candidateId );


            //doesn't exist warn and drop
            if ( entity == null ) {
                logger.warn(
                    "Searched and received candidate with entityId {} and version {}, yet was not found in cassandra.  Ignoring since this could be a region sync issue",
                    candidateId, candidateVersion );


                //TODO trigger an audit after a fail count where we explicitly try to repair from other regions

                return;

            }


            final UUID entityVersion = entity.getVersion();
            final Id entityId = entity.getId();





            //entity is newer than ES version, could be an update or the entity is marked as deleted
            if ( UUIDComparator.staticCompare( entityVersion, candidateVersion ) > 0 ||
                    !entity.getEntity().isPresent()  ||
                    entity.getStatus() == MvccEntity.Status.DELETED ) {

                // when updating entities, we don't delete previous versions from ES so this action is expected
                if(logger.isDebugEnabled()){
                    logger.debug( "Deindexing stale entity on edge {} for entityId {} and version {}",
                        searchEdge, entityId, entityVersion);
                }

                batch.deindex( searchEdge, entityId, candidateVersion );
                return;
            }

            //ES is newer than cass, it means we haven't repaired the record in Cass, we don't want to
            //remove the ES record, since the read in cass should cause a read repair, just ignore
            if ( UUIDComparator.staticCompare( candidateVersion, entityVersion ) > 0 ) {

                logger.warn(
                    "Found a newer version in ES over cassandra for edge {} for entityId {} and version {}.  Repair should be run",
                        searchEdge, entityId, entityVersion);

                  //TODO trigger an audit after a fail count where we explicitly try to repair from other regions

                return;
            }

            //they're the same add it

            final Entity returnEntity = entity.getEntity().get();
            if(isGeo){
                returnEntity.setField(new DistanceField(((GeoCandidateResult)candidateResult).getDistance()));
            }

            final Optional<EdgePath> parent = filterResult.getPath();

            final FilterResult<Entity> toReturn = new FilterResult<>( returnEntity, parent );

            results.add( toReturn );
        }
    }
}
