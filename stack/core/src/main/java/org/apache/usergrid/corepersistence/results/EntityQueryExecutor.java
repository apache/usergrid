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

package org.apache.usergrid.corepersistence.results;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.usergrid.corepersistence.pipeline.read.ResultsPage;
import org.apache.usergrid.corepersistence.util.CpEntityMapUtils;
import org.apache.usergrid.persistence.EntityFactory;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;

import rx.Observable;


/**
 * Processes our results of entities
 */
@Deprecated//Required for 1.0 compatibility
public abstract class EntityQueryExecutor extends ObservableQueryExecutor<Entity> {


    protected EntityQueryExecutor( final Optional<String> startCursor ) {
        super( startCursor );
    }


    @Override
    protected Results createResults( final ResultsPage resultsPage ) {

        final List<Entity> entityList = resultsPage.getEntityList();
        final List<org.apache.usergrid.persistence.Entity> resultsEntities = new ArrayList<>( entityList.size() );


        for ( final Entity entity : entityList ) {
            resultsEntities.add( mapEntity( entity ) );
        }

        final Results results = Results.fromEntities( resultsEntities );

        return results;
    }


    /**
     *
     * @param cpEntity
     * @return
     */
    private org.apache.usergrid.persistence.Entity mapEntity( final Entity cpEntity ) {


        final Id entityId = cpEntity.getId();

        org.apache.usergrid.persistence.Entity entity =
            EntityFactory.newEntity( entityId.getUuid(), entityId.getType() );

        entity.setProperties( cpEntity );

        return entity;
    }



}
