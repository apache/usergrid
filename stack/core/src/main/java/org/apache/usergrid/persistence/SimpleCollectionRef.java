/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence;


import java.util.UUID;

import org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;


public class SimpleCollectionRef implements CollectionRef {

    public static final String MEMBER_ENTITY_TYPE = "member";

    protected final EntityRef ownerRef;
    protected final String collectionName;
    protected final EntityRef itemRef;
    protected final String type;
    protected final UUID id;


    public SimpleCollectionRef( EntityRef ownerRef, String collectionName, EntityRef itemRef ) {
        this.ownerRef = ownerRef;
        this.collectionName = collectionName;
        this.itemRef = itemRef;
        type = itemRef.getType() + ":" + MEMBER_ENTITY_TYPE;
        id = CassandraPersistenceUtils.keyID( ownerRef.getUuid(), collectionName, itemRef.getUuid() );
    }


    @Override
    public EntityRef getOwnerEntity() {
        return ownerRef;
    }


    @Override
    public String getCollectionName() {
        return collectionName;
    }


    @Override
    public EntityRef getItemRef() {
        return itemRef;
    }


    @Override
    public UUID getUuid() {
        return id;
    }


    @Override
    public String getType() {
        return type;
    }


    @Override
    public Id asId() {
        return new SimpleId( id, type );
    }


    @Override
    public String toString() {
        if ( ( type == null ) && ( id == null ) ) {
            return "CollectionRef(" + SimpleEntityRef.NULL_ID.toString() + ")";
        }
        if ( type == null ) {
            return "CollectionRef(" + id.toString() + ")";
        }
        return type + "(" + id + "," + ownerRef + "," + collectionName + "," + itemRef + ")";
    }
}
