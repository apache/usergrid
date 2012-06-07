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
package org.usergrid.tools.bean;

import java.util.ArrayList;
import java.util.List;

import org.usergrid.management.OrganizationInfo;
import org.usergrid.management.UserInfo;

/**
 * @author tnine
 *
 */
public class ExportOrg extends OrganizationInfo {

    private List<String> adminUserNames;
    
    public ExportOrg() {  
        adminUserNames = new ArrayList<String>();
    }
    
    public ExportOrg(OrganizationInfo info){
        setName(info.getName());
        setUuid(info.getUuid());
        adminUserNames = new ArrayList<String>();
    }

    /**
     * @return the admins
     */
    public List<String> getAdmins() {
        return adminUserNames;
    }

    /**
     * @param admins the admins to set
     */
    public void setAdmins(List<String> admins) {
        this.adminUserNames = admins;
    }
    
    
    public void addAdmin(String username){
        adminUserNames.add(username);
    }
    
    
}
