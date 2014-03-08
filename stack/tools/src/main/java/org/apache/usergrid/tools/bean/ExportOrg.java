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
package org.apache.usergrid.tools.bean;


import java.util.ArrayList;
import java.util.List;

import org.apache.usergrid.management.OrganizationInfo;


/** @author tnine */
public class ExportOrg extends OrganizationInfo {

    private List<String> adminUserNames;
    private int passwordHistorySize;


    public ExportOrg() {
        adminUserNames = new ArrayList<String>();
    }


    public ExportOrg( OrganizationInfo info ) {
        setName( info.getName() );
        setUuid( info.getUuid() );
        adminUserNames = new ArrayList<String>();
    }


    /** @return the admins */
    public List<String> getAdmins() {
        return adminUserNames;
    }


    /** @param admins the admins to set */
    public void setAdmins( List<String> admins ) {
        this.adminUserNames = admins;
    }


    public void addAdmin( String username ) {
        adminUserNames.add( username );
    }


    public int getPasswordHistorySize() {
        return passwordHistorySize;
    }


    public void setPasswordHistorySize( final int passwordHistorySize ) {
        this.passwordHistorySize = passwordHistorySize;
    }
}
