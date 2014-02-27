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
package org.apache.usergrid.count.common;


import org.junit.Test;

import static org.junit.Assert.assertEquals;


/** @author zznate */
public class CountSerDeUtilsTest {

    private static final String SIMPLE_JSON =
            "{\"tableName\":\"Counters\",\"keyName\":\"k1\",\"columnName\":\"c1\",\"value\":1}";

    private static final String MIXED_TYPE_JSON =
            "{\"tableName\":\"Counters\",\"keyName\":1,\"columnName\":\"c1\",\"value\":1}";


    @Test
    public void testSerialize() {
        Count count = new Count( "Counters", "k1", "c1", 1 );
        String sered = CountSerDeUtils.serialize( count );
        assertEquals( SIMPLE_JSON, sered );
    }


    @Test
    public void testDeserializer() {
        Count count = CountSerDeUtils.deserialize( SIMPLE_JSON );
        assertEquals( "k1", count.getKeyName() );
        assertEquals( "c1", count.getColumnName() );
        assertEquals( "Counters", count.getTableName() );
        assertEquals( 1, count.getValue() );
    }


    @Test
    public void testMixedSerializer() {
        Count count = new Count( "Counters", 1, "c1", 1 );
        String sered = CountSerDeUtils.serialize( count );
        assertEquals( MIXED_TYPE_JSON, sered );
    }


    @Test
    public void testMixedDeserializer() {
        Count count = CountSerDeUtils.deserialize( MIXED_TYPE_JSON );
        assertEquals( 1, count.getKeyName() );
        assertEquals( "c1", count.getColumnName() );
        assertEquals( "Counters", count.getTableName() );
        assertEquals( 1, count.getValue() );
    }
}
