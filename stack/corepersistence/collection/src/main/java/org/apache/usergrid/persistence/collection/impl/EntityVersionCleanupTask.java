package org.apache.usergrid.persistence.collection.impl;


import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RecursiveTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.event.EntityVersionDeleted;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccLogEntry;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.impl.LogEntryIterator;
import org.apache.usergrid.persistence.core.task.Task;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * Cleans up previous versions from the specified version. Note that this means the version passed in the io event is
 * retained, the range is exclusive.
 */
public class EntityVersionCleanupTask extends Task<Void> {

    private static final Logger LOG = LoggerFactory.getLogger( EntityVersionCleanupTask.class );


    private final List<EntityVersionDeleted> listeners;

    private final MvccLogEntrySerializationStrategy logEntrySerializationStrategy;
    private final MvccEntitySerializationStrategy entitySerializationStrategy;

    private final SerializationFig serializationFig;

    private final CollectionScope scope;
    private final Id entityId;
    private final UUID version;


    public EntityVersionCleanupTask( final SerializationFig serializationFig,
                                     final MvccLogEntrySerializationStrategy logEntrySerializationStrategy,
                                     final MvccEntitySerializationStrategy entitySerializationStrategy,
                                     final List<EntityVersionDeleted> listeners, final CollectionScope scope,
                                     final Id entityId, final UUID version ) {

        this.serializationFig = serializationFig;
        this.logEntrySerializationStrategy = logEntrySerializationStrategy;
        this.entitySerializationStrategy = entitySerializationStrategy;
        this.listeners = listeners;
        this.scope = scope;
        this.entityId = entityId;
        this.version = version;
    }


    @Override
    public void exceptionThrown( final Throwable throwable ) {
        LOG.error( "Unable to run update task for collection {} with entity {} and version {}",
                new Object[] { scope, entityId, version }, throwable );
    }


    @Override
    public void rejected() {
        //Our task was rejected meaning our queue was full.  We need this operation to run,
        // so we'll run it in our current thread

        try {
            executeTask();
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Exception thrown in call task", e );
        }
    }


    @Override
    public Void executeTask() throws Exception {


        final UUID maxVersion = version;


        LogEntryIterator logEntryIterator =
                new LogEntryIterator( logEntrySerializationStrategy, scope, entityId, maxVersion,
                        serializationFig.getHistorySize() );


        //for every entry, we want to clean it up with listeners

        while ( logEntryIterator.hasNext() ) {

            final MvccLogEntry logEntry = logEntryIterator.next();


            final UUID version = logEntry.getVersion();


            fireEvents();

            //we do multiple invocations on purpose.  Our log is our source of versions, only delete from it
            //after every successful invocation of listeners and entity removal
            entitySerializationStrategy.delete( scope, entityId, version ).execute();

            logEntrySerializationStrategy.delete( scope, entityId, version ).execute();
        }


        return null;
    }


    private void fireEvents() throws ExecutionException, InterruptedException {

        if ( listeners.size() == 0 ) {
            return;
        }

        //stack to track forked tasks
        final Stack<RecursiveTask<Void>> tasks = new Stack<>();


        //we don't want to fork the final listener, we'll run that in our current thread
        final int forkedTaskSize = listeners.size() - 1;


        //execute all the listeners
        for ( int i = 0; i < forkedTaskSize; i++ ) {

            final EntityVersionDeleted listener = listeners.get( i );

            final RecursiveTask<Void> task = createTask( listener );

            task.fork();

            tasks.push( task );
        }


        final RecursiveTask<Void> lastTask = createTask( listeners.get( forkedTaskSize ) );

        lastTask.invoke();


        //wait for them to complete
        while ( !tasks.isEmpty() ) {
            tasks.pop().get();
        }
    }


    /**
     * Return the new task to execute
     */
    private RecursiveTask<Void> createTask( final EntityVersionDeleted listener ) {
        return new RecursiveTask<Void>() {
            @Override
            protected Void compute() {
                listener.versionDeleted( scope, entityId, version );
                return null;
            }
        };
    }
}



