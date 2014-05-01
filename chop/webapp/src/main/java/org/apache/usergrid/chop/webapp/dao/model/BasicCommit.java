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

public class BasicCommit implements Commit {

    private String id;
    private String moduleId;
    private String md5;
    private Date createTime;
    private String runnerWarPath;


    public BasicCommit(String id, String moduleId, String md5, Date createTime, String runnerWarPath) {
        this.id = id;
        this.moduleId = moduleId;
        this.md5 = md5;
        this.createTime = createTime;
        this.runnerWarPath = runnerWarPath;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getModuleId() {
        return moduleId;
    }

    @Override
    public String getMd5() {
        return md5;
    }


    public void setMd5() {
        this.md5 = md5;
    }


    @Override
    public String getRunnerPath() {
        return runnerWarPath;
    }


    public void setRunnerPath(String runnerWarPath) {
        this.runnerWarPath = runnerWarPath;
    }


    @Override
    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other != null
                && other instanceof BasicCommit
                && ((BasicCommit) other).getId().equals(id);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", id)
                .append("moduleId", moduleId)
                .append("md5", md5)
                .append("createTime", createTime)
                .toString();
    }


}
