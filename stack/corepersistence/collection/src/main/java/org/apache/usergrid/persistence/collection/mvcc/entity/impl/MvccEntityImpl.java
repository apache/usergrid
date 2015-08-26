/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.collection.mvcc.entity.impl;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;


/**
 * @author tnine
 */
public class MvccEntityImpl implements MvccEntity {

    private final Id entityId;
    private final UUID version;
    private final Optional<Entity> entity;
    private final Status status;
    private long size;


    public MvccEntityImpl( final Id entityId, final UUID version, final Status status, final Entity entity ) {
        this(entityId, version, status, Optional.of(entity));
    }

    public MvccEntityImpl(
        final Id entityId, final UUID version, final Status status, final Optional<Entity> entity) {
        this(entityId,version,status,entity,0);
    }

    public MvccEntityImpl(
            final Id entityId, final UUID version, final Status status, final Optional<Entity> entity, final long size) {
        Preconditions.checkNotNull(entityId, "entity id is required");
        Preconditions.checkNotNull(version, "version id is required");
        Preconditions.checkNotNull(status, "status  is required");
        Preconditions.checkNotNull(entity, "entity  is required");

        this.entityId = entityId;
        this.version = version;
        this.entity = entity;
        this.status = status;
        this.size = size;
    }


    @Override
    public Optional<Entity> getEntity() {
        return entity;
    }


    @Override
    public UUID getVersion() {
        return version;
    }


    @Override
    public Id getId() {
        return entityId;
    }


    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public void setSize(long size) {
        this.size = size;
        if(this.entity.isPresent()){
            this.entity.get().setSize(size);
        }
    }

    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !( o instanceof MvccEntityImpl ) ) {
            return false;
        }

        final MvccEntityImpl that = ( MvccEntityImpl ) o;

        if ( !entityId.equals( that.entityId ) ) {
            return false;
        }
        if ( status != that.status ) {
            return false;
        }
        if ( !version.equals( that.version ) ) {
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = entityId.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + status.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "MvccEntityImpl{" +
                ", entityId=" + entityId +
                ", version=" + version +
                ", entity=" + entity +
                '}';
    }
}
