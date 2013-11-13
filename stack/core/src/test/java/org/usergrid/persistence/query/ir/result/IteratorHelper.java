package org.usergrid.persistence.query.ir.result;


import java.nio.ByteBuffer;
import java.util.UUID;


/**
 *
 * @author: tnine
 *
 */
public class IteratorHelper {

    public static ScanColumn uuidColumn( UUID value ) {
        return new UUIDIndexSliceParser.UUIDColumn( value, ByteBuffer.allocate( 0 ) );
    }
}
