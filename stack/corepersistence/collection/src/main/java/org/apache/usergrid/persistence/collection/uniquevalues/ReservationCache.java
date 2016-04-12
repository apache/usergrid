package org.apache.usergrid.persistence.collection.uniquevalues;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;


// cannot be a Guice singleton, must be shared across injectors
// @com.google.inject.Singleton
public class ReservationCache {
    private static final Logger logger = LoggerFactory.getLogger( RequestActor.class );

    Cache<String, UniqueValueActor.Reservation> cache = CacheBuilder.newBuilder()
       .maximumSize(1000)
       .concurrencyLevel( 300 )
       .expireAfterWrite(30, TimeUnit.SECONDS)
       .recordStats()
       .build();

    private static ReservationCache instance = new ReservationCache();

    public static ReservationCache getInstance() {
        return instance;
    }

    private ReservationCache() {}

    public UniqueValueActor.Reservation get( String rowKey ) {
        UniqueValueActor.Reservation res = cache.getIfPresent( rowKey );
        return res;
    }

    public void cacheReservation( UniqueValueActor.Reservation reservation ) {
        cache.put( reservation.getRowKey(), reservation );
    }

    public void cancelReservation( UniqueValueActor.Cancellation cancellation ) {
        cache.invalidate( cancellation.getRowKey() );
    }

    public CacheStats getStats() {
        return cache.stats();
    }

    public long getSize() {
        return cache.size();
    }
}
