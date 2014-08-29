package org.apache.usergrid.persistence.model.util;


import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.TimestampSynchronizer;
import com.fasterxml.uuid.UUIDTimer;
import com.fasterxml.uuid.impl.TimeBasedGenerator;


/**
 * TODO replace this with the Astyanax generator libs
 * @author: tnine
 *
 */
public class UUIDGenerator {


    private static final TimestampSynchronizer synchronize = new TimestampSynchronizer() {

        /**
         * Pointer to the last value we returned
         */
        private long last = 0;

        /**
         * The number of ticks that can be used in the millisecond.  In a time UUID a tick is divided into 1/10000 of
         * a millisecond
         *
         */
        private AtomicInteger ticks = new AtomicInteger();


        @Override
        protected long initialize() throws IOException {

            last = System.currentTimeMillis();
            return last;
        }


        @Override
        protected void deactivate() throws IOException {
            //no op
        }


        @Override
        protected long update( long now ) throws IOException {
            /**
             * Our timestamp is greater just use that and reset last
             */
            if ( now > last ) {
                last = now;
                ticks.set( 0 );
                return last;
            }

            //we have the same value (since now should always be increasing) increment a tick
            return last + ticks.incrementAndGet();
        }
    };


    private static final Random random = new Random();
    private static final UUIDTimer timer;


    /**
     * Lame, but required
     */
    static {
        try {
            timer = new UUIDTimer( random, synchronize );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Couldn't intialize timer", e );
        }
    }


    private static final TimeBasedGenerator generator = new TimeBasedGenerator( EthernetAddress.fromInterface(), timer );


    /** Create a new time uuid */
    public static UUID newTimeUUID() {
        return generator.generate();
    }
}
