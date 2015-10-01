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
package org.apache.usergrid.persistence.collection.mvcc.stage;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.EntityUtils;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/** Helper class for generating MvccEntities and Entities
 * @author tnine */
public class TestEntityGenerator {


    /**
     * Return an MvccEntityMock with valid inputs from the supplied entity
     *
     * @param entity
     * @return
     */
    public static MvccEntity fromEntity(Entity entity) {

        final MvccEntity mvccEntity = mock(MvccEntity.class);
        when(mvccEntity.getId()).thenReturn(entity.getId());
        when(mvccEntity.getVersion()).thenReturn(entity.getVersion());
        when(mvccEntity.getEntity()).thenReturn(Optional.of(entity));

        return mvccEntity;
    }

    /**
     * Return an MvccEntityMock with valid inputs from the supplied entity
     *
     * @param entity
     * @return
     */
    public static MvccEntity fromEntityStatus(Entity entity, MvccEntity.Status status) {

        final MvccEntity mvccEntity = mock(MvccEntity.class);
        when(mvccEntity.getId()).thenReturn(entity.getId());
        when(mvccEntity.getVersion()).thenReturn(entity.getVersion());
        when(mvccEntity.getEntity()).thenReturn(Optional.of(entity));
        when(mvccEntity.getStatus()).thenReturn(status);

        return mvccEntity;
    }


    /**
     * Generate a valid entity
     *
     * @return
     */
    public static Entity generateEntity() {
        final Entity entity = new Entity(generateId());
        final UUID version = UUIDGenerator.newTimeUUID();

        EntityUtils.setVersion(entity, version);

        return entity;
    }

    /**
     * Generate a valid entity
     *
     * @return
     */
    public static Entity generateEntity(final Id id, final UUID version) {
        final Entity entity = new Entity(id);

        EntityUtils.setVersion(entity, version);

        return entity;
    }


    /**
     * Generate an id with type "test" and a new time uuid
     *
     * @return
     */
    public static Id generateId() {
        return new SimpleId(UUIDGenerator.newTimeUUID(), "test");
    }
}
