package org.apache.usergrid.perftest.rest;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Testing the LockableInputStream.
 */
public class LockableInputStreamTest {
    private static final Logger LOG = LoggerFactory.getLogger( LockableInputStreamTest.class );

    @Test
    public void testBlockage() throws Exception {
        // First let's write some bogus data to a temporary file
        File bogusFile = File.createTempFile( "foobogus", "bar" );
        FileOutputStream out = new FileOutputStream( bogusFile );
        final byte[] buf = new byte[1000];

        // write out about ~100M to the temp file
        for ( int ii = 0; ii < 100000; ii++ )
        {
            out.write( buf );
        }
        out.flush();
        out.close();

        LOG.info( "Filled up the file to about 100M" );
        int total = 100000 * buf.length;
        assertEquals( "we wrote 1000 bytes 100000 times", total, bogusFile.length() );


        int limit = 99900000;
        final LockableInputStream in = new LockableInputStream( bogusFile, limit );

        LOG.info( "about to start the reader thread" );
        new Thread( new Runnable() {
            @Override
            public void run() {
                try {
                    int total = 0;
                    int readCount;
                    LOG.info( "before call to read" );
                    while ( ( readCount = in.read( buf ) ) != -1 ) {
                        total += readCount;
                        if ( total % 10000000 == 0 )
                        {
                            LOG.info( "Read {} bytes so far.", total );
                        }
                    }
                }
                catch ( Exception e ) {
                    e.printStackTrace();
                }
            }
        }).start();


        in.returnOnLimit();
        assertTrue( in.isBlockedAtLimit() );
        assertEquals( limit - buf.length, in.getReadCount() );

        in.deactivateLimit();
        Thread.sleep( 200 );
        assertEquals( total-1, in.getReadCount() );
    }
}
