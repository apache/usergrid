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


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * An object that represents an entity field.
 *
 * All factory methods should be used to create an instance of the field
 */
public class EntityField extends HashMap<String, Object> {


    /**
     * We only ever have 2 values in our field
     */
    private EntityField(){
        super(2);
    }


    /**
     * Set a string value into the field
     */
    public static EntityField create( final String fieldName, final String value ) {
        EntityField field = new EntityField();
        field.put( IndexingUtils.FIELD_NAME, fieldName.toLowerCase() );
        field.put( IndexingUtils.FIELD_STRING, value );

        return field;
    }


    /**
     * Set a uuid value into the field
     */
    public static EntityField create( final String fieldName, final UUID value ) {
        EntityField field = new EntityField();
        field.put( IndexingUtils.FIELD_NAME, fieldName.toLowerCase() );
        field.put( IndexingUtils.FIELD_UUID, value.toString() );

        return field;
    }


    /**
     * Set a string value into the field
     */
    public static EntityField create( final String fieldName, final boolean value ) {
        EntityField field = new EntityField();
        field.put( IndexingUtils.FIELD_NAME, fieldName.toLowerCase() );
        field.put( IndexingUtils.FIELD_BOOLEAN, value );

        return field;
    }




    /**
     * Set a string value into the field
     */
    public static EntityField create( final String fieldName, final int value ) {
        EntityField field = new EntityField();
        field.put( IndexingUtils.FIELD_NAME, fieldName.toLowerCase() );
        field.put( IndexingUtils.FIELD_LONG, value );

        return field;
    }


    /**
     * Set a string value into the field
     */
    public static EntityField create( final String fieldName, final long value ) {
        EntityField field = new EntityField();
        field.put( IndexingUtils.FIELD_NAME, fieldName.toLowerCase() );
        field.put( IndexingUtils.FIELD_LONG, value );

        return field;
    }


    /**
     * Set a string value into the field
     */
    public static EntityField create( final String fieldName, final float value ) {
        EntityField field = new EntityField();
        field.put( IndexingUtils.FIELD_NAME, fieldName.toLowerCase() );
        field.put( IndexingUtils.FIELD_DOUBLE, value );

        return field;
    }


    /**
     * Set a string value into the field
     */
    public static EntityField create( final String fieldName, final double value ) {
        EntityField field = new EntityField();
        field.put( IndexingUtils.FIELD_NAME, fieldName.toLowerCase() );
        field.put( IndexingUtils.FIELD_DOUBLE, value );

        return field;
    }




    /**
     * Set a location into our field
     */
    public static EntityField create( final String fieldName, final Map location) {
        EntityField field = new EntityField();
        field.put( IndexingUtils.FIELD_NAME, fieldName.toLowerCase() );
        field.put( IndexingUtils.FIELD_LOCATION, location );

        return field;
    }


}
