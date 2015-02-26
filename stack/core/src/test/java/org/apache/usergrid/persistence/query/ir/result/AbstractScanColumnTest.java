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
package org.apache.usergrid.persistence.query.ir.result;


import java.nio.ByteBuffer;
import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.utils.UUIDUtils;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;


/**
 * Simple test to test null value
 */
public class AbstractScanColumnTest {

    @Test
    public void testValues() {
        final UUID uuid = UUIDUtils.newTimeUUID();
        final ByteBuffer buffer = ByteBuffer.allocate( 4 );
        buffer.putInt( 1 );
        buffer.rewind();

        TestScanColumn col = new TestScanColumn( uuid, buffer );

        assertSame( uuid, col.getUUID() );

        assertEquals( 1, col.getCursorValue().getInt() );
    }


    @Test
    public void nullUUID() {
        final UUID uuid = null;
        final ByteBuffer buffer = ByteBuffer.allocate( 4 );
        buffer.putInt( 1 );
        buffer.rewind();

        TestScanColumn col = new TestScanColumn( uuid, buffer );

        assertNull( col.getUUID() );

        assertEquals( 1, col.getCursorValue().getInt() );
    }


    @Test
    public void nullBuffer() {
        final UUID uuid = UUIDUtils.newTimeUUID();
        final ByteBuffer buffer = null;

        TestScanColumn col = new TestScanColumn( uuid, buffer );

        assertSame( uuid, col.getUUID() );

        assertNull( col.getCursorValue() );
    }


    @Test
    public void nullBoth() {
        final UUID uuid = null;
        final ByteBuffer buffer = null;

        TestScanColumn col = new TestScanColumn( uuid, buffer );

        assertNull( col.getUUID() );

        assertNull( col.getCursorValue() );
    }




    private class TestScanColumn extends AbstractScanColumn {

        protected TestScanColumn( final UUID uuid, final ByteBuffer buffer ) {
            super( uuid, buffer );
        }
    }
}
