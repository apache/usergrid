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

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.Field;

/**
 * A default implementation of {@link ChangeLogGenerator}.
 */
public class ChangeLogGeneratorImpl implements ChangeLogGenerator {

    /**
     * See parent comment {@link ChangeLogGenerator#getChangeLog(org.apache.usergrid.persistence.model.entity.Id, java.util.List, java.util.UUID)}
     */
    @Override
    public List<ChangeLogEntry> getChangeLog( List<MvccEntity> mvccEntities, UUID minVersion ) {

        Map<String, ChangeLogEntry> deleteMap = new HashMap<String, ChangeLogEntry>();
        Map<byte[], ChangeLogEntry> writeMap = new HashMap<byte[], ChangeLogEntry>();

        List<ChangeLogEntry> changeLog = new ArrayList<ChangeLogEntry>();

        for (MvccEntity mvccEntity : mvccEntities) {

            System.out.println("-------------------------------------");
            System.out.println("Version " + mvccEntity.getVersion());

            Entity entity = mvccEntity.getEntity().get();

            int compare = mvccEntity.getVersion().compareTo(minVersion);

            if (compare == -1) { // less than minVersion

                for (Field field : entity.getFields()) {

                    String key = field.getName();

                    ChangeLogEntry cle = deleteMap.get( key );
                    if ( cle == null ) {
                        cle = new ChangeLogEntry( entity.getId(), null, 
                                ChangeLogEntry.ChangeType.PROPERTY_DELETE, field );
                        deleteMap.put( key, cle );
                        changeLog.add( 0, cle );
                    } 
                }

            } else { // greater than or equal to minVersion

                for (Field field : entity.getFields()) {

                    byte[] hash;
                    String key = field.getName() + field.getValue(); 
                    try {
                        MessageDigest instance = MessageDigest.getInstance( "MD5" );
                        hash = instance.digest( key.getBytes("UTF-8") );
                    } catch ( Exception ex ) {
                        throw new RuntimeException("MD5 not supported", ex);
                    }

                    ChangeLogEntry cle = writeMap.get( hash );
                    if ( cle == null ) {
                        cle = new ChangeLogEntry( entity.getId(), mvccEntity.getVersion(), 
                                ChangeLogEntry.ChangeType.PROPERTY_WRITE, field );
                        writeMap.put( hash, cle );
                        changeLog.add( 0, cle );
                    } else {
                        cle.addVersion( mvccEntity.getVersion() );
                    } 
                } 
            }
        }
        return changeLog;
    }
}
