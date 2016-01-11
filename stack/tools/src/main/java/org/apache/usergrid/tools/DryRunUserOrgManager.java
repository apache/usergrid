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

import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManagerFactory;

import java.util.UUID;

class DryRunUserOrgManager extends UserOrgManager {

    public DryRunUserOrgManager(EntityManagerFactory emf, ManagementService managementService) {
        super( emf, managementService );
    }

    @Override
    public void removeUserFromOrg(OrgUser user, Org org) throws Exception {
    }

    @Override
    public void addUserToOrg(OrgUser user, Org org) throws Exception {
    }

    @Override
    public void addAppToOrg(UUID appId, Org org) throws Exception {
    }

    @Override
    public void removeOrgUser(OrgUser orgUser) {
    }

    @Override
    public void updateOrgUser(OrgUser targetUserEntity) {
    }

    @Override
    public void setOrgUserName(OrgUser other, String newUserName) {

    }

    @Override
    public void removeAppFromOrg(UUID appId, Org org) throws Exception {
    }

    @Override
    public void removeOrg(Org keeper, Org duplicate) throws Exception {
    }
}
