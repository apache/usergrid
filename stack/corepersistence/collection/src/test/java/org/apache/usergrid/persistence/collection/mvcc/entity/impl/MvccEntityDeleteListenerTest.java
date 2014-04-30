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

package org.apache.usergrid.persistence.collection.mvcc.entity.impl;

import com.google.inject.Inject;
import com.netflix.astyanax.MutationBatch;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;
import org.apache.usergrid.persistence.model.entity.Id;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith( JukitoRunner.class )
@UseModules( { TestCollectionModule.class } )
public class MvccEntityDeleteListenerTest {



    @Inject
    protected MvccEntitySerializationStrategy mvccEntitySerializationStrategy;

    protected AsyncProcessor<MvccEntityEvent<MvccEntity>> processor;

    protected MvccEntityDeleteListener listener;

    @Before
    public void setup(){
        processor = mock( AsyncProcessor.class );
        mvccEntitySerializationStrategy = mock(MvccEntitySerializationStrategy.class);
        listener = new MvccEntityDeleteListener(mvccEntitySerializationStrategy,processor );
    }

    @Test
    public void TestConstructor(){
        CollectionScope scope = mock(CollectionScope.class);
        UUID id = UUID.randomUUID();
        MvccEntity entity = mock(MvccEntity.class);
        Id entityId = new SimpleId(id,"test");
        when(entity.getId()).thenReturn(entityId);
        when(entity.getVersion()).thenReturn(id);
        MvccEntityEvent<MvccEntity> entityEvent = new MvccEntityEvent<MvccEntity>(scope,id,entity);
        MutationBatch batch = mock(MutationBatch.class);

        when(mvccEntitySerializationStrategy.delete(scope,entityId,id)).thenReturn(batch);

        Observable<MvccEntityEvent<MvccEntity>> observable = listener.receive(entityEvent);
        MvccEntityEvent<MvccEntity> entityEventReturned = observable.toBlockingObservable().last();
        assertEquals(entityEvent,entityEventReturned);
    }
}
