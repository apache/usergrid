/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.runner.drivers;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.runner.Result;
import org.apache.usergrid.chop.api.Constants;
import org.apache.usergrid.chop.api.IterationChop;
import org.apache.usergrid.chop.api.TimeChop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;


/** An asynchronous results log implementation. */
public class ResultsLog implements IResultsLog, Runnable {
    private static final Logger LOG = LoggerFactory.getLogger( ResultsLog.class );

    private final AtomicLong resultCount = new AtomicLong();
    private final AtomicBoolean isOpen = new AtomicBoolean( false );
    private LinkedBlockingDeque<Result> buffer = new LinkedBlockingDeque<Result>();
    private Thread thread;
    private JsonGenerator jgen;

    private Tracker tracker;
    private DynamicStringProperty resultsFile;
    private DynamicLongProperty waitTime;
    private DynamicBooleanProperty prettyPrint;


    public ResultsLog( Tracker tracker ) throws IOException {
        this.tracker = tracker;
        File defaultFile = File.createTempFile( tracker.getTestClass().getCanonicalName(), "log" );
        LOG.info( "Default results log file path = {}", defaultFile.getAbsolutePath() );


        resultsFile = DynamicPropertyFactory.getInstance().
                getStringProperty( RESULTS_FILE_KEY, defaultFile.getAbsolutePath() );
        LOG.info( "Actual results log file path = {}", resultsFile.get() );
        waitTime = DynamicPropertyFactory.getInstance().getLongProperty( WAIT_TIME_KEY, 200 );
        prettyPrint = DynamicPropertyFactory.getInstance().getBooleanProperty( Constants.PRETTY_PRINT_RESULTS, true );
    }


    @Override
    public void open() throws IOException {
        synchronized ( isOpen ) {
            if ( isOpen.compareAndSet( false, true ) ) {
                resultCount.set( 0 );

                // write the json header for recording the chop results
                JsonFactory factory = new JsonFactory();
                jgen = factory.createGenerator(  new File( resultsFile.get() ), JsonEncoding.UTF8 );

                if ( prettyPrint.get() ) {
                    jgen.useDefaultPrettyPrinter();
                }

                jgen.setCodec( new ObjectMapper() );

                setupJsonStream();

                thread = new Thread( this, "ResultLog Writer" );
                thread.start();
            }
        }
    }


    /**
     * Sets up the JSON preamble to start streaming results into the entity. This must be
     * protected via isOpen. This is an unsafe call, make sure you know how it is used.
     */
    private void setupJsonStream() throws IOException {
        Class<?> testClass = tracker.getTestClass();
        jgen.writeStartObject();
        jgen.writeStringField( "testClass", testClass.getCanonicalName() );
        jgen.writeNumberField( "startTime", tracker.getStartTime() );

        if ( testClass.isAnnotationPresent( TimeChop.class ) ) {
            jgen.writeStringField( "chopType", "TimeChop" );
            jgen.writeObjectField( "chopParameters", testClass.getAnnotation( TimeChop.class ) );
        }
        else if ( tracker.getTestClass().isAnnotationPresent( IterationChop.class ) ) {
            jgen.writeStringField( "chopType", "IterationChop" );
            jgen.writeObjectField( "chopParameters", testClass.getAnnotation( IterationChop.class ) );
        }
        else {
            throw new IllegalStateException( "Supplied testClass " + testClass.getCanonicalName() +
                    "has no chop annotation." );
        }

        jgen.writeFieldName( "runResults" );
        jgen.writeStartArray();
        jgen.flush();
    }


    /**
     * Cleans up the JSON preamble to close streaming results into the entity. This must
     * be protected via isOpen. This is an unsafe call, make sure you know how it is used.
     */
    private void cleanupJsonStream() throws IOException {
        jgen.writeEndArray(); // end the array of results

        // write summary information after the
        jgen.writeNumberField( "stopTime", tracker.getStopTime() );
        jgen.writeNumberField( "totalRunTime", tracker.getTotalRunTime() );
        jgen.writeNumberField( "actualIterations", tracker.getActualIterations() );
        jgen.writeNumberField( "actualTime", tracker.getActualTime() );
        jgen.writeNumberField( "failures", tracker.getFailures() );
        jgen.writeNumberField( "ignores", tracker.getIgnores() );
        jgen.writeNumberField( "totalTestsRun", tracker.getTotalTestsRun() );
        jgen.writeNumberField( "maxTime", tracker.getMaxTime() );
        jgen.writeNumberField( "minTime", tracker.getMinTime() );
        jgen.writeNumberField( "meanTime", tracker.getMeanTime() );
        jgen.writeEndObject();
        jgen.flush();
    }


    @Override
    public void close() throws IOException {
        if ( isOpen.compareAndSet( true, false ) ) {

            // Forces us to wait until the writer thread dies
            synchronized ( isOpen ) {
                cleanupJsonStream();
                jgen.flush();
                jgen.close();
                thread = null;
            }
        }
    }


    @Override
    public void truncate() throws IOException {
        if ( isOpen.get() ) {
            throw new IOException( "Cannot truncate while log is open for writing. Close the log then truncate." );
        }

        // Synchronize on isOpen to prevent re-opening while truncating (rare)
        synchronized ( isOpen ) {
            File results = new File( resultsFile.get() );
            FileChannel channel = new FileOutputStream( results, true ).getChannel();
            channel.truncate( 0 );
            channel.close();
            resultCount.set( 0 );
        }
    }


    @Override
    public void write( Result result ) {
        Preconditions.checkState( isOpen.get(), "The result log is not open for writing!" );

        try {
            buffer.putFirst( result );
        }
        catch ( InterruptedException e ) {
            LOG.error( "Was interrupted on write.", e );
        }
    }


    @Override
    public long getResultCount() {
        return resultCount.get();
    }


    @Override
    public String getPath() {
        return resultsFile.get();
    }


    @Override
    public void run() {
        synchronized ( isOpen ) {
            // Keep writing after closed until buffer is flushed (empty)
            while ( isOpen.get() || !buffer.isEmpty() ) {
                try {
                    Result result = buffer.pollLast( waitTime.get(), TimeUnit.MILLISECONDS );

                    if ( result != null ) {
                        resultCount.incrementAndGet();
                        jgen.writeObject( result );
                    }
                }
                catch ( InterruptedException e ) {
                    LOG.error( "ResultLog thread interrupted.", e );
                }
                catch ( JsonProcessingException e ) {
                    LOG.error( "Failed to generate the JSON for a result.", e );
                }
                catch ( IOException e ) {
                    LOG.error( "Failed to write JSON to output stream for a result", e );
                }
            }

            isOpen.notifyAll();
        }
    }
}
