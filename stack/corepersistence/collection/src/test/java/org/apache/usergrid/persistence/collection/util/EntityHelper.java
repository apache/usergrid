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

package org.apache.usergrid.persistence.collection.util;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.StringField;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Utilities for generating test data.
 */
public class EntityHelper {

    /**
     * Generates an entity with all string fields to have at least the minSize number of characters in the field names +
     * field values
     */
    public static Entity generateEntity( final int minSize ) {
        int currentLength = 0;

        final Entity entity = new Entity( new SimpleId( "test" ) );

        //generate a really large string value


        //loop until our size is beyond the set size
        for ( int i = 0; currentLength < minSize; i++ ) {
            final String key = "newStringField" + i;

            currentLength += key.length();

            StringBuilder builder = new StringBuilder();

            for ( int j = 0; j < 1000 && currentLength < minSize; j++ ) {
                builder.append( "a" );
                currentLength ++;
            }


            entity.setField( new StringField( key, builder.toString() ) );
        }

        return entity;
    }


    /**
     * Verify that all fields in the expected are present in the returned, and have the same values
     * via .equals.  Does not recurse on object values.  Also verifies there are no additional fields
     * in the returned entity
     * ø
     * @param expected
     * @param returned
     */
    public static void verifyDeepEquals( final Entity expected, final Entity returned ){

        //perform object equals
        assertEquals("Expected same entity equality", expected, returned);

        final Collection<Field> expectedFields = expected.getFields();

        final Map<String, Field> returnedFields = new HashMap<>(returned.getFieldMap());

        for(Field expectedField: expectedFields){

            final Field returnedField = returnedFields.get( expectedField.getName() );

            assertNotNull("Field " + expectedField.getName() + " exists in returned entity", returnedField );

            assertEquals("Field values should match", expectedField.getValue(), returnedField.getValue());

            returnedFields.remove( expectedField.getName() );
        }

        assertEquals("There are no additional fields in the returned entity", 0, returnedFields.size());
    }
}
