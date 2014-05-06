/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.webapp.dao.model;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.usergrid.chop.api.Module;

public class BasicModule implements Module {

    private String id;
    private String groupId;
    private String artifactId;
    private String version;
    private String vcsRepoUrl;
    private String testPackageBase;


    public BasicModule( String groupId, String artifactId, String version, String vcsRepoUrl, String testPackageBase ) {
        id = createId( groupId, artifactId, version );
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.vcsRepoUrl = vcsRepoUrl;
        this.testPackageBase = testPackageBase;
    }


    @Override
    public String getId() {
        return id;
    }


    public static String createId( String groupId, String artifactId, String version ) {
        int hash = new HashCodeBuilder( 97, 239 )
                .append( groupId )
                .append( artifactId )
                .append( version )
                .toHashCode();
        if( hash < 0 ) {
            hash += Integer.MAX_VALUE;
        }
        return "" + hash;
    }


    @Override
    public String getGroupId() {
        return groupId;
    }


    @Override
    public String getArtifactId() {
        return artifactId;
    }


    @Override
    public String getVersion() {
        return version;
    }


    @Override
    public String getVcsRepoUrl() {
        return vcsRepoUrl;
    }


    @Override
    public String getTestPackageBase() {
        return testPackageBase;
    }


    @Override
    public String toString() {
        return new ToStringBuilder( this, ToStringStyle.SHORT_PREFIX_STYLE )
                .append( "id", id )
                .append( "groupId", groupId )
                .append( "artifactId", artifactId )
                .append( "version", version )
                .append( "vcsRepoUrl", vcsRepoUrl )
                .append( "testPackageBase", testPackageBase )
                .toString();
    }


    @Override
    public boolean equals( final Object obj ) {
        if( this == obj ) {
            return true;
        }
        return obj != null &&
                obj instanceof BasicModule &&
                this.id.equals( ( ( BasicModule ) obj ).id  );
    }
}
