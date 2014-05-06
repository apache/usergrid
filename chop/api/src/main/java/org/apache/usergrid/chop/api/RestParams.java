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
 * Parameters used by REST resources.
 */
public interface RestParams {
    String CONTENT = "content";
    String FILENAME = "file";
    String RUNNER_URL = "runnerUrl";
    String RUNNER_HOSTNAME = "runnerHostname";
    String RUNNER_PORT = "runnerPort";
    String RUNNER_IPV4_ADDRESS = "runnerIpv4Address";
    String MODULE_GROUPID = "moduleGroupId";
    String MODULE_ARTIFACTID = "moduleArtifactId";
    String MODULE_VERSION = "moduleVersion";
    String COMMIT_ID = "commitId";
    String USERNAME = "user";
    String PASSWORD = "pwd";
    String TEST_CLASS = "testClass";
    String RUN_NUMBER = "runNumber";
    String RUN_ID = "runId";
    String VCS_REPO_URL = "vcsRepoUrl";
    String TEST_PACKAGE = "testPackageBase";
    String MD5 = "md5";
    String RUNNER_COUNT = "runnerCount";
}
