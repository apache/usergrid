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

public class Note {

    private String id;
    private String commitId;
    private int runNumber;
    private String text;

    public Note(String commitId, int runNumber, String text) {
        this.id = createId(commitId, runNumber);
        this.commitId = commitId;
        this.runNumber = runNumber;
        this.text = text;
    }

    private static String createId(String commitId, int runNumber) {
        return "" + new HashCodeBuilder()
                .append(commitId)
                .append(runNumber)
                .toHashCode();
    }

    public String getId() {
        return id;
    }

    public String getCommitId() {
        return commitId;
    }

    public int getRunNumber() {
        return runNumber;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("id", id)
                .append("commitId", commitId)
                .append("runNumber", runNumber)
                .append("text", text)
                .toString();
    }
}
