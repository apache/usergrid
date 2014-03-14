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
 * Models the metrics associated with an application. The UUID for the id parameter is considered unique in the system,
 * so equals and hashCode are delegated to such.
 *
 * @author zznate
 */
public class AppScore {
    private final OrgScore orgScore;
    private final UUID appId;
    private final String appName;
    private long userCount;
    private long requestCount;


    public AppScore( OrgScore orgScore, UUID appId, String appName ) {
        this.orgScore = orgScore;
        this.appId = appId;
        this.appName = appName;
    }


    public OrgScore getOrgScore() {
        return orgScore;
    }


    public UUID getAppId() {
        return appId;
    }


    public String getAppName() {
        return appName;
    }


    public long getUserCount() {
        return userCount;
    }


    public long getRequestCount() {
        return requestCount;
    }


    public void setUserCount( long userCount ) {
        this.userCount = userCount;
    }


    public void setRequestCount( long requestCount ) {
        this.requestCount = requestCount;
    }


    /** Returns the hashCode of he appid parameter */
    @Override
    public int hashCode() {
        return appId.hashCode();
    }


    /**
     * Checks the equality of the appId vs. o.getAppId()
     *
     * @return true if the appId attributes are equal
     */
    @Override
    public boolean equals( Object o ) {
        if ( o instanceof AppScore ) {
            return ( ( AppScore ) o ).getAppId().equals( appId );
        }
        return false;
    }
}
