package org.usergrid.persistence.query.ir.result;


import java.nio.ByteBuffer;
import java.util.UUID;


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
        this.buffer = buffer.duplicate();
    }


    @Override
    public UUID getUUID() {
        return uuid;
    }


    @Override
    public ByteBuffer getCursorValue() {
        return buffer.duplicate();
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
}
