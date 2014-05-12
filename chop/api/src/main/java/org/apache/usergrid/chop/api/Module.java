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
package org.apache.usergrid.chop.api;


/**
 * This represents the Maven Module under test.
 */
public interface Module {

    /**
     * @return  A unique id calculated using all groupId, artifactId and version of this module
     */
    String getId();


    /**
     * @return  groupId field of this Maven module
     */
    String getGroupId();


    /**
     * @return  artifactId of this Maven module
     */
    String getArtifactId();


    /**
     * @return  version of this Maven module
     */
    String getVersion();


    /**
     * @return  Version control system repository's URL where this module's code is located.
     *          Corresponds to remote.origin.url for git.
     */
    String getVcsRepoUrl();


    /**
     * @return  base package in this module that all chop annotated test classes are located.
     */
    String getTestPackageBase();

    // TODO Enum for vcs type later
}
