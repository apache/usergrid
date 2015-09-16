/*
 *
 *
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
 *
 *
 */

package org.apache.usergrid.persistence.index.impl;


import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.IndexEdge;
import org.apache.usergrid.persistence.index.SearchEdge;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.ArrayField;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.EntityObjectField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.FloatField;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.LocationField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;
import org.apache.usergrid.persistence.model.field.value.EntityObject;
import org.apache.usergrid.persistence.model.field.value.Location;
import org.apache.usergrid.persistence.model.util.EntityUtils;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import rx.functions.Action2;

import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;


/**
 * Tests our entity conversion
 */
public class EntityToMapConverterTest {


    @Test
    public void testBaseFields() {
        Entity entity = new Entity( "test" );


        final UUID version = UUIDGenerator.newTimeUUID();

        EntityUtils.setVersion( entity, version );

        final ApplicationScope scope = new ApplicationScopeImpl( createId( "application" ) );

        final IndexEdge indexEdge =
            new IndexEdgeImpl( createId( "source" ), "testEdgeType", SearchEdge.NodeType.SOURCE, 1000 );


        final Map<String, Object> entityMap = EntityToMapConverter.convert( scope, indexEdge, entity );


        //verify our root fields


        final String applicationId = entityMap.get( IndexingUtils.APPLICATION_ID_FIELDNAME ).toString();

        assertEquals( IndexingUtils.applicationId( scope.getApplication() ), applicationId );

        final String entityIdString = entityMap.get( IndexingUtils.ENTITY_ID_FIELDNAME ).toString();

        assertEquals( IndexingUtils.entityId( entity.getId() ), entityIdString );


        final String versionString = entityMap.get( IndexingUtils.ENTITY_VERSION_FIELDNAME ).toString();

        assertEquals( versionString, version.toString() );

        final String entityTypeString = entityMap.get( IndexingUtils.ENTITY_TYPE_FIELDNAME ).toString();

        assertEquals( IndexingUtils.getType( scope, entity.getId() ), entityTypeString );

        final String nodeIdString = entityMap.get( IndexingUtils.EDGE_NODE_ID_FIELDNAME ).toString();

        assertEquals( IndexingUtils.nodeId( indexEdge.getNodeId() ), nodeIdString );

        final String edgeName = entityMap.get( IndexingUtils.EDGE_NAME_FIELDNAME ).toString();

        assertEquals( indexEdge.getEdgeName(), edgeName );

        final String nodeType = entityMap.get( IndexingUtils.EDGE_NODE_TYPE_FIELDNAME ).toString();


        assertEquals( indexEdge.getNodeType().toString(), nodeType );

        final long edgeTimestamp = ( long ) entityMap.get( IndexingUtils.EDGE_TIMESTAMP_FIELDNAME );

        assertEquals( indexEdge.getTimestamp(), edgeTimestamp );

        final String edgeSearch = entityMap.get( IndexingUtils.EDGE_SEARCH_FIELDNAME ).toString();

        assertEquals( IndexingUtils.createContextName( scope, indexEdge ), edgeSearch );


        final List<EntityField> fields = ( List<EntityField> ) entityMap.get( IndexingUtils.ENTITY_FIELDS );

        assertEquals( 0, fields.size() );
    }


    @Test
    public void testStringField() {
        testSingleField( new StringField( "Name", "value" ),
            ( field, entityField ) -> assertEquals( field.getValue(), entityField.get( IndexingUtils.FIELD_STRING ) ) );
    }


    @Test
    public void testBooleanField() {
        testSingleField( new BooleanField( "Name", true ), ( field, entityField ) -> assertEquals( field.getValue(),
            entityField.get( IndexingUtils.FIELD_BOOLEAN ) ) );
    }


    @Test
    public void testIntegerField() {
        testSingleField( new IntegerField( "Name", 100 ),
            ( field, entityField ) -> assertEquals( field.getValue(), entityField.get( IndexingUtils.FIELD_LONG ) ) );
    }


    @Test
    public void testLongField() {
        testSingleField( new LongField( "Name", 100l ),
            ( field, entityField ) -> assertEquals( field.getValue(), entityField.get( IndexingUtils.FIELD_LONG ) ) );
    }


    @Test
    public void testFloadField() {
        testSingleField( new FloatField( "Name", 1.10f ),
            ( field, entityField ) -> assertEquals( field.getValue(), entityField.get( IndexingUtils.FIELD_DOUBLE ) ) );
    }


    /**
     * Test converting a UUID to a string
     */
    @Test
    public void testUUIDField() {
        testSingleField( new UUIDField( "Name", UUIDGenerator.newTimeUUID() ),
            ( field, entityField ) -> assertEquals( field.getValue().toString(), entityField.get( IndexingUtils.FIELD_STRING ) ) );
    }


    @Test
    public void testDoubleField() {
        testSingleField( new DoubleField( "Name", 2.20d ),
            ( field, entityField ) -> assertEquals( field.getValue(), entityField.get( IndexingUtils.FIELD_DOUBLE ) ) );
    }


    @Test
    public void testLocationField() {
        testSingleField( new LocationField( "Name", new Location( 10, 20 ) ), ( field, entityField ) -> {
            final Map<String, Double> latLong = ( Map<String, Double> ) entityField.get( IndexingUtils.FIELD_LOCATION );

            assertEquals( Double.valueOf( 10 ),
                latLong.get( "lat" ) );
            assertEquals( Double.valueOf( 20 ),
                latLong.get( "lon" ) );
        } );
    }


    /**
     * Test the single field in our root level
     */
    public <T> void testSingleField( final Field<T> field, Action2<Field, EntityField> assertFunction ) {
        Entity entity = new Entity( "test" );
        entity.setField( field );


        final UUID version = UUIDGenerator.newTimeUUID();

        EntityUtils.setVersion( entity, version );

        final ApplicationScope scope = new ApplicationScopeImpl( createId( "application" ) );

        final IndexEdge indexEdge =
            new IndexEdgeImpl( createId( "source" ), "testEdgeType", SearchEdge.NodeType.SOURCE, 1000 );


        final Map<String, Object> entityMap = EntityToMapConverter.convert( scope, indexEdge, entity );


        final List<EntityField> fields = ( List<EntityField> ) entityMap.get( IndexingUtils.ENTITY_FIELDS );

        assertEquals( 1, fields.size() );

        final EntityField esField = fields.get( 0 );

        //always test the lower case arrays
        final String fieldName = field.getName().toLowerCase();

        assertEquals( fieldName, esField.get( IndexingUtils.FIELD_NAME ) );

        assertFunction.call( field, esField );
    }


    @Test
    public void testStringArray() {


        final ArrayField<String> array = new ArrayField<>( "strings" );

        array.add( "one" );
        array.add( "two" );
        array.add( "three" );

        testPrimitiveArray( array, IndexingUtils.FIELD_STRING );
    }


    @Test
    public void testBooleanArray() {


        final ArrayField<Boolean> array = new ArrayField<>( "bools" );

        array.add( true );
        array.add( true );
        array.add( false );
        array.add( true );

        testPrimitiveArray( array, IndexingUtils.FIELD_BOOLEAN );
    }


    @Test
    public void testIntArray() {


        final ArrayField<Integer> array = new ArrayField<>( "ints" );

        array.add( 1 );
        array.add( 2 );
        array.add( 3 );

        testPrimitiveArray( array, IndexingUtils.FIELD_LONG );
    }


    @Test
    public void testLongArray() {


        final ArrayField<Long> array = new ArrayField<>( "longs" );

        array.add( 1l );
        array.add( 2l );
        array.add( 3l );

        testPrimitiveArray( array, IndexingUtils.FIELD_LONG );
    }


    @Test
    public void testFloatArray() {


        final ArrayField<Float> array = new ArrayField<>( "floats" );

        array.add( 1.0f );
        array.add( 2.0f );
        array.add( 3.0f );

        testPrimitiveArray( array, IndexingUtils.FIELD_DOUBLE );
    }


    @Test
    public void testDoubleArray() {


        final ArrayField<Double> array = new ArrayField<>( "doubles" );

        array.add( 1.0d );
        array.add( 2.0d );
        array.add( 3.0d );

        testPrimitiveArray( array, IndexingUtils.FIELD_DOUBLE );
    }


    /**
     * Test primitive arrays in the root of an object
     *
     * @param indexType the field name for the expected type in ES
     */
    private <T> void testPrimitiveArray( final ArrayField<T> array, final String indexType ) {

        Entity entity = new Entity( "test" );


        entity.setField( array );


        final UUID version = UUIDGenerator.newTimeUUID();

        EntityUtils.setVersion( entity, version );

        final ApplicationScope scope = new ApplicationScopeImpl( createId( "application" ) );

        final IndexEdge indexEdge =
            new IndexEdgeImpl( createId( "source" ), "testEdgeType", SearchEdge.NodeType.SOURCE, 1000 );


        final Map<String, Object> entityMap = EntityToMapConverter.convert( scope, indexEdge, entity );


        final List<EntityField> fields = ( List<EntityField> ) entityMap.get( IndexingUtils.ENTITY_FIELDS );

        assertEquals( array.size(), fields.size() );


        for ( int i = 0; i < array.size(); i++ ) {
            final EntityField field = fields.get( i );

            assertEquals( array.getName(), field.get( IndexingUtils.FIELD_NAME ) );

            assertEquals( array.getValue().get( i ), field.get( indexType ) );
        }
    }


    @Test
    public void testStringFieldSubObject() {
        testNestedField( new StringField( "name", "value" ),
            ( field, entityField ) -> assertEquals( field.getValue(), entityField.get( IndexingUtils.FIELD_STRING ) ) );
    }


    @Test
    public void testBooleanFieldSubObject() {
        testNestedField( new BooleanField( "name", true ), ( field, entityField ) -> assertEquals( field.getValue(),
            entityField.get( IndexingUtils.FIELD_BOOLEAN ) ) );
    }


    @Test
    public void testIntegerFieldSubObject() {
        testNestedField( new IntegerField( "name", 100 ),
            ( field, entityField ) -> assertEquals( field.getValue(), entityField.get( IndexingUtils.FIELD_LONG ) ) );
    }


    @Test
    public void testLongFieldSubObject() {
        testNestedField( new LongField( "name", 100l ),
            ( field, entityField ) -> assertEquals( field.getValue(), entityField.get( IndexingUtils.FIELD_LONG ) ) );
    }


    @Test
    public void testFloadFieldSubObject() {
        testNestedField( new FloatField( "name", 1.10f ),
            ( field, entityField ) -> assertEquals( field.getValue(), entityField.get( IndexingUtils.FIELD_DOUBLE ) ) );
    }


    @Test
    public void testDoubleFieldSubObject() {
        testNestedField( new DoubleField( "name", 2.20d ),
            ( field, entityField ) -> assertEquals( field.getValue(), entityField.get( IndexingUtils.FIELD_DOUBLE ) ) );
    }


    @Test
    public void testLocationFieldSubObject() {
        testNestedField( new LocationField( "name", new Location( 10, 20 ) ), ( field, entityField ) -> {
            final Map<String, Double> latLong = ( Map<String, Double> ) entityField.get( IndexingUtils.FIELD_LOCATION );

            assertEquals( Double.valueOf( 10 ),
                latLong.get( "lat" ) );
            assertEquals( Double.valueOf( 20 ),
                latLong.get( "lon" ) );
        } );
    }


    /**
     * Test primitive arrays in the root of an object
     *
     * @param storedField The field stored on the nested object
     * @param assertFunction The function
     */
    private <T> void testNestedField( final Field<T> storedField, Action2<Field, EntityField> assertFunction ) {

        EntityObject leafEntity = new EntityObject();

        leafEntity.setField( storedField );


        final EntityObjectField nested2Field = new EntityObjectField( "nested2", leafEntity );

        final EntityObject nested2 = new EntityObject();
        nested2.setField( nested2Field );

        final EntityObjectField nested1Field = new EntityObjectField( "nested1", nested2 );


        Entity rootEntity = new Entity( "test" );


        rootEntity.setField( nested1Field );


        final UUID version = UUIDGenerator.newTimeUUID();

        EntityUtils.setVersion( rootEntity, version );

        final ApplicationScope scope = new ApplicationScopeImpl( createId( "application" ) );

        final IndexEdge indexEdge =
            new IndexEdgeImpl( createId( "source" ), "testEdgeType", SearchEdge.NodeType.SOURCE, 1000 );


        final Map<String, Object> entityMap = EntityToMapConverter.convert( scope, indexEdge, rootEntity );


        final List<EntityField> fields = ( List<EntityField> ) entityMap.get( IndexingUtils.ENTITY_FIELDS );

        assertEquals( 1, fields.size() );


        for ( int i = 0; i < fields.size(); i++ ) {
            final EntityField field = fields.get( i );

            final String path = "nested1.nested2." + storedField.getName();

            assertEquals( path, field.get( IndexingUtils.FIELD_NAME ) );


            assertFunction.call( storedField, field );
        }
    }


    @Test
    public void testStringFieldSubObjectArray() {
        testNestedFieldArraySubObject( new StringField( "name", "value" ),
            ( field, entityField ) -> assertEquals( field.getValue(), entityField.get( IndexingUtils.FIELD_STRING ) ) );
    }


    @Test
    public void testBooleanFieldSubObjectArray() {
        testNestedFieldArraySubObject( new BooleanField( "name", true ),
            ( field, entityField ) -> assertEquals( field.getValue(),
                entityField.get( IndexingUtils.FIELD_BOOLEAN ) ) );
    }


    @Test
    public void testIntegerFieldSubObjectArray() {
        testNestedFieldArraySubObject( new IntegerField( "name", 100 ),
            ( field, entityField ) -> assertEquals( field.getValue(), entityField.get( IndexingUtils.FIELD_LONG ) ) );
    }


    @Test
    public void testLongFieldSubObjectArray() {
        testNestedFieldArraySubObject( new LongField( "name", 100l ),
            ( field, entityField ) -> assertEquals( field.getValue(), entityField.get( IndexingUtils.FIELD_LONG ) ) );
    }


    @Test
    public void testFloadFieldSubObjectArray() {
        testNestedFieldArraySubObject( new FloatField( "name", 1.10f ),
            ( field, entityField ) -> assertEquals( field.getValue(), entityField.get( IndexingUtils.FIELD_DOUBLE ) ) );
    }


    @Test
    public void testDoubleFieldSubObjectArray() {
        testNestedFieldArraySubObject( new DoubleField( "name", 2.20d ),
            ( field, entityField ) -> assertEquals( field.getValue(), entityField.get( IndexingUtils.FIELD_DOUBLE ) ) );
    }


    @Test
    public void testLocationFieldSubObjectArray() {
        testNestedFieldArraySubObject( new LocationField( "name", new Location( 10, 20 ) ), ( field, entityField ) -> {
            final Map<String, Double> latLong = ( Map<String, Double> ) entityField.get( IndexingUtils.FIELD_LOCATION );

            assertEquals( Double.valueOf( 10 ),
                latLong.get("lat" ) );
            assertEquals( Double.valueOf( 20 ),
                latLong.get("lon" ) );
        } );
    }


    /**
     * Test primitive arrays in the root of an object
     *
     * @param storedField The field stored on the nested object
     * @param assertFunction The function
     */
    private <T> void testNestedFieldArraySubObject( final Field<T> storedField,
                                                    Action2<Field, EntityField> assertFunction ) {

        EntityObject leafEntity = new EntityObject();

        leafEntity.setField( storedField );


        final EntityObjectField nested2Field = new EntityObjectField( "nested2", leafEntity );

        final EntityObject nested2 = new EntityObject();
        nested2.setField( nested2Field );

        final EntityObjectField nested1Field = new EntityObjectField( "nested1", nested2 );

        final EntityObject nested1 = new EntityObject();
        nested1.setField( nested1Field );


        final ArrayField<EntityObject> array = new ArrayField<>( "array" );

        array.add( nested1 );

        Entity rootEntity = new Entity( "test" );

        rootEntity.setField( array );


        final UUID version = UUIDGenerator.newTimeUUID();

        EntityUtils.setVersion( rootEntity, version );

        final ApplicationScope scope = new ApplicationScopeImpl( createId( "application" ) );

        final IndexEdge indexEdge =
            new IndexEdgeImpl( createId( "source" ), "testEdgeType", SearchEdge.NodeType.SOURCE, 1000 );


        final Map<String, Object> entityMap = EntityToMapConverter.convert( scope, indexEdge, rootEntity );


        final List<EntityField> fields = ( List<EntityField> ) entityMap.get( IndexingUtils.ENTITY_FIELDS );

        assertEquals( 1, fields.size() );


        for ( int i = 0; i < fields.size(); i++ ) {
            final EntityField field = fields.get( i );

            final String path = "array.nested1.nested2." + storedField.getName();

            assertEquals( path, field.get( IndexingUtils.FIELD_NAME ) );


            assertFunction.call( storedField, field );
        }
    }


    /**
     * 2d + arrays aren't supported, ensure we drop elements with a depth > 1
     */
    @Test
    public void testNDimensionalArray() {


        //create an object with a string value in it, this should be indexed
        final StringField string = new StringField( "string", "value" );

        final EntityObject nested1 = new EntityObject();
        nested1.setField( string );


        //create a sub array, this shouldn't get indexed
        final ArrayField<String> subArray = new ArrayField<>( "subArray" );

        subArray.add( "test2" );

        final ArrayField<Object> array = new ArrayField<>( "array" );

        //not indexed
        array.add( subArray );

        //indexed
        array.add( nested1 );


        Entity rootEntity = new Entity( "test" );

        rootEntity.setField( array );


        final UUID version = UUIDGenerator.newTimeUUID();

        EntityUtils.setVersion( rootEntity, version );

        final ApplicationScope scope = new ApplicationScopeImpl( createId( "application" ) );

        final IndexEdge indexEdge =
            new IndexEdgeImpl( createId( "source" ), "testEdgeType", SearchEdge.NodeType.SOURCE, 1000 );


        final Map<String, Object> entityMap = EntityToMapConverter.convert( scope, indexEdge, rootEntity );


        final List<EntityField> fields = ( List<EntityField> ) entityMap.get( IndexingUtils.ENTITY_FIELDS );

        assertEquals( 1, fields.size() );


        final EntityField field = fields.get( 0 );

        final String path = "array.string";

        assertEquals( path, field.get( IndexingUtils.FIELD_NAME ) );


        assertEquals( "value", field.get( IndexingUtils.FIELD_STRING ) );
    }
}
