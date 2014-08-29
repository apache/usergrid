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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * A help class to group runners by user, commit and module.
 */
public class RunnerGroup {

    private String user;
    private String commitId;
    private String moduleId;


    public RunnerGroup( String user, String commitId, String moduleId ) {
        this.user = user;
        this.commitId = commitId;
        this.moduleId = moduleId;
    }


    /**
     * By default ElasticSearch ignores the dash in search. We need to fix this to get correct result.
     *
     * @return  hashCode of this RunnerGroup
     */
    public String getId() {
        return "" + hashCode();
    }


    public String getUser() {
        return user;
    }


    public String getCommitId() {
        return commitId;
    }


    public String getModuleId() {
        return moduleId;
    }


    /**
     * @return  true if any of values (user, commit, module) is null
     */
    public boolean isNull() {
        return StringUtils.isEmpty( user )
                || StringUtils.isEmpty( commitId )
                || StringUtils.isEmpty( moduleId );
    }


    @Override
    public boolean equals( Object v ) {
        if ( ! ( v instanceof RunnerGroup ) ) {
            return false;
        }

        RunnerGroup other = ( RunnerGroup ) v;

        return new EqualsBuilder()
                .append( user, other.getUser() )
                .append( commitId, other.getCommitId() )
                .append( moduleId, other.getModuleId() )
                .isEquals();
    }


    @Override
    public int hashCode() {
        return Math.abs( new HashCodeBuilder()
                .append( user )
                .append( commitId )
                .append( moduleId )
                .hashCode() );
    }


    @Override
    public String toString() {
        return new ToStringBuilder( this, ToStringStyle.SHORT_PREFIX_STYLE )
                .append( "user", user )
                .append( "commitId", commitId )
                .append( "moduleId", moduleId )
                .toString();
    }
}
