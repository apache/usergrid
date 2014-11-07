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
package org.apache.usergrid.persistence.core.shard;


import java.nio.charset.Charset;

import org.junit.Test;

import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

import static org.junit.Assert.assertEquals;


/**
 * Simple test that validates hashing is actually consistent as buckets grow
 */
public class ShardLocatorTest {

    public static final Funnel<String> STRING_FUNNEL = new Funnel<String>() {

        private Charset UTF8 = Charset.forName( "UTF8" );


        @Override
        public void funnel( final String from, final PrimitiveSink into ) {
            into.putString( from, UTF8 );
        }
    };


    @Test
    public void stringHashing() {

        final String hashValue = "keystring";

        ShardLocator<String> shardLocator1 = new ShardLocator<>(STRING_FUNNEL,  100 );

        int index1 = shardLocator1.getBucket( hashValue );

        ShardLocator<String> shardLocator2 = new ShardLocator<>( STRING_FUNNEL, 100 );

        int index2 = shardLocator2.getBucket( hashValue );

        assertEquals( "Same index expected", index1, index2 );
    }
}
