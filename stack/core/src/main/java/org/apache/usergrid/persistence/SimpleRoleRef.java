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

import org.springframework.util.Assert;
import org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils;
import org.apache.usergrid.persistence.entities.Group;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.utils.StringUtils;
import org.apache.usergrid.utils.UUIDUtils;


public class SimpleRoleRef implements RoleRef {

    protected final UUID groupId;
    protected final String roleName;
    protected final UUID id;


    public SimpleRoleRef( String roleName ) {
        this( null, roleName );
    }


    public SimpleRoleRef( UUID groupId, String roleName ) {
        Assert.notNull( roleName );
        if ( groupId != null ) {
            this.groupId = groupId;
        }
        else {
            this.groupId = UUIDUtils.tryExtractUUID( roleName );
        }
        this.roleName = StringUtils.stringOrSubstringAfterLast( roleName.toLowerCase(), ':' );
        if ( groupId == null ) {
            id = CassandraPersistenceUtils.keyID( "role", this.groupId, this.roleName );
        }
        else {
            id = CassandraPersistenceUtils.keyID( "role", this.roleName );
        }
    }


    public static SimpleRoleRef forRoleEntity( Entity role ) {
        if ( role == null ) {
            return null;
        }
        UUID groupId = ( UUID ) role.getProperty( "group" );
        String name = role.getName();
        return new SimpleRoleRef( groupId, name );
    }


    public static SimpleRoleRef forRoleName( String roleName ) {
        return new SimpleRoleRef( null, roleName );
    }


    public static SimpleRoleRef forGroupIdAndRoleName( UUID groupId, String roleName ) {
        return new SimpleRoleRef( groupId, roleName );
    }


    public static UUID getIdForRoleName( String roleName ) {
        return forRoleName( roleName ).getUuid();
    }


    public static UUID getIdForGroupIdAndRoleName( UUID groupId, String roleName ) {
        return forGroupIdAndRoleName( groupId, roleName ).getUuid();
    }


    @Override
    public UUID getUuid() {
        return id;
    }


    @Override
    public String getType() {
        return "role";
    }


    @Override
    public Id asId() {
        return new SimpleId( id, "role" );
    }


    @Override
    public EntityRef getGroupRef() {
        return new SimpleEntityRef( Group.ENTITY_TYPE, groupId );
    }


    @Override
    public String getRoleName() {
        return roleName;
    }


    @Override
    public UUID getGroupId() {
        return groupId;
    }


    @Override
    public String getApplicationRoleName() {
        if ( groupId == null ) {
            return roleName;
        }
        return groupId + ":" + roleName;
    }
}
