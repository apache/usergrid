package org.apache.usergrid.perftest;


import com.netflix.blitz4j.LoggingConfiguration;
import org.apache.usergrid.perftest.logging.Log;
import org.apache.usergrid.perftest.rest.CallStatsSnapshot;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import org.apache.usergrid.perfteststats.CallStats;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Invokes a Perftest based on a CallSpec.
 */
@Singleton
public class PerftestRunner {
    @Log
    Logger log;


    private final TestModuleLoader loader;
    private final Injector injector;
    private final Object lock = new Object();
    private DynamicLongProperty sleepToStop = DynamicPropertyFactory.getInstance().getLongProperty( "sleep.to.stop", 100 );
    private CallStats stats;
    private List<Thread> threads = new ArrayList<Thread>();
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
            this.threads.clear();
            this.stopSignal = false;
            this.running = false;
            this.startTime = 0;
            this.stopTime = 0;

            this.stats = injector.getInstance( CallStats.class );
            final Perftest test = loader.getChildInjector().getInstance(Perftest.class);

            final long delay = test.getDelayBetweenCalls();
            threads = new ArrayList<Thread>( test.getThreadCount() );
            for ( int ii = 0; ii < test.getThreadCount(); ii++ ) {
                threads.add( new Thread( new Runnable() {
                    @Override
                    public void run() {
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
                                    log.error( "Thread was interrupted.", e );
                                }
                            }

                            synchronized ( lock ) {
                                lock.notifyAll();
                            }
                        }
                    }
                }) );
            }

            this.needsReset = false;
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
            for ( Thread t : threads ) {
                t.start();
            }
        }

        // launch a coordinator thread to detect when all others are done
        new Thread( new Runnable() {
            @Override
            public void run() {
                while ( threadsRunning() )
                {
                    synchronized ( lock )
                    {
                        try {
                            lock.wait( sleepToStop.get() );
                            log.info( "woke up running = {}", PerftestRunner.this.running );
                            lock.notifyAll();
                        } catch (InterruptedException e) {
                            log.error( "Thread interrupted while sleeping", e );
                        }
                    }
                }

                log.info( "COORDINATOR THREAD: all threads have died." );
                PerftestRunner.this.running = false;
                PerftestRunner.this.needsReset = true;
                stopTime = System.nanoTime();
            }
        } ).start();
    }


    private boolean threadsRunning()
    {
        boolean anyAlive = false;

        try {
            Thread.sleep( sleepToStop.get() );
        }
        catch ( InterruptedException e ) {
            log.error( "Thread was interrupted.", e );
        }

        for ( Thread t : threads )
        {
            anyAlive |= t.isAlive();
        }

        return anyAlive;
    }


    public void stop() {
        synchronized ( lock ) {
            stopSignal = true;
            boolean anyAlive = false;

            do {
                anyAlive |= threadsRunning();
            } while( anyAlive );

            running = false;
            stopTime = System.nanoTime();
            needsReset = true;
        }
    }
}
