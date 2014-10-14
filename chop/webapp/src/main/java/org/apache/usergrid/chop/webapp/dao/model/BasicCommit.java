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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.usergrid.chop.api.Commit;

import java.util.Date;


/**
 * A specific commit of a Maven Module under test.
 */
public class BasicCommit implements Commit {

    private String id;
    private String moduleId;
    private String md5;
    private Date createTime;
    private String runnerJarPath;


    /**
     * @param id            Commit id string
     * @param moduleId      Id that represents the module this commit is about
     * @param md5           An md5 string calculated using commit id and timestamp of runner file creation time
     * @param createTime    Runner.jar file upload time
     * @param runnerJarPath Absolute file path of the runner.jar file
     */
    public BasicCommit( String id, String moduleId, String md5, Date createTime, String runnerJarPath ) {
        this.id = id;
        this.moduleId = moduleId;
        this.md5 = md5;
        this.createTime = createTime;
        this.runnerJarPath = runnerJarPath;
    }


    /**
     * @return  Commit id string
     */
    @Override
    public String getId() {
        return id;
    }


    /**
     * @return  An md5 string calculated using commit id and timestamp of runner file creation time
     */
    @Override
    public String getMd5() {
        return md5;
    }


    /**
     * Maven groupId, artifactId and version should be used to calculate this id.
     *
     * @return  Id that represents the module this commit is about.
     */
    @Override
    public String getModuleId() {
        return moduleId;
    }


    public void setMd5( String md5 ) {
        this.md5 = md5;
    }


    /**
     * @return  Absolute file path of the runner.jar file
     */
    @Override
    public String getRunnerPath() {
        return runnerJarPath;
    }


    public void setRunnerPath( String runnerJarPath ) {
        this.runnerJarPath = runnerJarPath;
    }


    @Override
    public Date getCreateTime() {
        return createTime;
    }


    @Override
    public int hashCode() {
        return Math.abs( id.hashCode() );
    }


    @Override
    public boolean equals( Object other ) {
        return ( other != null )
                && ( other instanceof BasicCommit )
                && ( ( BasicCommit ) other ).getId().equals( id );
    }


    @Override
    public String toString() {
        return new ToStringBuilder( this, ToStringStyle.SHORT_PREFIX_STYLE )
                .append( "id", id )
                .append( "moduleId", moduleId )
                .append( "md5", md5 )
                .append( "createTime", createTime )
                .toString();
    }


}
