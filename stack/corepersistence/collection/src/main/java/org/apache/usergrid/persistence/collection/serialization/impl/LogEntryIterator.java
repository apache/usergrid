package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


/**
 * Iterator that will iterate all versions of the entity from the log from < the specified maxVersion
 */
public class LogEntryIterator implements Iterator<MvccLogEntry> {


    private final MvccLogEntrySerializationStrategy logEntrySerializationStrategy;
    private final ApplicationScope scope;
    private final Id entityId;
    private final int pageSize;


    private Iterator<MvccLogEntry> elementItr;
    private UUID nextStart;


    /**
     * @param logEntrySerializationStrategy The serialization strategy to get the log entries
     * @param scope The scope of the entity
     * @param entityId The id of the entity
     * @param maxVersion The max version of the entity.  Iterator will iterate from max to min starting with the version
     * < max
     * @param pageSize The fetch size to get when querying the serialization strategy
     */
    public LogEntryIterator( final MvccLogEntrySerializationStrategy logEntrySerializationStrategy,
                             final ApplicationScope scope, final Id entityId, final UUID maxVersion,
                             final int pageSize ) {

        Preconditions.checkArgument( pageSize > 0, "pageSize must be > 0" );

        this.logEntrySerializationStrategy = logEntrySerializationStrategy;
        this.scope = scope;
        this.entityId = entityId;
        this.nextStart = maxVersion;
        this.pageSize = pageSize;
    }


    @Override
    public boolean hasNext() {
        if ( elementItr == null || !elementItr.hasNext() && nextStart != null ) {
            try {
                advance();
            }
            catch ( ConnectionException e ) {
                throw new RuntimeException( "Unable to query cassandra", e );
            }
        }

        return elementItr.hasNext();
    }


    @Override
    public MvccLogEntry next() {
        if ( !hasNext() ) {
            throw new NoSuchElementException( "No more elements exist" );
        }

        return elementItr.next();
    }


    @Override
    public void remove() {
        throw new UnsupportedOperationException( "Remove is unsupported" );
    }


    /**
     * Advance our iterator
     */
    public void advance() throws ConnectionException {

        final int requestedSize = pageSize + 1;

        //loop through even entry that's < this one and remove it
        List<MvccLogEntry> results = logEntrySerializationStrategy.load( scope, entityId, nextStart, requestedSize );

        //we always remove the first version if it's equal since it's returned
        if ( results.size() > 0 && results.get( 0 ).getVersion().equals( nextStart ) ) {
            results.remove( 0 );
        }


        //we have results, set our next start
        if ( results.size() == pageSize ) {
            nextStart = results.get( results.size() - 1 ).getVersion();
        }
        //nothing left to do
        else {
            nextStart = null;
        }

        elementItr = results.iterator();
    }
}
