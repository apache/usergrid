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
package org.apache.usergrid.persistence.entities;


import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.usergrid.persistence.CredentialsInfo;
import org.apache.usergrid.persistence.TypedEntity;
import org.apache.usergrid.persistence.annotations.EntityCollection;
import org.apache.usergrid.persistence.annotations.EntityDictionary;
import org.apache.usergrid.persistence.annotations.EntityProperty;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;


/** Groups are used to organize users. */
@XmlRootElement
public class Group extends TypedEntity {

    public static final String ENTITY_TYPE = "group";

    public static final String CONNECTION_MEMBERSHIP = "membership";

    @EntityProperty(indexed = true, fulltextIndexed = false, required = true, aliasProperty = true,
            pathBasedName = true, mutable = true, unique = true, basic = true)
    protected String path;

    @EntityDictionary(keyType = java.lang.String.class)
    protected Set<String> connections;

    @EntityDictionary(keyType = java.lang.String.class, valueType = java.lang.String.class)
    protected Map<String, String> rolenames;

    @EntityDictionary(keyType = java.lang.String.class)
    protected Set<String> permissions;

    @EntityDictionary(keyType = java.lang.String.class, valueType = CredentialsInfo.class)
    protected Map<String, CredentialsInfo> credentials;

    @EntityCollection(type = "user", linkedCollection = "groups")
    protected List<UUID> users;

    @EntityCollection(type = "activity", reversed = true, sort = "published desc", indexingDynamicDictionaries = true)
    protected List<UUID> activities;

    @EntityCollection(type = "activity", reversed = true, sort = "published desc", indexingDynamicDictionaries = true)
    protected List<UUID> feed;

    @EntityCollection(type = "role", linkedCollection = "groups", indexingDynamicDictionaries = true)
    protected List<UUID> roles;


    public Group() {
        // id = UUIDUtils.newTimeUUID();
    }


    public Group( UUID id ) {
        uuid = id;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getPath() {
        return path;
    }


    public void setPath( String path ) {
        this.path = path;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public List<UUID> getUsers() {
        return users;
    }


    public void setUsers( List<UUID> users ) {
        this.users = users;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public Set<String> getConnections() {
        return connections;
    }


    public void setConnections( Set<String> connections ) {
        this.connections = connections;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public Map<String, String> getRolenames() {
        return rolenames;
    }


    public void setRolenames( Map<String, String> rolenames ) {
        this.rolenames = rolenames;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public List<UUID> getActivities() {
        return activities;
    }


    public void setActivities( List<UUID> activities ) {
        this.activities = activities;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public List<UUID> getFeed() {
        return feed;
    }


    public void setFeed( List<UUID> feed ) {
        this.feed = feed;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public Map<String, CredentialsInfo> getCredentials() {
        return credentials;
    }


    public void setCredentials( Map<String, CredentialsInfo> credentials ) {
        this.credentials = credentials;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public List<UUID> getRoles() {
        return roles;
    }


    public void setRoles( List<UUID> roles ) {
        this.roles = roles;
    }
}
