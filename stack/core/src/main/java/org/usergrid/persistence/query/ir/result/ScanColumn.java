package org.usergrid.persistence.query.ir.result;


import java.nio.ByteBuffer;
import java.util.UUID;


/** An interface that represents a column */
public interface ScanColumn {

    /** Get the uuid from the column */
    public UUID getUUID();

    /** Get the cursor value of this column */
    public ByteBuffer getCursorValue();
}
