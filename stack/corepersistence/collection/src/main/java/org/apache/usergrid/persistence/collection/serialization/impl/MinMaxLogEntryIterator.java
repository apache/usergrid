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
public class MinMaxLogEntryIterator implements Iterator<MvccLogEntry> {


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
     * @param pageSize The fetch size to get when querying the serialization strategy
     */
    public MinMaxLogEntryIterator( final MvccLogEntrySerializationStrategy logEntrySerializationStrategy,
                                   final ApplicationScope scope, final Id entityId, final int pageSize ) {

        Preconditions.checkArgument( pageSize > 0, "pageSize must be > 0" );

        this.logEntrySerializationStrategy = logEntrySerializationStrategy;
        this.scope = scope;
        this.entityId = entityId;
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

        final int requestedSize;

        if ( nextStart != null ) {
            requestedSize = pageSize + 1;
        }
        else {
            requestedSize = pageSize;
        }

        //loop through even entry that's < this one and remove it
        List<MvccLogEntry> results = logEntrySerializationStrategy.loadReversed( scope, entityId, nextStart, requestedSize );

        //we always remove the first version if it's equal since it's returned
        if ( nextStart != null && results.size() > 0 && results.get( 0 ).getVersion().equals( nextStart ) ) {
            results.remove( 0 );
        }



        //we have results, set our next start.  If we miss our start version (due to deletion) and we request a +1, we want to ensure we set our next, hence the >=
        if ( results.size() >= pageSize ) {
            nextStart = results.get( results.size() - 1 ).getVersion();
        }
        //nothing left to do
        else {
            nextStart = null;
        }




        elementItr = results.iterator();
    }
}
