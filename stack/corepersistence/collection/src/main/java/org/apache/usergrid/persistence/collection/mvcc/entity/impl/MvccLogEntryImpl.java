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

import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.mvcc.entity.Stage;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Preconditions;


/**
 * The simple implementation of a log entry
 *
 * @author tnine
 */
public class MvccLogEntryImpl implements MvccLogEntry {

    private final Id entityId;
    private final UUID version;
    private final Stage stage;
    private final State state;


    public MvccLogEntryImpl( final Id entityId, final UUID version, final Stage stage, final State state ) {
        Preconditions.checkNotNull( entityId, "entity id is required" );
        ValidationUtils.verifyTimeUuid( version, "version" );
        Preconditions.checkNotNull( stage, "entity  is required" );
        Preconditions.checkNotNull( state, "state  is required" );


        this.entityId = entityId;
        this.version = version;
        this.stage = stage;
        this.state = state;
    }


    @Override
    public Stage getStage() {
        return stage;
    }


    @Override
    public Id getEntityId() {
        return entityId;
    }


    @Override
    public UUID getVersion() {
        return version;
    }

    @Override
    public State getState(){ return state;}


    @Override
    public boolean equals( final Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }

        final MvccLogEntryImpl that = ( MvccLogEntryImpl ) o;

        if ( !entityId.equals( that.entityId ) ) {
            return false;
        }
        if ( !version.equals( that.version ) ) {
            return false;
        }

        if ( stage != that.stage ) {
            return false;
        }

        if( state != that.state ){
            return false;
        }

        return true;
    }


    @Override
    public int hashCode() {
        int result = 31 * entityId.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + stage.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return "MvccLogEntryImpl{" +
                ", entityId=" + entityId +
                ", version=" + version +
                ", stage=" + stage +
                '}';
    }
}
