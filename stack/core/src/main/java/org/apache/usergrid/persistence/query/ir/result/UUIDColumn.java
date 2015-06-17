package org.apache.usergrid.persistence.query.ir.result;


import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.usergrid.utils.UUIDUtils;


/**
 * Used as a comparator for columns
 */
class UUIDColumn implements  ScanColumn{

    private final UUID uuid;
    private final int compareReversed;
    private ScanColumn child;


    UUIDColumn( final UUID uuid, final int compareReversed ) {
        this.uuid = uuid;
        this.compareReversed = compareReversed;
    }


    @Override
    public UUID getUUID() {
        return uuid;
    }


    @Override
    public ByteBuffer getCursorValue() {
        return null;
    }


    @Override
    public void setChild( final ScanColumn childColumn ) {
        this.child = childColumn;
    }


    @Override
    public ScanColumn getChild() {
        return child;
    }


    @Override
    public int compareTo( final ScanColumn other ) {

        return  UUIDUtils.compare( uuid, other.getUUID() ) * compareReversed;

    }
}
