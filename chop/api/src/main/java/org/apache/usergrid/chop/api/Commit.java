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


import java.util.Date;


/**
 * A specific commit of a Maven Module under test.
 */
public interface Commit {


    /**
     * @return  commit id string
     */
    String getId();


    /**
     * Maven groupId, artifactId and version should be used to calculate this id.
     *
     * @return  Id that represents the module this commit is about.
     */
    String getModuleId();


    /**
     * @return  An md5 string calculated using commit id and timestamp of runner file creation time
     */
    String getMd5();


    /**
     * @return  Absolute file path of the runner.jar file
     */
    String getRunnerPath();


    /**
     * @return  Runner file creation time
     */
    Date getCreateTime();
}
