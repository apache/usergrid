/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.index.impl;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.IndexEdge;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.EntityMap;
import org.apache.usergrid.persistence.model.entity.Id;

import static org.apache.usergrid.persistence.index.impl.IndexingUtils.APPLICATION_ID_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.EDGE_NAME_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.EDGE_NODE_ID_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.EDGE_NODE_TYPE_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.EDGE_SEARCH_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.EDGE_TIMESTAMP_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.ENTITY_FIELDS;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.ENTITY_ID_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.ENTITY_SIZE_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.ENTITY_TYPE_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.ENTITY_VERSION_FIELDNAME;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.applicationId;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.entityId;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.getType;
import static org.apache.usergrid.persistence.index.impl.IndexingUtils.nodeId;


/**
 * Convert a CP entity to an elasticsearch document
 */
public class EntityToMapConverter {


    public static Map<String, Object> convert(ApplicationScope applicationScope, final IndexEdge indexEdge,
                                              final Entity entity) {

        return convert( applicationScope, indexEdge, entity, Optional.empty() );
    }

    /**
     * Set the entity as a map with the context
     *
     * @param applicationScope
     * @param entity The entity
     * @param indexEdge The edge this entity is indexed on
     */
    public static Map<String, Object> convert(ApplicationScope applicationScope, final IndexEdge indexEdge,
                                              final Entity entity, Optional<Set<String>> fieldsToIndex) {



        final Map<String, Object> outputEntity = new HashMap<>();


        final Id entityId = entity.getId();

        /***
         * Add our static fields for easier admin/debugging/reporting
         ****/

        outputEntity.put( ENTITY_ID_FIELDNAME, entityId( entityId ) );

        outputEntity.put( ENTITY_VERSION_FIELDNAME, entity.getVersion() );

        outputEntity.put( ENTITY_TYPE_FIELDNAME, getType( applicationScope, entityId));

        outputEntity.put( APPLICATION_ID_FIELDNAME, applicationId( applicationScope.getApplication() ) );

        outputEntity.put( EDGE_NODE_ID_FIELDNAME, nodeId( indexEdge.getNodeId() ) );

        outputEntity.put( EDGE_NODE_TYPE_FIELDNAME, indexEdge.getNodeType() );

        outputEntity.put( EDGE_NAME_FIELDNAME, indexEdge.getEdgeName()  );

        outputEntity.put( EDGE_TIMESTAMP_FIELDNAME, indexEdge.getTimestamp()  );

        outputEntity.put( ENTITY_SIZE_FIELDNAME, entity.getSize() );

        //add the context for filtering later
        outputEntity.put( EDGE_SEARCH_FIELDNAME, IndexingUtils.createContextName( applicationScope, indexEdge ) );

        //migrate the entity to map since we're ultimately going to use maps once we get rid of the Field objects
        final EntityMap entityMap = EntityMap.fromEntity( entity );

        //now visit our entity
        final FieldParser parser = new EntityMappingParser();

        final Set<EntityField> fieldsToBeFiltered =   parser.parse( entityMap );

        //add our fields to output entity
        outputEntity.put( ENTITY_FIELDS, fieldsToBeFiltered );

        if(fieldsToIndex.isPresent()){
            Set<String> defaultProperties = fieldsToIndex.get();
            //copy paste logic here.
            HashSet mapFields = ( HashSet ) outputEntity.get( "fields" );
            Iterator collectionIterator = mapFields.iterator();

            //Loop through all of the fields of the flatted entity and check to see if they should be filtered out.
            collectionIterator.forEachRemaining(outputEntityField -> {
                EntityField testedField = ( EntityField ) outputEntityField;
                String fieldName = ( String ) ( testedField ).get( "name" );

                if ( !defaultProperties.contains( fieldName ) ) {
                iterateThroughMapForFieldsToBeIndexed( defaultProperties, collectionIterator, fieldName );
                }
            });

        }



        return outputEntity;
    }

    /**
     * This method is crucial for selective top level indexing. Here we check to see if the flatted properties
     * are in fact a top level exclusion e.g one.two.three and one.three.two can be allowed for querying by
     * specifying one in the schema. If they are not a top level exclusion then they are removed from the iterator and
     * the map.
     *
     * @param fieldsToKeep - contains a list of fields that the user defined in their schema.
     * @param collectionIterator - contains the iterator with the reference to the map where we want to remove the field.
     * @param fieldName - contains the name of the field that we want to keep.
     */
    private static void iterateThroughMapForFieldsToBeIndexed( final Set<String> fieldsToKeep,
                                                               final Iterator collectionIterator,
                                                               final String fieldName ) {
        boolean toRemoveFlag = true;
        Iterator fieldIterator = fieldsToKeep.iterator();



        //goes through a loop of all the fields ( excluding default ) that we want to keep.
        //if the toRemoveFlag is set to false then we want to keep the property, otherwise we set it to true and remove
        //the property.
        while ( fieldIterator.hasNext() ) {
            //this is the field that we're
            String requiredInclusionString = ( String ) fieldIterator.next();


            //Since we know that the fieldName cannot be equal to the requiredInclusion criteria due to the if condition before we enter this method
            //and we are certain that the indexing criteria is shorter we want to be sure that the inclusion criteria
            //is contained within the field we're evaluating. i.e that one.two.three contains one.two

            //The second part of the if loop also requires that the fieldName is followed by a period after we check to ensure that the
            //indexing criteria is included in the string. This is done to weed out values such as one.twoexample.three
            // when we should only keep one.two.three when comparing the indexing criteria of one.two.
            if(fieldName.length() > requiredInclusionString.length()
                && fieldName.contains( requiredInclusionString )
                && fieldName.charAt( requiredInclusionString.length() )=='.' ) {
                toRemoveFlag = false;
                break;
            }
            else {
                //the of the field we're evaluating is shorter than the indexing criteria so it can't match.
                //Move onto the next field and see if they match.
                toRemoveFlag = true;
            }
        }

        if ( toRemoveFlag ) {
            collectionIterator.remove();
        }
    }
}

