package org.apache.usergrid.persistence.index.impl;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityEvent;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(JukitoRunner.class)
@UseModules({ TestIndexModule.class })
public class EsEntityIndexDeleteListenerTest {

    EntityIndex entityIndex;
    EsEntityIndexDeleteListener esEntityIndexDeleteListener;

    @Before
    public void setup(){
        this.entityIndex =  mock(EntityIndex.class);
        this.esEntityIndexDeleteListener = new EsEntityIndexDeleteListener(entityIndex);
    }

    @Test
    public void delete(){
        CollectionScope scope = mock(CollectionScope.class);
        UUID uuid = UUID.randomUUID();
        Id entityId = new SimpleId(uuid,"test");
        Entity entity = new Entity(entityId);
        MvccEntity mvccEntity = new MvccEntityImpl(entityId,uuid, MvccEntity.Status.COMPLETE,entity);
        MvccEntityEvent<MvccEntity> event = new MvccEntityEvent<MvccEntity>(scope,uuid,mvccEntity);
        Observable<MvccEntity> o = esEntityIndexDeleteListener.receive(event);
        MvccEntity testEntity = o.toBlockingObservable().last();
        assertEquals(testEntity,mvccEntity);
        verify(entityIndex).deindex(scope,entity);
    }
}
