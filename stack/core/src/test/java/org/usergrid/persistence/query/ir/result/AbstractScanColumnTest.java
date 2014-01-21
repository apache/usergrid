package org.usergrid.persistence.query.ir.result;


import java.nio.ByteBuffer;
import java.util.UUID;

import org.junit.Test;
import org.usergrid.utils.UUIDUtils;

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
