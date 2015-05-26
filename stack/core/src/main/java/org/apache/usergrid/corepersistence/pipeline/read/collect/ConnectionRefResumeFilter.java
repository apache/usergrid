/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.pipeline.read.collect;


import java.util.UUID;

import org.apache.usergrid.corepersistence.pipeline.cursor.CursorSerializer;
import org.apache.usergrid.corepersistence.pipeline.read.AbstractPathFilter;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.persistence.ConnectedEntityRef;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.google.common.base.Optional;

import rx.Observable;


/**
 * A filter that is used when we can potentially serialize pages via cursor.  This will filter the first result, only if
 * it matches the Id that was set.   This is a 1.0 compatibility implementation, and should be removed when services no
 * longer depends on connection refs
 */
public class ConnectionRefResumeFilter extends AbstractPathFilter<ConnectionRef, ConnectionRef, Id> {


    @Override
    public Observable<FilterResult<ConnectionRef>> call(
        final Observable<FilterResult<ConnectionRef>> filterResultObservable ) {

        //filter only the first id, then map into our path for our next pass


        return filterResultObservable.skipWhile( filterResult -> {

            final Optional<Id> startFromCursor = getSeekValue();


            if ( !startFromCursor.isPresent() ) {
                return false;
            }

            final ConnectedEntityRef ref = filterResult.getValue().getTargetRefs();

            final Id entityId = startFromCursor.get();

            return entityId.getUuid().equals( ref.getUuid() ) && entityId.getType().equals( ref.getType() );
        } ).map( filterResult -> {


            final ConnectionRef entity = filterResult.getValue();

            final String type = entity.getTargetRefs().getType();
            final UUID uuid = entity.getTargetRefs().getUuid();

            final Id entityId = new SimpleId( uuid, type );

            return createFilterResult( entity, entityId, filterResult.getPath() );
        } );
    }


    @Override
    protected CursorSerializer<Id> getCursorSerializer() {
        return IdCursorSerializer.INSTANCE;
    }
}
