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

package org.apache.usergrid.persistence.collection.serialization.impl;


import org.junit.Test;

import org.apache.usergrid.persistence.core.astyanax.IdRowCompositeSerializer;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.model.Composites;

import static org.junit.Assert.assertEquals;


/** @author tnine */
public class IdRowSerializerTest {

    @Test
    public void testSerialization() {

        Id testId = new SimpleId( "test" );

        IdRowCompositeSerializer rowSerializer = IdRowCompositeSerializer.get();


        final CompositeBuilder builder = Composites.newCompositeBuilder();

        rowSerializer.toComposite( builder, testId );


        final CompositeParser parser = Composites.newCompositeParser( builder.build() );

        //now convert it back

        Id deserialized = rowSerializer.fromComposite( parser );

        assertEquals( "Serialization works correctly", testId, deserialized );
    }


}
