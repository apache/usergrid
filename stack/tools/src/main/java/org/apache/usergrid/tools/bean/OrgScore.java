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


import java.util.UUID;


/**
 * Models an organization metrics. The UUID for the id parameter is considered unique in the system, so equals and
 * hashCode are delegated to such.
 *
 * @author zznate
 */
public class OrgScore {
    private final UUID id;
    private final String name;
    private long adminLogins;
    private long userCount;
    private long adminCount;
    private long appCount;


    public OrgScore( UUID id, String name ) {
        this.id = id;
        this.name = name;
    }


    public UUID getId() {
        return id;
    }


    public String getName() {
        return name;
    }


    public long getUserCount() {
        return userCount;
    }


    public void addToUserCount( long userCount ) {
        this.userCount += userCount;
    }


    public long getAdminCount() {
        return adminCount;
    }


    public void setAdminCount( long adminCount ) {
        this.adminCount = adminCount;
    }


    public long getAdminLogins() {
        return adminLogins;
    }


    public void setAdminLogins( long adminLogins ) {
        this.adminLogins = adminLogins;
    }


    public long getAppCount() {
        return appCount;
    }


    public void setAppCount( long appCount ) {
        this.appCount = appCount;
    }


    /** Delegates to id UUID */
    @Override
    public int hashCode() {
        return id.hashCode();
    }


    /** Delegates to the id UUID */
    @Override
    public boolean equals( Object o ) {
        if ( o instanceof OrgScore ) {
            return ( ( OrgScore ) o ).getId().equals( id );
        }
        return false;
    }
}
