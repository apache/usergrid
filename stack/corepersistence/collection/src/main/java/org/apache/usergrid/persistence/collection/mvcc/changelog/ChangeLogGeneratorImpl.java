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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.Field;

import com.fasterxml.uuid.UUIDComparator;


/**
 * A default implementation of {@link ChangeLogGenerator}.
 */
public class ChangeLogGeneratorImpl implements ChangeLogGenerator {

    /**
     * See parent comment
     * {@link ChangeLogGenerator#getChangeLog(org.apache.usergrid.persistence.model.entity.Id, java.util.List, java.util.UUID)}
     * @param mvccEntities
     */
    @Override
    public List<ChangeLogEntry> getChangeLog( List<MvccEntity> mvccEntities, UUID minVersion ) {

        Map<String, ChangeLogEntry> writeMap = new HashMap<String, ChangeLogEntry>();
        Map<String, ChangeLogEntry> deleteMap = new HashMap<String, ChangeLogEntry>();
        List<ChangeLogEntry> changeLog = new ArrayList<ChangeLogEntry>();
        Entity keeper = null;

        for ( MvccEntity mvccEntity : mvccEntities ) {

            Entity entity = mvccEntity.getEntity().get();

            int compare = UUIDComparator.staticCompare( mvccEntity.getVersion(), minVersion );

            if ( compare == 0 ) {
                keeper = entity;
            }
        }

        // TODO: what about cleared entities, all fields deleted but entity still there.
        // i.e. the optional entity will be delete

        for (MvccEntity mvccEntity : mvccEntities) {

            Entity entity = mvccEntity.getEntity().get();
            int compare = UUIDComparator.staticCompare(mvccEntity.getVersion(), minVersion);

            if (compare < 0) { // less than minVersion

                for (Field field : entity.getFields()) {

                    // only delete field if it is not in the keeper
                    Field keeperField = keeper.getField(field.getName());
                    if (keeperField == null
                            || keeperField.getValue() == null
                            || !keeperField.getValue().equals(field.getValue())) {

                        String key = field.getName() + field.getValue();
                        ChangeLogEntry cle = deleteMap.get(key);
                        if (cle == null) {
                            cle = new ChangeLogEntry(
                                    entity.getId(), mvccEntity.getVersion(),
                                    ChangeLogEntry.ChangeType.PROPERTY_DELETE, field);
                            changeLog.add(cle);
                        } else {
                            cle.addVersion(mvccEntity.getVersion());
                        }
                    }
                }

            } else { // greater than or equal to minVersion

                for (Field field : entity.getFields()) {

                    String key = field.getName() + field.getValue();
                    ChangeLogEntry cle = writeMap.get(key);
                    if (cle == null) {
                        cle = new ChangeLogEntry(
                                entity.getId(), mvccEntity.getVersion(),
                                ChangeLogEntry.ChangeType.PROPERTY_WRITE, field);
                        writeMap.put(key, cle);
                        changeLog.add(cle);
                    } else {
                        cle.addVersion(mvccEntity.getVersion());
                    }
                }
            }
        }
        return changeLog;
    }
 }
