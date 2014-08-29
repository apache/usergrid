package org.apache.usergrid.persistence.model.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import com.fasterxml.uuid.UUIDComparator;
import com.google.common.collect.Sets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/** @author tnine */
public class UUIDGeneratorTest {


    @Test
    public void testOrderingConcurrency() throws InterruptedException, ExecutionException {


        //either all processor count, or 2 threads
        final int numberThreads = Math.max( Runtime.getRuntime().availableProcessors(), 2 );

        /**
         * 10k  uuids per thread
         */
        final int count = 10000;

        ExecutorService executor = Executors.newFixedThreadPool( numberThreads );

        List<UUIDConsumer> consumers = new ArrayList<UUIDConsumer>( numberThreads );

        for ( int i = 0; i < numberThreads; i++ ) {
            consumers.add( new UUIDConsumer( count ) );
        }

        List<Future<Void>> futures = executor.invokeAll( consumers );

        //wait for them all to finish
        for(Future<Void> future: futures){
            future.get();
        }

        //now validate each one is in order and does not intersect with any other
        for(int i = 0; i < numberThreads; i ++){

            UUIDConsumer current = consumers.get( i );

            current.validateOrder();

            for(int j = i+1; j < numberThreads; j++){
                current.noIntersect( consumers.get( j ) );
            }
        }

    }


    private static class UUIDConsumer implements Callable<Void> {

        private final int toGenerate;
        private final List<UUID> results;


        private UUIDConsumer( final int toGenerate ) {
            this.toGenerate = toGenerate;
            this.results = new ArrayList<UUID>( toGenerate );
        }


        /**
         * Validate that each UUID is greater than than it's previous entry when comparing them
         */
        public void validateOrder() {

            for(int i = 0; i < toGenerate -1; i ++){
                int comparison = UUIDComparator.staticCompare( results.get( i ), results.get( i+1 ) );
                assertTrue(comparison < 0);
            }
        }


        /**
         * Validate the other UUID consumer does not have any intersection with this consumer
         * @param other
         */
        public void noIntersect(UUIDConsumer other){

            Set<UUID> firstSet = new HashSet<UUID>(results);
            Set<UUID> otherSet = new HashSet<UUID>(other.results);

            Set<UUID> intersection = Sets.intersection(firstSet, otherSet);

            assertEquals("No UUID Generator should have a UUID that intersects with another UUID", 0, intersection.size());
        }


        @Override
        public Void call() throws Exception {
            for ( int i = 0; i < toGenerate; i++ ) {
                this.results.add( UUIDGenerator.newTimeUUID() );
            }

            return null;
        }
    }

}
