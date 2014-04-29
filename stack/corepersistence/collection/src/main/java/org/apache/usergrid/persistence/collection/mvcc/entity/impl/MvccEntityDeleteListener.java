package org.apache.usergrid.persistence.collection.mvcc.entity.impl;

import com.netflix.astyanax.Keyspace;
import org.apache.usergrid.persistence.collection.guice.MvccEntityDelete;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.consistency.MessageListener;
import rx.Observable;
import rx.functions.Func1;

/**
 * Created by ApigeeCorporation on 4/28/14.
 */
public class MvccEntityDeleteListener implements MessageListener<MvccEntityEvent<MvccEntity>,MvccEntityEvent<MvccEntity>> {

    private final MvccEntitySerializationStrategy entityMetadataSerialization;
    private final AsyncProcessor<MvccEntityEvent<MvccEntity>> entityDelete;

    public MvccEntityDeleteListener(final MvccEntitySerializationStrategy entityMetadataSerialization, final Keyspace keyspace,
                                    @MvccEntityDelete final AsyncProcessor<MvccEntityEvent<MvccEntity>> entityDelete){
        this.entityMetadataSerialization = entityMetadataSerialization;
        this.entityDelete = entityDelete;
        entityDelete.addListener( this );
    }

    @Override
    public Observable<MvccEntityEvent<MvccEntity>> receive(final MvccEntityEvent<MvccEntity> entityEvent) {
        final MvccEntity entity = entityEvent.getData();
        return Observable.from(entity).map( new Func1<MvccEntity, MvccEntityEvent<MvccEntity>>() {
            @Override
            public MvccEntityEvent<MvccEntity> call(MvccEntity mvccEntity) {
                entityMetadataSerialization.delete(entityEvent.getCollectionScope(),entity.getId(),entity.getVersion());
                return entityEvent;
            }
        });
    }
}
