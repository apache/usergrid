package org.apache.usergrid.persistence.model.util;


import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.NoArgGenerator;
import com.fasterxml.uuid.TimestampSynchronizer;
import com.fasterxml.uuid.UUIDTimer;
import com.fasterxml.uuid.UUIDType;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import com.fasterxml.uuid.impl.UUIDUtil;


/**
 * TODO replace this with the Astyanax generator libs
 */
public class UUIDGenerator {


    private static final Random random = new Random();
    private static final UUIDTimer timedTimer;
    private static final UUIDTimer forcedTimer;


    private static final InternalTimestampSynchronizer timeSynchronizer = new InternalTimestampSynchronizer();

    private static final InternalTimestampSynchronizer forceSynchronizer = new InternalTimestampSynchronizer();


    /**
     * Lame, but required
     */
    static {
        try {
            timedTimer = new UUIDTimer( random, timeSynchronizer );
            forcedTimer = new UUIDTimer( random, forceSynchronizer );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Couldn't intialize timer", e );
        }
    }


    private static final TimeBasedGenerator generator =
            new TimeBasedGenerator( EthernetAddress.fromInterface(), timedTimer );


    private static final FutureTimeBasedGenerator FUTURE_TIME_BASED_GENERATOR =
            new FutureTimeBasedGenerator( EthernetAddress.fromInterface(), forcedTimer, random );


    /**
     * Create a new time uuid
     */
    public static UUID newTimeUUID() {
        return generator.generate();
    }

    public static UUID newTimeUUID(long ts){
        return FUTURE_TIME_BASED_GENERATOR.generate( ts );
    }


    /**
     * Implementation of UUID generator that uses time/location based generation method (variant 1). <p> As all JUG
     * provided implementations, this generator is fully thread-safe. Additionally it can also be made externally
     * synchronized with other instances (even ones running on other JVMs); to do this, use {@link
     * com.fasterxml.uuid.ext .FileBasedTimestampSynchronizer} (or equivalent).
     *
     * @since 3.0
     */
    private static class FutureTimeBasedGenerator extends NoArgGenerator {


        /**
         * Since System.longTimeMillis() returns time from january 1st 1970,
         * and UUIDs need time from the beginning of gregorian calendar
         * (15-oct-1582), need to apply the offset:
         */
        private final static long kClockOffset = 0x01b21dd213814000L;
        /**
         * Also, instead of getting time in units of 100nsecs, we get something
         * with max resolution of 1 msec... and need the multiplier as well
         */
        private final static int kClockMultiplier = 10000;

        private final static long kClockMultiplierL = 10000L;

        /**
         * Let's allow "virtual" system time to advance at most 100 milliseconds
         * beyond actual physical system time, before adding delays.
         */
        private final static long kMaxClockAdvance = 100L;

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

        protected final EthernetAddress _ethernetAddress;

        /**
         * Object used for synchronizing access to timestamps, to guarantee that timestamps produced by this generator
         * are unique and monotonically increasings. Some implementations offer even stronger guarantees, for example
         * that same guarantee holds between instances running on different JVMs (or with native code).
         */
        protected final UUIDTimer _timer;

        /**
         * Base values for the second long (last 8 bytes) of UUID to construct
         */
        protected final long _uuidL2;


        protected final Random random;
    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */


        /**
         * @param ethAddr Hardware address (802.1) to use for generating spatially unique part of UUID. If system has more
         * than one NIC,
         * @param timer The timer to use
         * @param random The randomizer to user
         */

        public FutureTimeBasedGenerator( EthernetAddress ethAddr, UUIDTimer timer, Random random ) {
            byte[] uuidBytes = new byte[16];
            if ( ethAddr == null ) {
                ethAddr = EthernetAddress.constructMulticastAddress();
            }
            // initialize baseline with MAC address info
            _ethernetAddress = ethAddr;
            _ethernetAddress.toByteArray( uuidBytes, 10 );
            // and add clock sequence
            int clockSeq = timer.getClockSequence();
            uuidBytes[UUIDUtil.BYTE_OFFSET_CLOCK_SEQUENCE] = ( byte ) ( clockSeq >> 8 );
            uuidBytes[UUIDUtil.BYTE_OFFSET_CLOCK_SEQUENCE + 1] = ( byte ) clockSeq;
            long l2 = gatherLong( uuidBytes, 8 );
            _uuidL2 = UUIDUtil.initUUIDSecondLong( l2 );
            _timer = timer;
            this.random = random;
        }

    /*
    /**********************************************************************
    /* Access to config
    /**********************************************************************
     */


        @Override
        public UUIDType getType() { return UUIDType.TIME_BASED; }


    /*
    /**********************************************************************
    /* UUID generation
    /**********************************************************************
     */


        /* As timer is not synchronized (nor _uuidBytes), need to sync; but most
         * importantly, synchronize on timer which may also be shared between
         * multiple instances
         */


        public UUID generate( long rawTimestamp ) {


            long newTimestamp = rawTimestamp * kClockMultiplierL;

            newTimestamp += kClockOffset;
            newTimestamp += getClockCounter();


            // Time field components are kind of shuffled, need to slice:
            int clockHi = ( int ) ( newTimestamp >>> 32 );
            int clockLo = ( int ) newTimestamp;
            // and dice
            int midhi = ( clockHi << 16 ) | ( clockHi >>> 16 );
            // need to squeeze in type (4 MSBs in byte 6, clock hi)
            midhi &= ~0xF000; // remove high nibble of 6th byte
            midhi |= 0x1000; // type 1
            long midhiL = ( long ) midhi;
            midhiL = ( ( midhiL << 32 ) >>> 32 ); // to get rid of sign extension
            // and reconstruct
            long l1 = ( ( ( long ) clockLo ) << 32 ) | midhiL;
            // last detail: must force 2 MSB to be '10'
            return new UUID( l1, _uuidL2 );
        }


        private int getClockCounter()
           {
               /* Let's generate the clock sequence field now; as with counter,
                * this reduces likelihood of collisions (as explained in UUID specs)
                */
               int clockSequence = random.nextInt();
               /* Ok, let's also initialize the counter...
                * Counter is used to make it slightly less likely that
                * two instances of UUIDGenerator (from separate JVMs as no more
                * than one can be created in one JVM) would produce colliding
                * time-based UUIDs. The practice of using multiple generators,
                * is strongly discouraged, of course, but just in case...
                */
               int clockCounter = (clockSequence >> 16) & 0xFF;

               return clockCounter;
           }


        //private final static long MASK_LOW_INT = 0x0FFFFFFFF;


        protected final static long gatherLong( byte[] buffer, int offset ) {
            long hi = ( ( long ) _gatherInt( buffer, offset ) ) << 32;
            //long lo = ((long) _gatherInt(buffer, offset+4)) & MASK_LOW_INT;
            long lo = ( ( ( long ) _gatherInt( buffer, offset + 4 ) ) << 32 ) >>> 32;
            return hi | lo;
        }


        private final static int _gatherInt( byte[] buffer, int offset ) {
            return ( buffer[offset] << 24 ) | ( ( buffer[offset + 1] & 0xFF ) << 16 ) | ( ( buffer[offset + 2] & 0xFF )
                    << 8 ) | ( buffer[offset + 3] & 0xFF );
        }


        @Override
        public UUID generate() {
            throw new UnsupportedOperationException( "Use generate with a timestamp instead" );
        }
    }


    private static class InternalTimestampSynchronizer extends TimestampSynchronizer {

        /**
         * Pointer to the last value we returned
         */
        private long last = 0;

        /**
         * The number of ticks that can be used in the millisecond.  In a time UUID a tick is divided into 1/10000
         * of a millisecond
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
    }


    ;
}
