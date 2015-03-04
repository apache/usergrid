/*
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
package org.apache.usergrid.persistence.index.impl;

import java.util.*;

import org.apache.usergrid.persistence.core.future.BetterFuture;
import org.apache.usergrid.persistence.index.*;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.index.query.CandidateResult;
import org.apache.usergrid.persistence.index.utils.IndexValidationUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.ArrayField;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.EntityObjectField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.FloatField;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.ListField;
import org.apache.usergrid.persistence.model.field.LocationField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.SetField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;
import org.apache.usergrid.persistence.model.field.value.EntityObject;

import rx.Observable;
import rx.functions.Func1;

import static org.apache.usergrid.persistence.index.impl.IndexingUtils.ANALYZED_STRING_PREFIX;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.BOOLEAN_PREFIX;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.ENTITYID_ID_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.ENTITY_CONTEXT_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.GEO_PREFIX;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.NUMBER_PREFIX;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.STRING_PREFIX;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.createContextName;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.createIndexDocId;


public class EsEntityIndexBatchImpl implements EntityIndexBatch {

    private static final Logger log = LoggerFactory.getLogger( EsEntityIndexBatchImpl.class );

    private final ApplicationScope applicationScope;

    private final Client client;

    private final boolean refresh;

    private final IndexIdentifier.IndexAlias alias;
    private final IndexIdentifier indexIdentifier;

    private final IndexBufferProducer indexBatchBufferProducer;

    private final AliasedEntityIndex entityIndex;
    private IndexOperationMessage container;


    public EsEntityIndexBatchImpl(final ApplicationScope applicationScope, final Client client,final IndexBufferProducer indexBatchBufferProducer,
            final IndexFig config, final AliasedEntityIndex entityIndex ) {

        this.applicationScope = applicationScope;
        this.client = client;
        this.indexBatchBufferProducer = indexBatchBufferProducer;
        this.entityIndex = entityIndex;
        this.indexIdentifier = IndexingUtils.createIndexIdentifier(config, applicationScope);
        this.alias = indexIdentifier.getAlias();
        this.refresh = config.isForcedRefresh();
        //constrained
        this.container = new IndexOperationMessage();
    }


    @Override
    public EntityIndexBatch index( final IndexScope indexScope, final Entity entity ) {
        IndexValidationUtils.validateIndexScope( indexScope );
        ValidationUtils.verifyEntityWrite( entity );

        final String context = createContextName(indexScope);

        if ( log.isDebugEnabled() ) {
            log.debug( "Indexing entity {}:{}\n   alias: {}\n" +
                       "   app: {}\n   scope owner: {}\n   scope name: {}\n   context: {}",
                entity.getId().getType(), entity.getId().getUuid(), alias.getWriteAlias(),
                    applicationScope.getApplication(), indexScope.getOwner(), indexScope.getName(), context );
        }

        ValidationUtils.verifyEntityWrite( entity );

        Map<String, Object> entityAsMap = entityToMap( entity, context );

        // need prefix here because we index UUIDs as strings

        // let caller add these fields if needed
        // entityAsMap.put("created", entity.getId().getUuid().timestamp();
        // entityAsMap.put("updated", entity.getVersion().timestamp());

        String indexId = createIndexDocId( entity, context );

        log.debug( "Indexing entity documentId {} data {} ", indexId, entityAsMap );
        final String entityType = entity.getId().getType();
        IndexRequestBuilder builder =
                client.prepareIndex(alias.getWriteAlias(), entityType, indexId).setSource( entityAsMap );
        container.addOperation(builder);
        return this;
    }


    @Override
    public EntityIndexBatch deindex( final IndexScope indexScope, final Id id, final UUID version) {

        IndexValidationUtils.validateIndexScope( indexScope );
        ValidationUtils.verifyIdentity(id);
        ValidationUtils.verifyVersion( version );

        final String context = createContextName(indexScope);
        final String entityType = id.getType();

        final String indexId = createIndexDocId( id, version, context );


        if ( log.isDebugEnabled() ) {
            log.debug( "De-indexing entity {}:{} in scope\n   app {}\n   owner {}\n   "
                + "name {} context{}, type {},",
                new Object[] {
                    id.getType(),
                    id.getUuid(),
                    applicationScope.getApplication(),
                    indexScope.getOwner(),
                    indexScope.getName(),
                    context,
                    entityType
                } );
        }


        log.debug( "De-indexing type {} with documentId '{}'" , entityType, indexId);
        String[] indexes = entityIndex.getIndexes(AliasedEntityIndex.AliasType.Read);
        //get the default index if no alias exists yet
        if(indexes == null ||indexes.length == 0){
            indexes = new String[]{indexIdentifier.getIndex(null)};
        }
        //get all indexes then flush everyone
        Observable.from(indexes)
               .map(new Func1<String, Object>() {
                   @Override
                   public Object call(String index) {
                       try {
                           DeleteRequestBuilder builder = client.prepareDelete(index, entityType, indexId).setRefresh(refresh);
                           container.addOperation(builder);
                       }catch (Exception e){
                           log.error("failed to deindex",e);
                           throw e;
                       }
                       return index;
                   }
               }).toBlocking().last();

        log.debug("Deindexed Entity with index id " + indexId);

        return this;
    }


    @Override
    public EntityIndexBatch deindex( final IndexScope indexScope, final Entity entity ) {
        return deindex( indexScope, entity.getId(), entity.getVersion() );
    }


    @Override
    public EntityIndexBatch deindex( final IndexScope indexScope, final CandidateResult entity ) {

        return deindex( indexScope, entity.getId(), entity.getVersion() );
    }

    @Override
    public BetterFuture execute() {
        IndexOperationMessage tempContainer = container;
        container = new IndexOperationMessage();
        return indexBatchBufferProducer.put(tempContainer);
    }

    /**
     * Set the entity as a map with the context
     *
     * @param entity The entity
     * @param context The context this entity appears in
     */
    private static Map entityToMap( final Entity entity, final String context ) {
        final Map entityMap = entityToMap( entity );

        //add the context for filtering later
        entityMap.put( ENTITY_CONTEXT_FIELDNAME, context );

        //but the fieldname we have to prefix because we use query equality to seek this later.
        // TODO see if we can make this more declarative
        entityMap.put( ENTITYID_ID_FIELDNAME, IndexingUtils.idString(entity.getId()).toLowerCase());

        return entityMap;
    }


    /**
     * Convert Entity to Map and Adding prefixes for types:
     * <pre>
     * su_ - String unanalyzed field
     * sa_ - String analyzed field
     * go_ - Location field nu_ - Number field
     * bu_ - Boolean field
     * </pre>
     */
    private static Map entityToMap( EntityObject entity ) {

        Map<String, Object> entityMap = new HashMap<String, Object>();

        for ( Object f : entity.getFields().toArray() ) {

            Field field = ( Field ) f;

            if ( f instanceof ListField ) {
                List list = ( List ) field.getValue();
                entityMap.put( field.getName().toLowerCase(),
                        new ArrayList( processCollectionForMap( list ) ) );

                if ( !list.isEmpty() ) {
                    if ( list.get( 0 ) instanceof String ) {
                        entityMap.put( ANALYZED_STRING_PREFIX + field.getName().toLowerCase(),
                                new ArrayList( processCollectionForMap( list ) ) );
                    }
                }
            }
            else if ( f instanceof ArrayField ) {
                List list = ( List ) field.getValue();
                entityMap.put( field.getName().toLowerCase(),
                        new ArrayList( processCollectionForMap( list ) ) );
            }
            else if ( f instanceof SetField ) {
                Set set = ( Set ) field.getValue();
                entityMap.put( field.getName().toLowerCase(),
                        new ArrayList( processCollectionForMap( set ) ) );
            }
            else if ( f instanceof EntityObjectField ) {
                EntityObject eo = ( EntityObject ) field.getValue();
                entityMap.put( field.getName().toLowerCase(), entityToMap( eo ) ); // recursion
            }
            else if ( f instanceof StringField ) {

                // index in lower case because Usergrid queries are case insensitive
                entityMap.put( ANALYZED_STRING_PREFIX + field.getName().toLowerCase(),
                        ( ( String ) field.getValue() ).toLowerCase() );
                entityMap.put( STRING_PREFIX + field.getName().toLowerCase(),
                        ( ( String ) field.getValue() ).toLowerCase() );
            }
            else if ( f instanceof LocationField ) {
                LocationField locField = ( LocationField ) f;
                Map<String, Object> locMap = new HashMap<String, Object>();

                // field names lat and lon trigger ElasticSearch geo location
                locMap.put( "lat", locField.getValue().getLatitude() );
                locMap.put( "lon", locField.getValue().getLongitude() );
                entityMap.put( GEO_PREFIX + field.getName().toLowerCase(), locMap );
            }
            else if (  f instanceof DoubleField
                    || f instanceof FloatField
                    || f instanceof IntegerField
                    || f instanceof LongField ) {

                entityMap.put( NUMBER_PREFIX + field.getName().toLowerCase(), field.getValue() );
            }
            else if ( f instanceof BooleanField ) {

                entityMap.put( BOOLEAN_PREFIX + field.getName().toLowerCase(), field.getValue() );
            }
            else if ( f instanceof UUIDField ) {

                entityMap.put( STRING_PREFIX + field.getName().toLowerCase(),
                        field.getValue().toString().toLowerCase() );
            }
            else {
                entityMap.put( field.getName().toLowerCase(), field.getValue() );
            }
        }

        return entityMap;
    }


    private static Collection processCollectionForMap( final Collection c ) {
        if ( c.isEmpty() ) {
            return c;
        }
        List processed = new ArrayList();
        Object sample = c.iterator().next();

        if ( sample instanceof Entity ) {
            for ( Object o : c.toArray() ) {
                Entity e = ( Entity ) o;
                processed.add( entityToMap( e ) );
            }
        }
        else if ( sample instanceof List ) {
            for ( Object o : c.toArray() ) {
                List list = ( List ) o;
                processed.add( processCollectionForMap( list ) ); // recursion;
            }
        }
        else if ( sample instanceof Set ) {
            for ( Object o : c.toArray() ) {
                Set set = ( Set ) o;
                processed.add( processCollectionForMap( set ) ); // recursion;
            }
        }
        else {
            for ( Object o : c.toArray() ) {
                processed.add( o );
            }
        }
        return processed;
    }

}
