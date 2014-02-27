/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.cassandra;


import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.internal.builders.AllDefaultPossibilitiesBuilder;
import org.junit.runner.Runner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.RunnerScheduler;


/** @author Mathieu Carbou (mathieu.carbou@gmail.com) */
public final class ConcurrentSuite extends Suite {
    public ConcurrentSuite( final Class<?> klass ) throws InitializationError {
        super( klass, new AllDefaultPossibilitiesBuilder( true ) {
            @Override
            public Runner runnerForClass( Class<?> testClass ) throws Throwable {
                List<RunnerBuilder> builders = Arrays.asList( new RunnerBuilder() {
                    @Override
                    public Runner runnerForClass( Class<?> testClass ) throws Throwable {
                        Concurrent annotation = testClass.getAnnotation( Concurrent.class );
                        if ( annotation != null ) {
                            return new ConcurrentJunitRunner( testClass );
                        }
                        return null;
                    }
                }, ignoredBuilder(), annotatedBuilder(), suiteMethodBuilder(), junit3Builder(), junit4Builder() );
                for ( RunnerBuilder each : builders ) {
                    Runner runner = each.safeRunnerForClass( testClass );
                    if ( runner != null ) {
                        return runner;
                    }
                }
                return null;
            }
        } );
        setScheduler( new RunnerScheduler() {
            ExecutorService executorService = Executors.newFixedThreadPool(
                    klass.isAnnotationPresent( Concurrent.class ) ? klass.getAnnotation( Concurrent.class ).threads() :
                    ( int ) ( Runtime.getRuntime().availableProcessors() * 1.5 ),
                    new NamedThreadFactory( klass.getSimpleName() ) );
            CompletionService<Void> completionService = new ExecutorCompletionService<Void>( executorService );
            Queue<Future<Void>> tasks = new LinkedList<Future<Void>>();


            @Override
            public void schedule( Runnable childStatement ) {
                tasks.offer( completionService.submit( childStatement, null ) );
            }


            @Override
            public void finished() {
                try {
                    while ( !tasks.isEmpty() ) {
                        tasks.remove( completionService.take() );
                    }
                }
                catch ( InterruptedException e ) {
                    Thread.currentThread().interrupt();
                }
                finally {
                    while ( !tasks.isEmpty() ) {
                        tasks.poll().cancel( true );
                    }
                    executorService.shutdownNow();
                }
            }
        } );
    }


    static final class NamedThreadFactory implements ThreadFactory {
        static final AtomicInteger poolNumber = new AtomicInteger( 1 );
        final AtomicInteger threadNumber = new AtomicInteger( 1 );
        final ThreadGroup group;


        NamedThreadFactory( String poolName ) {
            group = new ThreadGroup( poolName + "-" + poolNumber.getAndIncrement() );
        }


        @Override
        public Thread newThread( Runnable r ) {
            return new Thread( group, r, group.getName() + "-thread-" + threadNumber.getAndIncrement(), 0 );
        }
    }
}
