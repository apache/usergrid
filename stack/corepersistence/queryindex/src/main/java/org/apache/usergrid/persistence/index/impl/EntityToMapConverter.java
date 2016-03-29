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


import java.util.ArrayList;
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

        //add our fields
        outputEntity.put( ENTITY_FIELDS, fieldsToBeFiltered );

        if(fieldsToIndex.isPresent()){
            Set<String> defaultProperties = fieldsToIndex.get();
            //copy paste logic here.
            HashSet mapFields = ( HashSet ) outputEntity.get( "fields" );
            Iterator collectionIterator = mapFields.iterator();

            //Loop through all of the fields of the flatted entity and check to see if they should be filtered out.
            while ( collectionIterator.hasNext() ) {
                EntityField testedField = ( EntityField ) collectionIterator.next();
                String fieldName = ( String ) ( testedField ).get( "name" );

                if ( !defaultProperties.contains( fieldName ) ) {
                    iterateThroughMapForFieldsToBeIndexed( defaultProperties, collectionIterator, fieldName );
                }
            }
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
        String[] flattedStringArray = getStrings( fieldName );

        Iterator fieldIterator = fieldsToKeep.iterator(); //fieldsToKeep.iterator();

        //goes through a loop of all the fields ( excluding default ) that we want to keep.
        //if the toRemoveFlag is set to false then we want to keep the property, otherwise we set it to true and remove
        //the property.
        while ( fieldIterator.hasNext() ) {
            String requiredInclusionString = ( String ) fieldIterator.next();
            String[] flattedRequirementString = getStrings( requiredInclusionString );


            //loop each split array value to see if it matches the equivalent value
            //in the field. e.g in the example one.two.three and one.two.four we need to check that the schema
            //matches in both one and two above. If instead it says to exclude one.twor then we would still exclude the above
            //since it is required to be a hard match.

            //The way the below works if we see that the current field isn't as fine grained as the schema rule
            //( aka the array is shorter than the current index of the schema rule then there is no way the rule could apply
            // to the index.

            //Then if that check passes we go to check that both parts are equal. If they are ever not equal
            // e.g one.two.three and one.three.two then it shouldn't be included
            //TODO: regex.
            for ( int index = 0; index < flattedRequirementString.length; index++ ) {
                //if the array contains a string that it is equals to then set the remove flag to true
                //otherwise remain false.

                //one.three
                //one.two
                //one
                if ( flattedStringArray.length <= index ) {
                    toRemoveFlag = true;
                    break;
                }

                if ( flattedRequirementString[index].equals( flattedStringArray[index] ) ) {
                    toRemoveFlag = false;
                }
                else {
                    toRemoveFlag = true;
                    break;
                }
            }
            if ( toRemoveFlag == false ) {
                break;
            }
        }

        if ( toRemoveFlag ) {
            collectionIterator.remove();
        }
    }


    /**
     * Splits the string on the flattened period "." seperated values.
     * @param fieldName
     * @return
     */
    private static String[] getStrings( final String fieldName ) {
        final String[] flattedStringArray;
        if ( !fieldName.contains( "." ) ) {
            //create a single array that is made of a the single value.
            flattedStringArray = new String[] { fieldName };
        }
        else {
            flattedStringArray = fieldName.split( "\\." );
        }
        return flattedStringArray;
    }

}

