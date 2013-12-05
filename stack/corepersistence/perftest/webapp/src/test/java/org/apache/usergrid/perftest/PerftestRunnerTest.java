package org.apache.usergrid.perftest;

import com.google.inject.Guice;
import org.junit.Test;

import org.apache.usergrid.perftest.amazon.AmazonS3Module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
public class PerftestRunnerTest {
    @Test
    public void testPerftestRunner() throws InterruptedException {
        PerftestRunner runner = Guice.createInjector( new PerftestModule(),
                new AmazonS3Module() ).getInstance( PerftestRunner.class );
        assertFalse( runner.isRunning() );

        runner.start();
        assertTrue( runner.isRunning() );

        while ( runner.isRunning() )
        {
            Thread.sleep( 100 );
        }

        runner.stop();
        assertFalse( runner.isRunning() );

        assertEquals( 1000, runner.getCallStatsSnapshot().getCallCount() );
    }
}
