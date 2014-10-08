package org.apache.usergrid.persistence.collection.serialization.impl;/*
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


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.collection.mvcc.changelog.ChangeLog;
import org.apache.usergrid.persistence.collection.mvcc.changelog.ChangeLogGenerator;
import org.apache.usergrid.persistence.collection.mvcc.changelog.ChangeLogGeneratorImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.serialization.EntityRepair;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.Field;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class EntityRepairImpl implements EntityRepair {


    private static final ChangeLogGenerator CHANGE_LOG_GENERATOR = new ChangeLogGeneratorImpl();

    private final MvccEntitySerializationStrategy mvccEntitySerializationStrategy;
    private final SerializationFig serializationFig;


    @Inject
    public EntityRepairImpl( final MvccEntitySerializationStrategy mvccEntitySerializationStrategy,
                             final SerializationFig serializationFig ) {
        this.mvccEntitySerializationStrategy = mvccEntitySerializationStrategy;
        this.serializationFig = serializationFig;
    }


    @Override
    public MvccEntity maybeRepair( final CollectionScope collectionScope, final MvccEntity targetEntity ) {
        if ( !needsRepaired( targetEntity ) ) {
            return targetEntity;
        }


        final List<MvccEntity> partialEntities = new ArrayList<>( serializationFig.getBufferSize() );

        partialEntities.add( targetEntity );

        final Iterator<MvccEntity> results = mvccEntitySerializationStrategy
                .load( collectionScope, targetEntity.getId(), targetEntity.getVersion(),
                        serializationFig.getBufferSize() );



        //discard the one that's equal to the version we were passed in
        if(results.hasNext() ){
            results.next();
        }


//        MvccEntity oldestCompleteEntity;


        while ( results.hasNext() ) {
            final MvccEntity mvccEntity = results.next();
            partialEntities.add( mvccEntity );


            if ( !needsRepaired( mvccEntity ) ) {
                break;
            }
        }

        Collections.reverse( partialEntities );

        final ChangeLog changeLog =
                CHANGE_LOG_GENERATOR.getChangeLog( partialEntities );


        //repair
        final MvccEntity mergedEntity = entityRepair( changeLog, targetEntity );

        try {
            mvccEntitySerializationStrategy.write( collectionScope, mergedEntity ).execute();
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Couldn't rewrite repaired entity", e );
        }

        return mergedEntity;

    }


    /**
     * Appies changes to the entity log, oldest to newest version
     */
    private MvccEntity entityRepair( final ChangeLog changeLog, final MvccEntity targetEntity ) {

        //TODO review this, why do we care other than just replaying the changelog?

        final Entity entity = targetEntity.getEntity().get();

        for(final String removedField: changeLog.getDeletes()){
            entity.removeField( removedField );
        }

        for(final Field newField : changeLog.getWrites()){
            entity.setField( newField );
        }

        return targetEntity;
    }


    /**
     * Returns true if the entity needs repaired
     */
    private boolean needsRepaired( final MvccEntity headEntity ) {

        final MvccEntity.Status status = headEntity.getStatus();

        return !(status == MvccEntity.Status.COMPLETE || status == MvccEntity.Status.DELETED || !headEntity.getEntity().isPresent());
    }
}
