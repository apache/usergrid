/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.serialization.impl.UniqueFieldRowKeySerializer;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.IntegerField;

import com.netflix.astyanax.model.CompositeBuilder;
import com.netflix.astyanax.model.CompositeParser;
import com.netflix.astyanax.model.Composites;

public class UniqueFieldRowKeySerializerTest {

    @Test
    public void testBasicOperation() {

        Field original = new IntegerField( "count", 5 );

        CompositeBuilder builder = Composites.newCompositeBuilder();
        UniqueFieldRowKeySerializer fs = new UniqueFieldRowKeySerializer();
        fs.toComposite( builder, original );
        ByteBuffer serializer = builder.build();

        CompositeParser parser = Composites.newCompositeParser( serializer );

        Field deserialized = fs.fromComposite( parser );

        Assert.assertEquals( original, deserialized );
    }
}
