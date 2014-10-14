package org.apache.usergrid.persistence.collection.impl;


import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.event.EntityVersionDeleted;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.impl.LogEntryIterator;
import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.task.Task;
import org.apache.usergrid.persistence.model.entity.Id;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

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
    private final Keyspace keyspace;

    private final SerializationFig serializationFig;

    private final CollectionScope scope;
    private final Id entityId;
    private final UUID version;


    public EntityVersionCleanupTask( final SerializationFig serializationFig,
                                     final MvccLogEntrySerializationStrategy logEntrySerializationStrategy,
                                     final MvccEntitySerializationStrategy entitySerializationStrategy,
                                     final Keyspace keyspace, final List<EntityVersionDeleted> listeners,
                                     final CollectionScope scope, final Id entityId, final UUID version ) {

        this.serializationFig = serializationFig;
        this.logEntrySerializationStrategy = logEntrySerializationStrategy;
        this.entitySerializationStrategy = entitySerializationStrategy;
        this.keyspace = keyspace;
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


        Observable<MvccLogEntry> versions = Observable.create( new ObservableIterator( "versionIterators" ) {
            @Override
            protected Iterator getIterator() {
                return new LogEntryIterator( logEntrySerializationStrategy, scope, entityId, maxVersion,
                        serializationFig.getBufferSize() );
            }
        } );


        //get the uuid from the version
        versions.map( new Func1<MvccLogEntry, UUID>() {
            @Override
            public UUID call( final MvccLogEntry mvccLogEntry ) {
                return mvccLogEntry.getVersion();
            }
        } )
                //buffer our versions
         .buffer( serializationFig.getBufferSize() )
         //for each buffer set, delete all of them
         .doOnNext( new Action1<List<UUID>>() {
            @Override
            public void call( final List<UUID> versions ) {

                //Fire all the listeners
                fireEvents( versions );

                MutationBatch entityBatch = keyspace.prepareMutationBatch();
                MutationBatch logBatch = keyspace.prepareMutationBatch();

                for ( UUID version : versions ) {
                    final MutationBatch entityDelete = entitySerializationStrategy.delete( scope, entityId, version );

                    entityBatch.mergeShallow( entityDelete );

                    final MutationBatch logDelete = logEntrySerializationStrategy.delete( scope, entityId, version );

                    logBatch.mergeShallow( logDelete );
                }


                try {
                    entityBatch.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( "Unable to delete entities in cleanup", e );
                }

                try {
                    logBatch.execute();
                }
                catch ( ConnectionException e ) {
                    throw new RuntimeException( "Unable to delete entities from the log", e );
                }
            }
        } ).count().toBlocking().last();

        return null;
    }


    private void fireEvents( final List<UUID> versions ) {

        final int listenerSize = listeners.size();

        if ( listenerSize == 0 ) {
            return;
        }

        if ( listenerSize == 1 ) {
            listeners.get( 0 ).versionDeleted( scope, entityId, versions );
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
                                  listener.versionDeleted( scope, entityId, versions );
                              }
                          } );
                      }
                  }, Schedulers.io() ).toBlocking().last();

        LOG.debug( "Finished firing {} listeners", listenerSize );
    }
}



