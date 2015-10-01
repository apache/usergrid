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


import org.apache.usergrid.corepersistence.pipeline.cursor.CursorSerializer;
import org.apache.usergrid.corepersistence.pipeline.read.AbstractPathFilter;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;

import rx.Observable;


/**
 * A filter that is used when we can potentially serialize pages via cursor.  This will filter the first result, only if
 * it matches the Id that was set
 */
public class IdResumeFilter extends AbstractPathFilter<Id, Id, Id>  {


    @Override
    public Observable<FilterResult<Id>> call( final Observable<FilterResult<Id>> filterResultObservable ) {

        //filter only the first id, then map into our path for our next pass


        //skip our first and emit if neccessary
        return filterResultObservable.skipWhile( filterResult -> {

            final Optional<Id> startFromCursor = getSeekValue();

            return startFromCursor.isPresent() && startFromCursor.get().equals( filterResult.getValue() );
        } );
    }


    @Override
    protected CursorSerializer<Id> getCursorSerializer() {
        return IdCursorSerializer.INSTANCE;
    }
}
