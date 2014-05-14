package org.apache.usergrid.persistence.index.impl;

import com.netflix.astyanax.util.TimeUUIDUtils;
import com.yammer.metrics.core.Clock;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityEvent;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.index.query.Results;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JukitoRunner.class)
@UseModules({ TestIndexModule.class })
public class EsEntityIndexDeleteListenerTest {

    EntityIndex entityIndex;
    EsEntityIndexDeleteListener esEntityIndexDeleteListener;
    SerializationFig serializationFig;
    private EntityIndexFactory eif;

    @Before
    public void setup(){
        this.entityIndex =  mock(EntityIndex.class);
        serializationFig = mock(SerializationFig.class);
        this.eif = mock(EntityIndexFactory.class);

        AsyncProcessor<MvccEntityEvent<MvccEntity>> entityDelete = mock(AsyncProcessor.class);
        this.esEntityIndexDeleteListener = new EsEntityIndexDeleteListener(eif,entityDelete,serializationFig);
    }

    @Test
    public void delete(){
        CollectionScope scope = mock(CollectionScope.class);
        UUID uuid = TimeUUIDUtils.getTimeUUID(10000L);
        Id entityId = new SimpleId(uuid,"test");
        Entity entity = mock(Entity.class);
        when(entity.getVersion()).thenReturn(uuid);
        when(entity.getId()).thenReturn(entityId);
        when(eif.createEntityIndex(null,null)).thenReturn(entityIndex);


        Results results = mock(Results.class);
        List<Entity> entities = new ArrayList<>();
        entities.add(entity);
        when(serializationFig.getBufferSize()).thenReturn(10);
        when(serializationFig.getHistorySize()).thenReturn(20);
        when(results.getEntities()).thenReturn(entities);
        when(entityIndex.getEntityVersions(entityId, scope)).thenReturn(results);
        MvccEntity mvccEntity = new MvccEntityImpl(entityId,uuid, MvccEntity.Status.COMPLETE,entity);
        MvccEntityEvent<MvccEntity> event = new MvccEntityEvent<MvccEntity>(scope,uuid,mvccEntity);
        Observable<MvccEntity> o = esEntityIndexDeleteListener.receive(event);
        MvccEntity testEntity = o.toBlockingObservable().last();
        assertEquals(testEntity.getId(),mvccEntity.getId());
        verify(entityIndex).deindex(scope,entity);
    }
}
