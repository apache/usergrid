package org.apache.usergrid.perftest;


import org.apache.usergrid.perftest.rest.CallStatsSnapshot;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Invokes a Perftest based on a CallSpec.
 */
@Singleton
public class PerftestRunner implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger( PerftestRunner.class );


    private final TestModuleLoader loader;
    private final Injector injector;
    private final Object lock = new Object();
    private DynamicLongProperty sleepToStop =
            DynamicPropertyFactory.getInstance().getLongProperty( "sleep.to.stop", 100 );
    private CallStats stats;
    private Perftest test;
    private ExecutorService executorService;
    private boolean stopSignal = false;
    private boolean running = false;
    private boolean needsReset = false;
    private long startTime;
    private long stopTime;


    @Inject
    public PerftestRunner( Injector injector, TestModuleLoader loader )
    {
        this.loader = loader;
        this.injector = injector;
        setup();
    }


    public void setup() {
        synchronized ( lock ) {
            stopSignal = false;
            running = false;
            startTime = 0;
            stopTime = 0;

            if ( stats != null )
            {
                stats.reset();
            }

            stats = injector.getInstance( CallStats.class );
            test = loader.getChildInjector().getInstance( Perftest.class );
            executorService = Executors.newFixedThreadPool( test.getThreadCount() );
            needsReset = false;
        }
    }


    public CallStatsSnapshot getCallStatsSnapshot() {
        return stats.getStatsSnapshot( isRunning(), getStartTime(), getStopTime() );
    }


    public boolean isRunning() {
        synchronized ( lock ) {
            return running;
        }
    }


    public boolean needsReset() {
        synchronized ( lock )
        {
            return needsReset;
        }
    }


    public long getStartTime() {
        return startTime;
    }


    public long getStopTime() {
        return stopTime;
    }


    public void start() {
        synchronized ( lock ) {
            stopSignal = false;
            startTime = System.nanoTime();
            running = true;

            for ( int ii = 0; ii < test.getThreadCount(); ii++) {
                executorService.execute( this );
            }
        }

        // launch a coordinator thread to detect when all others are done
        new Thread( new Runnable() {
            @Override
            public void run() {
                try {
                    while ( executorService.awaitTermination( sleepToStop.get(), TimeUnit.MILLISECONDS ) ) {
                        LOG.info( "woke up running = {}", PerftestRunner.this.running );
                    }
                }
                catch ( InterruptedException e ) {
                    LOG.error( "Got interrupted while monitoring executor service.", e );
                }

                stats.stop();

                LOG.info( "COORDINATOR THREAD: all threads have died." );
                PerftestRunner.this.running = false;
                PerftestRunner.this.needsReset = true;
                stopTime = System.nanoTime();
            }
        } ).start();
    }


    public void stop() {
        synchronized ( lock ) {
            stopSignal = true;

            try {
                while ( executorService.awaitTermination( sleepToStop.get(), TimeUnit.MILLISECONDS ) ) {
                    LOG.info( "woke up: running = {}", PerftestRunner.this.running );
                }
            }
            catch ( InterruptedException e ) {
                LOG.error( "Got interrupted while monitoring executor service.", e );
            }

            running = false;
            stats.stop();
            stopTime = System.nanoTime();
            needsReset = true;
        }
    }

    @Override
    public void run() {
        int delay = test.getDelayBetweenCalls();

        while( ( ! stopSignal ) && ( stats.getCallCount() < test.getCallCount() ) ) {
            long startTime = System.nanoTime();
            test.call();
            long endTime = System.nanoTime();
            stats.callOccurred( test, startTime, endTime, TimeUnit.NANOSECONDS );

            if ( delay > 0 )
            {
                try {
                    Thread.sleep( delay );
                } catch ( InterruptedException e ) {
                    LOG.error( "Thread was interrupted.", e );
                }
            }

            synchronized ( lock ) {
                lock.notifyAll();
            }
        }
    }
}
