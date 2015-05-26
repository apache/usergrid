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


import org.apache.usergrid.corepersistence.pipeline.read.AbstractFilter;
import org.apache.usergrid.corepersistence.pipeline.read.EdgePath;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.persistence.ConnectionRef;
import org.apache.usergrid.persistence.cassandra.ConnectionRefImpl;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import rx.Observable;


/**
 * This class only exists for 1.0 compatibility, remove once services no longer need connection refs
 */
public class ConnectionRefFilter extends AbstractFilter<FilterResult<Id>, FilterResult<ConnectionRef>> {


    private final Id sourceId;
    private final String connectionType;


    @Inject
    public ConnectionRefFilter( @Assisted( "sourceId" ) final Id sourceId,
                                @Assisted( "connectionType" ) final String connectionType ) {
        this.sourceId = sourceId;
        this.connectionType = connectionType;
    }


    @Override
    public Observable<FilterResult<ConnectionRef>> call( final Observable<FilterResult<Id>> filterResultObservable ) {

        return filterResultObservable.map( targetResult -> {

            final Id targetId = targetResult.getValue();
            final ConnectionRef ref =
                new ConnectionRefImpl( sourceId.getType(), sourceId.getUuid(), connectionType, targetId.getType(),
                    targetId.getUuid() );

            return new FilterResult<>( ref, Optional.<EdgePath>absent() );
        } );
    }
}
