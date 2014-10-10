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
package org.apache.usergrid.persistence.collection.mvcc.changelog;


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.Field;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;


/**
 * A default implementation of {@link ChangeLogGenerator}.
 */
public class ChangeLogGeneratorImpl implements ChangeLogGenerator {

    /**
     * See parent comment {@link ChangeLogGenerator#getChangeLog(java.util.Collection)}
     */
    @Override
    public ChangeLog getChangeLog( Collection<MvccEntity> mvccEntities ) {


        Preconditions.checkArgument( mvccEntities.size() > 0, "You must specify at least 1 entities for a change log" );

        //TODO, this is a SWAG on the entity size, this may be too little or too much.
        final ChangeLogImpl changeLog = new ChangeLogImpl( 50 );

        Iterator<MvccEntity> iterator = mvccEntities.iterator();

        Set<String> previousFieldNames = getFieldNames( iterator.next().getEntity() );

        Set<String> currentFieldNames = null;

        while ( iterator.hasNext() ) {


            final MvccEntity mvccEntity = iterator.next();

            currentFieldNames = getFieldNames( mvccEntity.getEntity() );

            if(mvccEntity.getStatus() == MvccEntity.Status.DELETED){
                changeLog.clear();
                continue;
            }

            final Entity currentEntity = mvccEntity.getEntity().orNull();

            //get all fields in the current field that aren't in the previous fields
            final Set<String> deletedFields = Sets.difference( previousFieldNames, currentFieldNames );

            changeLog.addDeletes( deletedFields );

            for ( String addedField : currentFieldNames ) {
                changeLog.addWrite( currentEntity.getField( addedField ) );
            }


            previousFieldNames = currentFieldNames;
        }

        //subtract off the the last set of fields from the entity
        if(currentFieldNames != null) {
            changeLog.clear( currentFieldNames );
        }


        return changeLog;
    }


    /**
     * Get all the fieldNames on this entity
     */
    private Set<String> getFieldNames( final Optional<Entity> entity ) {
        if ( !entity.isPresent() ) {
            return Collections.emptySet();
        }


        Collection<Field> fields = entity.get().getFields();

        Set<String> fieldNames = new HashSet<>( fields.size() );

        for ( final Field field : entity.get().getFields() ) {
            fieldNames.add( field.getName() );
        }

        return fieldNames;
    }
}
