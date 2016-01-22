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
package org.apache.usergrid.tools;

import org.apache.commons.lang3.builder.EqualsBuilder;
import rx.Observable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;


/**
 * Mockable abstraction of user-org management.
 */
interface UserOrgInterface {

    Observable<Org> getOrgs() throws Exception;

    Observable<OrgUser> getUsers() throws Exception;

    Set<Org> getUsersOrgs(OrgUser user) throws Exception;

    Set<OrgUser> getOrgUsers(Org org ) throws Exception;

    void removeOrg(Org keeper, Org duplicate) throws Exception;

    void removeUserFromOrg( OrgUser user, Org org ) throws Exception;

    void addUserToOrg( OrgUser user, Org org ) throws Exception;

    Set<UUID> getOrgApps(Org org) throws Exception;

    void removeAppFromOrg( UUID appId, Org org ) throws Exception;

    void addAppToOrg( UUID appId, Org org ) throws Exception;

    void logDuplicates(Map<String, Set<Org>> duplicatesByName);

    Org getOrg(UUID id ) throws Exception;

    OrgUser getOrgUser(UUID id ) throws Exception;

    OrgUser lookupOrgUserByUsername( String username ) throws Exception;

    OrgUser lookupOrgUserByEmail( String email ) throws Exception;

    void removeOrgUser( OrgUser orgUser ) throws Exception;

    void updateOrgUser(OrgUser targetUserEntity) throws Exception;

    void setOrgUserName(OrgUser other, String newUserName) throws Exception;

    Org selectBest( Set<Org> candidates ) throws Exception;

    class Org implements Comparable<Org> {
        private UUID id;
        private String name;
        private long created;
        public Object sourceValue;

        public Org( UUID id, String name, long created) {
            this.id = id;
            this.name = name;
            this.created = created;
            this.created = System.currentTimeMillis();
        }

        public Org( UUID id, String name) {
            this( id, name, System.currentTimeMillis());
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof Org ) {
                Org other = (Org)o;
                return getId().equals( other.getId() );
            }
            return false;
        }

        @Override
        public int compareTo(Org o) {
            return Long.compare( this.created, o.created );
        }

        public UUID getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public long getCreated() {
            return created;
        }
    }

    class OrgUser implements Comparable<OrgUser> {
        private UUID id;
        private String username;
        private String email;
        private long created;
        public Object sourceValue;

        public OrgUser( UUID id, String name, String email, long created ) {
            this.id = id;
            this.username = name;
            this.email = email;
            this.created = created;
        }

        public OrgUser( UUID id, String name, String email ) {
            this( id, name, email, System.currentTimeMillis());
        }

        public UUID getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername( String username ) {
            this.username = username;
        }

        public long getCreated() {
            return created;
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == null) { return false; }
            if (obj == this) { return true; }
            if (obj.getClass() != getClass()) {
                return false;
            }
            OrgUser rhs = (OrgUser) obj;
            return new EqualsBuilder().appendSuper(super.equals(obj))
                .append(id,       rhs.id)
                .append(username, rhs.username)
                .append(email,    rhs.email)
                .append(created,  rhs.created)
                .isEquals();
        }

        @Override
        public int compareTo(OrgUser o) {
            return Long.compare( this.created, o.created );
        }
    }
}
