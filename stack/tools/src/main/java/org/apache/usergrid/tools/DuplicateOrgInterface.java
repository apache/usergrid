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

import rx.Observable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;


/**
 * Abstraction to make duplicate org repair testable.
 */
interface DuplicateOrgInterface {
    
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
    
    class Org implements Comparable<Org> {
        private UUID id;
        private String name;
        private long created;
        public Object sourceValue;
        
        public Org( UUID id, String name, long created ) {
            this.id = id;
            this.name = name;
            this.created = created;
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
    
    class OrgUser {
        private UUID id;
        private String name;
        public Object sourceValue;
        
        public OrgUser( UUID id, String name ) {
            this.id = id;
            this.name = name;
        }

        public UUID getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
