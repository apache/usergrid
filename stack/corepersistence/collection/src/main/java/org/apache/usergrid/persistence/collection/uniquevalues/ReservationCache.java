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
package org.apache.usergrid.persistence.collection.uniquevalues;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import org.apache.usergrid.persistence.actorsystem.ClientActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;


public class ReservationCache {
    private static final Logger logger = LoggerFactory.getLogger( ClientActor.class );

    Cache<String, UniqueValueActor.Reservation> cache;
    long ttl;

    // use hokey old-style singleton because its not that easy to get Guice into an actor
    private static ReservationCache instance = null;

    ReservationCache( long ttl ) {
        this.ttl = ttl;
        cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .concurrencyLevel( 300 )
            .expireAfterWrite(ttl, TimeUnit.SECONDS)
            .recordStats()
            .build();
    }

    public static void init( long ttl ) {
        instance = new ReservationCache( ttl );
    }

    public static ReservationCache getInstance() {
        if ( instance == null ) {
            throw new IllegalStateException( "ReservationCache not initialized yet" );
        }
        return instance;
    }

    public UniqueValueActor.Reservation get( String rowKey ) {
        if ( ttl == 0 ) { return null; }
        UniqueValueActor.Reservation res = cache.getIfPresent( rowKey );
        return res;
    }

    public void cacheReservation( UniqueValueActor.Reservation reservation ) {
        if ( ttl == 0 ) { return; }
        cache.put( reservation.getConsistentHashKey(), reservation );
    }

    public void cancelReservation( UniqueValueActor.Cancellation cancellation ) {
        if ( ttl == 0 ) { return; }
        cache.invalidate( cancellation.getConsistentHashKey() );
    }

    public void cancelReservation( UniqueValueActor.Response response ) {
        if ( ttl == 0 ) { return; }
        cache.invalidate( response.getConsistentHashKey() );
    }

    public CacheStats getStats() {
        return cache.stats();
    }

    public long getSize() {
        return cache.size();
    }
}
