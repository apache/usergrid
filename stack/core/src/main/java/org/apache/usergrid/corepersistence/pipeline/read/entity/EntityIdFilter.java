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

package org.apache.usergrid.corepersistence.pipeline.read.entity;


import org.apache.usergrid.corepersistence.pipeline.read.AbstractPipelineOperation;
import org.apache.usergrid.corepersistence.pipeline.read.Filter;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import rx.Observable;


/**
 * This command is a stopgap to make migrating 1.0 code easier.  Once full traversal has been implemented, this should
 * be removed
 */
public class EntityIdFilter extends AbstractPipelineOperation<Id, Id> implements Filter<Id, Id> {

    private final Id entityId;


    @Inject
    public EntityIdFilter( @Assisted final Id entityId ) {this.entityId = entityId;}




    @Override
    public Observable<Id> call( final Observable<Id> idObservable ) {
        return Observable.just( entityId );
    }

}
