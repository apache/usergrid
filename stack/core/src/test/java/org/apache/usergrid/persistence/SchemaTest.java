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
package org.apache.usergrid.persistence;


import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.Simple;

import org.apache.usergrid.persistence.entities.SampleEntity;

import static org.apache.usergrid.utils.JsonUtils.mapToFormattedJsonString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class SchemaTest {
    private static final Logger LOG = LoggerFactory.getLogger( SchemaTest.class );


    @Test
    public void testTypes() throws Exception {
        LOG.info( "" + Schema.getDefaultSchema().getEntityClass( "sample_entity" ) );
        LOG.info( "" + Schema.getDefaultSchema().getEntityType( SampleEntity.class ) );

        SampleEntity entity = new SampleEntity();
        LOG.info( entity.getType() );
    }


    @Test
    public void testThirdPartyEntityTypes() throws Exception {
        String thirdPartyPackage = "org.usergrid";
        Schema schema = Schema.getDefaultSchema();
        schema.addEntitiesPackage( thirdPartyPackage );
        schema.scanEntities();

        List<String> entitiesPackage = schema.getEntitiesPackage();
        for ( String entityPackage : entitiesPackage ) {
            LOG.info( entityPackage );
        }

        Assert.assertEquals( schema.getEntityClass( "simple" ), Simple.class );
        Assert.assertEquals( schema.getEntityType( Simple.class ), "simple" );

        Simple entity = new Simple();
        LOG.info( entity.getType() );
    }


    @Test
    public void testSchema() throws Exception {

        dumpSetNames( "application" );
        dumpSetNames( "user" );
        dumpSetNames( "thing" );
    }


    public void dumpSetNames( String entityType ) {
        LOG.info( entityType + " entity has the following sets: " + Schema.getDefaultSchema()
                                                                          .getDictionaryNames( entityType ) );
    }


    @Test
    public void testJsonSchema() throws Exception {

        LOG.info( mapToFormattedJsonString( Schema.getDefaultSchema().getEntityJsonSchema( "user" ) ) );

        LOG.info( mapToFormattedJsonString( Schema.getDefaultSchema().getEntityJsonSchema( "test" ) ) );
    }


    @Test
    public void hasPropertyTyped() {
        assertFalse( Schema.getDefaultSchema().hasProperty( "user", "" ) );
        assertTrue( Schema.getDefaultSchema().hasProperty( "user", "username" ) );
    }


    @Test
    public void hasPropertyDynamic() {

        assertFalse( Schema.getDefaultSchema().hasProperty( "things", "" ) );

        assertFalse( Schema.getDefaultSchema().hasProperty( "things", "foo" ) );
    }


    /** Should always return true */
    @Test
    public void indexedTyped() {

        assertTrue( Schema.getDefaultSchema().isPropertyIndexed( "user", "" ) );

        assertTrue( Schema.getDefaultSchema().isPropertyIndexed( "user", "username" ) );
    }


    /** Should always return true for dynamic types */
    @Test
    public void indexedUnTyped() {


        assertTrue( Schema.getDefaultSchema().isPropertyIndexed( "things", "" ) );

        assertTrue( Schema.getDefaultSchema().isPropertyIndexed( "things", "foo" ) );
    }
}
