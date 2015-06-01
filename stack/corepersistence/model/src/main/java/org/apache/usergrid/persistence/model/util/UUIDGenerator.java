package org.apache.usergrid.persistence.model.util;


import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.UUIDTimer;
import com.fasterxml.uuid.impl.TimeBasedGenerator;


/**
 * TODO replace this with the Astyanax generator libs
 */
public class UUIDGenerator {


    private static final Random random = new Random();
    private static final UUIDTimer timer;

    /**
     * Lame, but required
     */
    static {
        try {
            timer = new UUIDTimer( random, null );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Couldn't initialize timer", e );
        }
    }


    private static final TimeBasedGenerator generator =
        new TimeBasedGenerator( EthernetAddress.fromInterface(), timer );


    /** Create a new time uuid */
    public static UUID newTimeUUID() {
        return generator.generate();
    }
}
