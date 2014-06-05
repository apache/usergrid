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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.jukito.JukitoRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityDeleteEvent;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.core.entity.EntityVersion;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.query.CandidateResult;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.netflix.astyanax.util.TimeUUIDUtils;

import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith( JukitoRunner.class )
public class CpEntityIndexDeleteListenerTest {
    EntityIndex entityIndex;
    CpEntityIndexDeleteListener esEntityIndexDeleteListener;
    SerializationFig serializationFig;
    private EntityIndexFactory eif;

    @Before
    public void setup(){
        this.entityIndex =  mock(EntityIndex.class);
        serializationFig = mock(SerializationFig.class);
        this.eif = mock(EntityIndexFactory.class);

        this.esEntityIndexDeleteListener = new CpEntityIndexDeleteListener(eif, serializationFig);
    }

    @Test
    public void delete(){
        CollectionScope scope = mock(CollectionScope.class);
        UUID uuid = TimeUUIDUtils.getTimeUUID(10000L);
        Id entityId = new SimpleId(uuid,"test");
        CandidateResult entity = mock(CandidateResult.class);
        when(entity.getVersion()).thenReturn(uuid);
        when(entity.getId()).thenReturn(entityId);
        when(scope.getOwner()).thenReturn(entityId);
        when(scope.getName()).thenReturn("test");
        when(scope.getApplication()).thenReturn(entityId);
        when(eif.createEntityIndex(any(IndexScope.class))).thenReturn(entityIndex);

        CandidateResults results = mock(CandidateResults.class);
        List<CandidateResult> resultsList  = new ArrayList<>();
        resultsList.add(entity);
        Iterator<CandidateResult> entities = resultsList.iterator();

        when(results.iterator()).thenReturn(entities);
        when(serializationFig.getBufferSize()).thenReturn(10);
        when(serializationFig.getHistorySize()).thenReturn(20);
        when(entityIndex.getEntityVersions(entityId)).thenReturn(results);
        MvccEntity mvccEntity = new MvccEntityImpl(entityId,uuid, MvccEntity.Status.COMPLETE,mock(Entity.class));


        MvccEntityDeleteEvent event = new MvccEntityDeleteEvent(scope,uuid,mvccEntity);
        Observable<EntityVersion> o = esEntityIndexDeleteListener.receive(event);
        EntityVersion testEntity = o.toBlockingObservable().last();
        assertEquals(testEntity.getId(),mvccEntity.getId());
        verify(entityIndex).deindex(entity.getId(),entity.getVersion());
    }
}
