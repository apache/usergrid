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

package org.apache.usergrid.persistence.collection.serialization.impl;


import org.junit.Test;

import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Performs basic unit tests on our settings validation
 */
public class SettingsValidationTest {

    /**
     * Tests that when we're within range, it passes
     */
    @Test
    public void withinBounds(){
        CassandraFig cassandraFig = mock(CassandraFig.class);

        final int thriftSize = 15728640;

        final int usableThriftSize = ( int ) (thriftSize * .9);

        when(cassandraFig.getThriftBufferSize()).thenReturn( thriftSize );


        SerializationFig serializationFig = mock(SerializationFig.class);

        when(serializationFig.getMaxEntitySize()).thenReturn( usableThriftSize );

        new  SettingsValidation( cassandraFig, serializationFig);

        when(serializationFig.getMaxEntitySize()).thenReturn( usableThriftSize -1  );

        new  SettingsValidation( cassandraFig, serializationFig);

    }


    /**
     * Tests that when we're within range, it passes
     */
    @Test(expected = IllegalArgumentException.class)
    public void outOfBounds(){
        CassandraFig cassandraFig = mock(CassandraFig.class);

        final int thriftSize = 15728640;

        final int usableThriftSize = ( int ) (thriftSize * .9);

        when(cassandraFig.getThriftBufferSize()).thenReturn( thriftSize );


        SerializationFig serializationFig = mock(SerializationFig.class);

        when(serializationFig.getMaxEntitySize()).thenReturn( usableThriftSize+1 );

        new  SettingsValidation( cassandraFig, serializationFig);



    }



    /**
     * Tests that when we're within range, it passes
     */
    @Test(expected = IllegalArgumentException.class)
    public void zeroBufferSize(){
        CassandraFig cassandraFig = mock(CassandraFig.class);

        final int thriftSize = 0;

        when(cassandraFig.getThriftBufferSize()).thenReturn( thriftSize );

        SerializationFig serializationFig = mock(SerializationFig.class);

        new  SettingsValidation( cassandraFig, serializationFig);



    }

    /**
         * Tests that when we're within range, it passes
         */
        @Test(expected = IllegalArgumentException.class)
        public void zeroEntitySize(){
            CassandraFig cassandraFig = mock(CassandraFig.class);

            final int thriftSize = 15728640;

            when(cassandraFig.getThriftBufferSize()).thenReturn( thriftSize );


            SerializationFig serializationFig = mock(SerializationFig.class);

            when(serializationFig.getMaxEntitySize()).thenReturn( 0 );

            new  SettingsValidation( cassandraFig, serializationFig);



        }
}
