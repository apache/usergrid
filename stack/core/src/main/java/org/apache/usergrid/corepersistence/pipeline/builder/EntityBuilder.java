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

package org.apache.usergrid.corepersistence.pipeline.builder;


import org.apache.usergrid.corepersistence.pipeline.Pipeline;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;
import org.apache.usergrid.corepersistence.pipeline.read.ResultsPage;
import org.apache.usergrid.corepersistence.pipeline.read.collect.EntityResumeFilter;
import org.apache.usergrid.corepersistence.pipeline.read.collect.ResultsPageCollector;
import org.apache.usergrid.persistence.model.entity.Entity;

import rx.Observable;


/**
 * Builder to build our entity state
 */
public class EntityBuilder {

    private final Pipeline<FilterResult<Entity>> pipeline;


    public EntityBuilder( final Pipeline<FilterResult<Entity>> pipeline ) {
        this.pipeline = pipeline;
    }


    /**
     * Build our results of entities
     * @return
     */
    public Observable<ResultsPage<Entity>> build(){
        //we must add our resume filter so we drop our previous page first element if it's present
        return pipeline.withFilter( new EntityResumeFilter() ).withFilter(new ResultsPageCollector<>()).execute();
    }
}
