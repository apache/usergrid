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
package org.apache.usergrid.persistence;


import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.Application;
import org.apache.usergrid.CoreApplication;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.guicyfig.SetConfigTestBypass;
import org.apache.usergrid.utils.JsonUtils;

import com.google.inject.Injector;

import static org.junit.Assert.assertEquals;




public class LargeEntityIT extends AbstractCoreIT {
    private static final Logger LOG = LoggerFactory.getLogger( LargeEntityIT.class );

    @Rule
    public Application app = new CoreApplication( setup );


    /**
     * Set our max size before and after so we can override it in our runtime tests
     */
    private int setMaxEntitySize;

    private SerializationFig serializationFig;


    @org.junit.Before
    public void setUp() {

        serializationFig = SpringResource.getInstance().getBean( Injector.class ).getInstance( SerializationFig.class );

        setMaxEntitySize = serializationFig.getMaxEntitySize();
    }


    @After
    public void tearDown() {
        SetConfigTestBypass.setValueByPass( serializationFig, "getMaxEntitySize", setMaxEntitySize + "" );
    }


    /**
     * Tests creating a large entity, then loading it, modifying it, saving it, then loading it again
     */
    @Test
    public void testLargeEntityCrud() throws Exception {

        LOG.debug( "testLargeEntityCrud" );

        SetConfigTestBypass.setValueByPass( serializationFig, "getMaxEntitySize", 641834 + "" );


        final URL resource = this.getClass().getClassLoader().getResource( TEST_DATA_FILE );

        final byte[] fileData = Files.readAllBytes( Paths.get( resource.toURI() ) );

        final String fileAsString = new String( fileData, Charset.forName( "UTF-8" ) );

        final Map<String, Object> json = ( Map<String, Object> ) JsonUtils.parse( fileAsString );


        final EntityManager em = app.getEntityManager();

        final Entity createReturned = em.create( "test", json );

        final Entity loadReturnedRef = em.get( createReturned );

        assertEquals( "Entities should be equal", createReturned, loadReturnedRef );

        final Entity loadReturnedId = em.get( createReturned.getUuid() );

        assertEquals( "Entities should be equal", createReturned, loadReturnedId );
    }


    private static final String TEST_DATA_FILE = "largeentity.json";
}
