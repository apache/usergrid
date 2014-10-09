/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.corepersistence.results;/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.corepersistence.CpManagerCache;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexBatch;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.impl.IndexScopeImpl;
import org.apache.usergrid.persistence.index.query.CandidateResult;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.fasterxml.uuid.UUIDComparator;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;


public class FilteringLoader implements ResultsLoader {

    private static final Logger logger = LoggerFactory.getLogger( FilteringLoader.class );

    private final CpManagerCache managerCache;
    private final ResultsVerifier resultsLoader;
    private final Id ownerId;
    private final ApplicationScope applicationScope;


     protected FilteringLoader( final CpManagerCache managerCache, final ResultsVerifier resultsLoader,
                                final EntityRef ownerId, final ApplicationScope applicationScope ) {
         this.managerCache = managerCache;
         this.resultsLoader = resultsLoader;
         this.ownerId = new SimpleId( ownerId.getUuid(), ownerId.getType());
         this.applicationScope = applicationScope;
     }


    @Override
    public Results getResults( final CandidateResults crs ) {


        final EntityIndex index = managerCache.getEntityIndex( applicationScope );

        final EntityIndexBatch indexBatch = index.createBatch();

        final Map<Id, Integer> orderIndex = new HashMap<>( crs.size() );

        final Map<Id, CandidateResult> idResultMapping = new HashMap<>( crs.size() );

        final HashMultimap<String, CandidateResult> groupedByScopes = HashMultimap.create( crs.size(), crs.size() );

        final Iterator<CandidateResult> iter = crs.iterator();


        /**
         * Go through the candidates and group them by scope for more efficient retrieval
         */
        for ( int i = 0; iter.hasNext(); i++ ) {

            final CandidateResult cr = iter.next();

            final String collectionType = CpNamingUtils.getCollectionScopeNameFromEntityType( cr.getId().getType() );

            final Id entityId = cr.getId();

            //if we've already seen this one, put which ever is greater

            final CandidateResult seen = idResultMapping.get( entityId );

            if ( seen == null ) {
                idResultMapping.put( entityId, cr );
                orderIndex.put( entityId, i );
                groupedByScopes.put( collectionType, cr );
            }

            //we have seen it, compare them
            else {
                final UUID seenVersion = seen.getVersion();
                final UUID currentVersion = cr.getVersion();

                //this is a newer version, we know we already have a stale entity, add it to be cleaned up
                if ( UUIDComparator.staticCompare( currentVersion, seenVersion ) > 0 ) {

                    //de-index it
                    logger.debug( "Stale version of Entity uuid:{} type:{}, stale v:{}, latest v:{}", new Object[] {
                            entityId.getUuid(), entityId.getType(), seen, currentVersion
                    } );

                    //deindex
                    deIndex( indexBatch, ownerId, cr );


                    //TODO, fire the entity repair cleanup task here instead of de-indexing

                    //replace the value with a more current version
                    idResultMapping.put( entityId, cr );
                    orderIndex.put( entityId, i );
                    groupedByScopes.put( collectionType, cr );
                }
            }
        }


        //now everything is ordered, and older versions are removed.  Batch fetch versions to verify existence and
        // correct versions

        final TreeMap<Integer, Id> sortedResults = new TreeMap<>();

        for ( final String scopeName : groupedByScopes.keys() ) {


            final Set<CandidateResult> candidateResults = groupedByScopes.get( scopeName );

            final Collection<Id> idsToLoad =
                    Collections2.transform( candidateResults, new Function<CandidateResult, Id>() {
                        @Nullable
                        @Override
                        public Id apply( @Nullable final CandidateResult input ) {
                            if ( input == null ) {
                                return null;
                            }

                            return input.getId();
                        }
                    } );


            //now using the scope, load the collection

            /**
             * Get the collection scope and batch load all the versions
             */
            final CollectionScope collScope =
                    new CollectionScopeImpl( applicationScope.getApplication(), applicationScope.getApplication(),
                            scopeName );


            final EntityCollectionManager ecm = managerCache.getEntityCollectionManager( collScope );


            //load the results into the loader for this ech
            resultsLoader.loadResults( idsToLoad, ecm );





            //now compare them

            for ( final Id requestedId : idsToLoad ) {

                final CandidateResult cr = idResultMapping.get( requestedId );

                //ask the loader if this is valid, if not discard it
                if(!resultsLoader.isValid( cr )){
                    deIndex( indexBatch, ownerId, cr );
                    continue;
                }

                //if we get here we're good, we need to add this to our results
                final int candidateIndex = orderIndex.get( requestedId  );

                sortedResults.put( candidateIndex, requestedId );

            }
        }


        //execute the cleanup
        indexBatch.execute();

        return resultsLoader.getResults(sortedResults.values());
    }





    protected void deIndex( final EntityIndexBatch batch, final Id ownerId, final CandidateResult candidateResult ) {
        IndexScope indexScope = new IndexScopeImpl( ownerId,
                CpNamingUtils.getCollectionScopeNameFromEntityType( candidateResult.getId().getType() ) );

        batch.deindex( indexScope, candidateResult );
    }
}
