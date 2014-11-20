/*
* Licensed to the Apache Software Foundation (ASF) under one or more
*  contributor license agreements.  The ASF licenses this file to You
* under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.  For additional information regarding
* copyright in this work, please see the NOTICE file in the top level
* directory of this distribution.
*/
package org.apache.usergrid.corepersistence;


import java.util.ArrayList;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityDeleteEvent;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.entity.EntityVersion;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;

import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class CpEntityDeleteListenerTest {


    protected MvccEntitySerializationStrategy mvccEntitySerializationStrategy;

    protected CpEntityDeleteListener listener;

    protected SerializationFig serializationFig;

    protected Keyspace keyspace;

    @Before
    public void setup() {
        serializationFig = mock(SerializationFig.class);
        keyspace = mock(Keyspace.class);
        mvccEntitySerializationStrategy = mock(MvccEntitySerializationStrategy.class);

        listener = new CpEntityDeleteListener(mvccEntitySerializationStrategy, keyspace, serializationFig);
    }

    @Test
    public void receive() {
        CollectionScope scope = mock(CollectionScope.class);
        UUID id = UUID.randomUUID();
        MvccEntity entity = mock(MvccEntity.class);
        Id entityId = new SimpleId(id, "test");
        when(entity.getId()).thenReturn(entityId);
        when(entity.getVersion()).thenReturn(id);
        MvccEntityDeleteEvent entityEvent = new MvccEntityDeleteEvent(scope, id, entity);
        MutationBatch batch = mock(MutationBatch.class);
        when(keyspace.prepareMutationBatch()).thenReturn(batch);
        when(serializationFig.getBufferSize()).thenReturn(10);
        when(serializationFig.getHistorySize()).thenReturn(20);

        ArrayList<MvccEntity> entityList = new ArrayList<>();
        entityList.add(entity);
        when(mvccEntitySerializationStrategy.delete(scope, entityId, id)).thenReturn(batch);
        when(mvccEntitySerializationStrategy.loadAscendingHistory( scope, entityId, id,
                serializationFig.getHistorySize() )).thenReturn(entityList.iterator());

        Observable<EntityVersion> observable = listener.receive(entityEvent);
        EntityVersion entityEventReturned = observable.toBlocking().last();
        assertEquals(entity.getVersion(), entityEventReturned.getVersion());
    }

}
