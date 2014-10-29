package org.apache.usergrid.persistence.collection.impl;


import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityVersionCreatedFactory;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.event.EntityDeleted;
import org.apache.usergrid.persistence.collection.event.EntityVersionCreated;
import org.apache.usergrid.persistence.collection.event.EntityVersionDeleted;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.serialization.UniqueValue;
import org.apache.usergrid.persistence.collection.serialization.UniqueValueSerializationStrategy;
import org.apache.usergrid.persistence.collection.serialization.impl.UniqueValueImpl;
import org.apache.usergrid.persistence.core.rx.ObservableIterator;
import org.apache.usergrid.persistence.core.task.Task;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.field.Field;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Created by ApigeeCorporation on 10/24/14.
 */
public class EntityVersionCreatedTask implements Task<Void> {
    private static final Logger LOG = LoggerFactory.getLogger( EntityVersionCleanupTask.class );


    private final Set<EntityVersionCreated> listeners;

    private final MvccLogEntrySerializationStrategy logEntrySerializationStrategy;
    private final MvccEntitySerializationStrategy entitySerializationStrategy;
    private UniqueValueSerializationStrategy uniqueValueSerializationStrategy;
    private final Keyspace keyspace;

    private final SerializationFig serializationFig;

    private final CollectionScope collectionScope;
    private final Entity entity;

    private EntityVersionCreatedFactory entityVersionCreatedFactory;



    @Inject
    public EntityVersionCreatedTask( EntityVersionCreatedFactory entityVersionCreatedFactory,
                                     final SerializationFig serializationFig,
                                     final MvccLogEntrySerializationStrategy logEntrySerializationStrategy,
                                     final MvccEntitySerializationStrategy entitySerializationStrategy,
                                     final UniqueValueSerializationStrategy uniqueValueSerializationStrategy,
                                     final Keyspace keyspace,
                                     @Assisted final CollectionScope collectionScope,
                                     final Set<EntityVersionCreated> listeners,
                                     @Assisted final Entity entity ) {

        this.entityVersionCreatedFactory = entityVersionCreatedFactory;
        this.serializationFig = serializationFig;
        this.logEntrySerializationStrategy = logEntrySerializationStrategy;
        this.entitySerializationStrategy = entitySerializationStrategy;
        this.uniqueValueSerializationStrategy = uniqueValueSerializationStrategy;
        this.keyspace = keyspace;
        this.listeners = listeners;
        this.collectionScope = collectionScope;
        this.entity = entity;

    }


    @Override
    public void exceptionThrown( final Throwable throwable ) {
        LOG.error( "Unable to run update task for collection {} with entity {} and version {}",
                new Object[] { collectionScope, entity}, throwable );
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

        fireEvents();
        return null;
    }

    private void fireEvents() {
        final int listenerSize = listeners.size();

        if ( listenerSize == 0 ) {
            return;
        }

        if ( listenerSize == 1 ) {
            listeners.iterator().next().versionCreated( collectionScope, entity );
            return;
        }

        LOG.debug( "Started firing {} listeners", listenerSize );
        //if we have more than 1, run them on the rx scheduler for a max of 8 operations at a time
        Observable.from(listeners)
                  .parallel( new Func1<Observable<EntityVersionCreated
                          >, Observable<EntityVersionCreated>>() {

                      @Override
                      public Observable<EntityVersionCreated> call(
                              final Observable<EntityVersionCreated> entityVersionCreatedObservable ) {

                          return entityVersionCreatedObservable.doOnNext( new Action1<EntityVersionCreated>() {
                              @Override
                              public void call( final EntityVersionCreated listener ) {
                                  listener.versionCreated(collectionScope,entity);
                              }
                          } );
                      }
                  }, Schedulers.io() ).toBlocking().last();

        LOG.debug( "Finished firing {} listeners", listenerSize );
    }
}
