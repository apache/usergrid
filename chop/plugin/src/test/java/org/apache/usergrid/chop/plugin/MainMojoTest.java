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
package org.apache.usergrid.chop.plugin;


import org.apache.usergrid.chop.plugin.Utils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.maven.plugin.MojoExecutionException;


public class MainMojoTest {
    @Test
    @Ignore
    public void testGetS3Client() {

    }


    @Test
    @Ignore
    public void testExecute() {

    }


    @Test
    @Ignore
    public void testExtractWar() {

    }


    @Test
    @Ignore
    public void testGetGitRemoteUrl() {

    }


    @Test
    @Ignore
    public void testIsCommitNecessary() {

    }


    @Test
    public void testGetMD5() {
        String lastCommitUUID = "6807ae52eb67cf7c1a8d44e1ca291e23845595c7";
        String timestamp = "2013.12.08.01.52.51";

        try {
            String md5 = Utils.getMD5(timestamp, lastCommitUUID);
            Assert.assertEquals( "MD5 generator is not working properly", md5, "81a28bb20ca1f89ed41ce5c861fb7222" );
        }
        catch ( MojoExecutionException e ) {
            Assert.fail( e.getMessage() );
        }
    }
}