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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;

import static org.apache.commons.lang.StringUtils.isNotBlank;


/** The User entity class for representing users in the service. */
@XmlRootElement
public class User extends TypedEntity {

    public static final String ENTITY_TYPE = "user";

    public static final String CONNECTION_FOLLOW = "follow";

    public static final String PROPERTY_UUID = "uuid";

    public static final String PROPERTY_EMAIL = "email";

    public static final String PROPERTY_HASHTYPE = "hashtype";

    public static final String HASHTYPE_MD5 = "md5";

    @EntityProperty(indexed = true, fulltextIndexed = false, required = true, aliasProperty = true, unique = true,
            basic = true)
    protected String username;

    @EntityProperty(indexed = true, unique = true, basic = true)
    protected String email;

    @EntityProperty(indexed = true, fulltextIndexed = true)
    protected String name;

    @EntityProperty(indexed = true)
    protected Boolean activated;

    @EntityProperty(indexed = true)
    protected Boolean confirmed;

    @EntityProperty(indexed = true)
    protected Boolean disabled;

    @EntityProperty(indexed = false)
    protected String picture;

    /** The time this user was deactivated */
    @EntityProperty(indexed = true)
    protected Long deactivated;

    @EntityDictionary(keyType = java.lang.String.class)
    protected Set<String> connections;

    @EntityDictionary(keyType = java.lang.String.class, valueType = java.lang.String.class)
    protected Map<String, String> rolenames;

    @EntityDictionary(keyType = java.lang.String.class)
    protected Set<String> permissions;

    @EntityDictionary(keyType = java.lang.String.class, valueType = CredentialsInfo.class)
    protected Map<String, CredentialsInfo> credentials;

    @EntityCollection(type = "group", linkedCollection = "users")
    protected List<UUID> groups;

    @EntityCollection(type = "device", linkedCollection = "users")
    protected List<UUID> devices;

    @EntityCollection(type = "activity", reversed = true, sort = "published desc", indexingDynamicDictionaries = true)
    protected List<UUID> activities;

    @EntityCollection(type = "activity", reversed = true, sort = "published desc", indexingDynamicDictionaries = true)
    protected List<UUID> feed;

    @EntityCollection(type = "role", linkedCollection = "users", indexingDynamicDictionaries = true)
    protected List<UUID> roles;

    @JsonIgnore
    @EntityDictionary(keyType = String.class, valueType = CredentialsInfo.class)
    protected Map<String, CredentialsInfo> credentialsHistory;


    public Map<String, CredentialsInfo> getCredentialsHistory() {
        return credentialsHistory;
    }


    public void setCredentialsHistory( Map<String, CredentialsInfo> credentialsHistory ) {
        this.credentialsHistory = credentialsHistory;
    }


    public User() {
        // id = UUIDUtils.newTimeUUID();
    }


    public User( UUID id ) {
        uuid = id;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getUsername() {
        return username;
    }


    public void setUsername( String username ) {
        this.username = username;
    }


    @Override
    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getName() {
        return name;
    }


    public void setName( String name ) {
        this.name = name;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getEmail() {
        return email;
    }


    @JsonIgnore
    public String getDisplayEmailAddress() {
        if ( isNotBlank( name ) ) {
            return name + " <" + email + ">";
        }
        return email;
    }


    public void setEmail( String email ) {
        this.email = email;
    }


    public boolean activated() {
        return ( activated != null ) && activated;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public Boolean getActivated() {
        return activated;
    }


    public void setActivated( Boolean activated ) {
        this.activated = activated;

        if ( activated ) {
            deactivated = null;
        }
    }


    /** @return the deactivated */
    @JsonSerialize(include = Inclusion.NON_NULL)
    public Long getDeactivated() {
        return deactivated;
    }


    /** @param deactivated the deactivated to set */
    public void setDeactivated( Long deactivated ) {
        this.deactivated = deactivated;
    }


    public boolean confirmed() {
        return ( confirmed != null ) && confirmed;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public Boolean getConfirmed() {
        return confirmed;
    }


    public void setConfirmed( Boolean confirmed ) {
        this.confirmed = confirmed;
    }


    public boolean disabled() {
        return ( disabled != null ) && disabled;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public Boolean getDisabled() {
        return disabled;
    }


    public void setDisabled( Boolean disabled ) {
        this.disabled = disabled;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getPicture() {
        return picture;
    }


    public void setPicture( String picture ) {
        this.picture = picture;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public List<UUID> getGroups() {
        return groups;
    }


    public void setGroups( List<UUID> groups ) {
        this.groups = groups;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public List<UUID> getDevices() {
        return devices;
    }


    public void setDevices( List<UUID> devices ) {
        this.devices = devices;
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
    public Set<String> getPermissions() {
        return permissions;
    }


    public void setPermissions( Set<String> permissions ) {
        this.permissions = permissions;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public Map<String, CredentialsInfo> getCredentials() {
        return credentials;
    }


    public void setCredentials( Map<String, CredentialsInfo> credentials ) {
        this.credentials = credentials;
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
    public List<UUID> getRoles() {
        return roles;
    }


    public void setRoles( List<UUID> roles ) {
        this.roles = roles;
    }
}
