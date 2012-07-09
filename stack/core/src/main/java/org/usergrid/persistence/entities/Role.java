/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.persistence.entities;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.usergrid.persistence.TypedEntity;
import org.usergrid.persistence.annotations.EntityCollection;
import org.usergrid.persistence.annotations.EntityDictionary;
import org.usergrid.persistence.annotations.EntityProperty;

/**
 * Groups are used to organize users.
 */
@XmlRootElement
public class Role extends TypedEntity {

    public static final String ENTITY_TYPE = "role";

    @EntityProperty(indexed = true, fulltextIndexed = false, required = true, indexedInConnections = false, aliasProperty = true, mutable = false, unique = true)
    protected String name;

    @EntityProperty(mutable = true)
    protected String roleName;

    @EntityProperty(mutable = true)
    protected String title;

    @EntityProperty(mutable = true)
    protected Long inactivity;

    @EntityDictionary(keyType = java.lang.String.class)
    protected Set<String> permissions;

    @EntityCollection(type = "user", linkedCollection = "roles")
    protected List<UUID> users;

    @EntityCollection(type = "group", linkedCollection = "roles")
    protected List<UUID> groups;

    public Role() {
        // id = UUIDUtils.newTimeUUID();
    }

    public Role(String roleName) {
        this.roleName = roleName;
    }

    public Role(UUID id) {
        this.uuid = id;
    }

    @Override
    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the inactivity
     */
    public Long getInactivity() {
        return inactivity;
    }

    /**
     * @param inactivity
     *            the inactivity to set
     */
    public void setInactivity(Long inactivity) {
        this.inactivity = inactivity;
    }

    @JsonSerialize(include = Inclusion.NON_NULL)
    public List<UUID> getUsers() {
        return users;
    }

    public void setUsers(List<UUID> users) {
        this.users = users;
    }

    @JsonSerialize(include = Inclusion.NON_NULL)
    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    @JsonSerialize(include = Inclusion.NON_NULL)
    public List<UUID> getGroups() {
        return groups;
    }

    public void setGroups(List<UUID> groups) {
        this.groups = groups;
    }

}
