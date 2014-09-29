package org.apache.usergrid.persistence.collection.impl;


import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Cleans up previous versions from the specified version. Note that this means the version passed in the io event is
 * retained, the range is exclusive.
 */
public class EntityVersionCleanupTask implements Task<Void> {

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
    public Void rejected() {
        //Our task was rejected meaning our queue was full.  We need this operation to run,
        // so we'll run it in our current thread

        try {
            call();
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Exception thrown in call task", e );
        }

        return null;
    }


    @Override
    public Void call() throws Exception {


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

        final int listenerSize = listeners.size();

        if ( listenerSize == 0 ) {
            return;
        }

        if ( listenerSize == 1 ) {
            listeners.get( 0 ).versionDeleted( scope, entityId, version );
            return;
        }

        LOG.debug( "Started firing {} listeners", listenerSize );

        //if we have more than 1, run them on the rx scheduler for a max of 8 operations at a time
        Observable.from( listeners )
                  .parallel( new Func1<Observable<EntityVersionDeleted>, Observable<EntityVersionDeleted>>() {

                      @Override
                      public Observable<EntityVersionDeleted> call(
                              final Observable<EntityVersionDeleted> entityVersionDeletedObservable ) {

                          return entityVersionDeletedObservable.doOnNext( new Action1<EntityVersionDeleted>() {
                              @Override
                              public void call( final EntityVersionDeleted listener ) {
                                  listener.versionDeleted( scope, entityId, version );
                              }
                          } );
                      }
                  }, Schedulers.io() ).toBlocking().last();

        LOG.debug( "Finished firing {} listeners", listenerSize );
    }


    private static interface ListenerRunner {

        /**
         * Run the listeners
         */
        public void runListeners();
    }
}



