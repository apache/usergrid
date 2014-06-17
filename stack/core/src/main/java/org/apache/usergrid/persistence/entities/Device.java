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
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;


import org.apache.usergrid.persistence.TypedEntity;
import org.apache.usergrid.persistence.annotations.EntityCollection;
import org.apache.usergrid.persistence.annotations.EntityProperty;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;

/** The Device entity class for representing devices in the service. */
@XmlRootElement
public class Device extends TypedEntity {

    public static final String ENTITY_TYPE = "device";
    public static final String RECEIPTS_COLLECTION = "receipts";

    @EntityProperty(indexed = true, fulltextIndexed = false, required = false, aliasProperty = true, unique = true,
            basic = true)
    protected String name;

    @EntityCollection(type = "user", linkedCollection = "devices")
    protected List<UUID> users;

    @EntityCollection(type = "receipt")
    protected List<UUID> receipts;

    @EntityProperty
    protected Integer badge;


    public Device() {
        // id = UUIDUtils.newTimeUUID();
    }


    public Device( UUID id ) {
        uuid = id;
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
    public List<UUID> getUsers() {
        return users;
    }


    public void setUsers( List<UUID> users ) {
        this.users = users;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public List<UUID> getReceipts() {
        return receipts;
    }


    public void setReceipts( List<UUID> receipts ) {
        this.receipts = receipts;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public Integer getBadge() {
        return badge;
    }


    public void setBadge( Integer badge ) {
        this.badge = badge;
    }
}
