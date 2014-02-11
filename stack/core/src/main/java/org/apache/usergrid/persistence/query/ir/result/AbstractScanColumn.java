package org.apache.usergrid.persistence.query.ir.result;


import java.nio.ByteBuffer;
import java.util.UUID;
import org.apache.cassandra.utils.ByteBufferUtil;


/**
 *
 * @author: tnine
 *
 */
public abstract class AbstractScanColumn implements ScanColumn {

    private final UUID uuid;
    private final ByteBuffer buffer;


    protected AbstractScanColumn( UUID uuid, ByteBuffer buffer ) {
        this.uuid = uuid;
        this.buffer = buffer;
    }


    @Override
    public UUID getUUID() {
        return uuid;
    }


    @Override
    public ByteBuffer getCursorValue() {
        return buffer == null ? null :buffer.duplicate();
    }


    @Override
    public boolean equals( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof AbstractScanColumn ) ) {
            return false;
        }

        AbstractScanColumn that = ( AbstractScanColumn ) o;

        if ( !uuid.equals( that.uuid ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        return uuid.hashCode();
    }


    @Override
    public String toString() {
        return "AbstractScanColumn{" +
                "uuid=" + uuid +
                ", buffer=" + ByteBufferUtil.bytesToHex( buffer ) +
                '}';
    }
}
