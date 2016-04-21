package org.apache.usergrid.persistence.collection.uniquevalues;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;


public class ReservationCache {
    private static final Logger logger = LoggerFactory.getLogger( RequestActor.class );

    Cache<String, UniqueValueActor.Reservation> cache;

    // use hokey old-style singleton because its not that easy to get Guice into an actor
    private static ReservationCache instance = null;

    ReservationCache( long ttl ) {
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
        UniqueValueActor.Reservation res = cache.getIfPresent( rowKey );
        return res;
    }

    public void cacheReservation( UniqueValueActor.Reservation reservation ) {
        cache.put( reservation.getConsistentHashKey(), reservation );
    }

    public void cancelReservation( UniqueValueActor.Cancellation cancellation ) {
        cache.invalidate( cancellation.getConsistentHashKey() );
    }

    public CacheStats getStats() {
        return cache.stats();
    }

    public long getSize() {
        return cache.size();
    }
}
