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


import java.io.IOException;

import org.apache.usergrid.chop.api.BaseResult;
import org.apache.usergrid.chop.api.Project;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;


/**
 * Tests serialization / de-serialization of Project.
 */
public class ProjectSerDeserTest {
    public static final String EXAMPLE = "{\"testPackageBase\":\"org.apache.usergrid.chop.example\",\"chopVersion\":\"1.0-SNAPSHOT\"," +
            "\"createTimestamp\":\"2013.12.24.06.14.22\",\"vcsVersion\":\"3d2ccc1b2b96a45936e58b782c4c2d5f8c1ba76e\"," +
            "\"vcsRepoUrl\":\"https://jim.rybacki@stash.safehaus.org/scm/chop/main.git\"," +
            "\"groupId\":\"org.apache.usergrid.chop\",\"artifactId\":\"chop-example\",\"projectVersion\":\"1.0-SNAPSHOT\"," +
            "\"md5\":\"1f31add62c0f1da0eb5aa74d4c441e2c\"," +
            "\"loadKey\":\"tests/3d2ccc1b2b96a45936e58b782c4c2d5f8c1ba76e/runner.war\"," +
            "\"loadTime\":\"2013.12.24.06.14.24\"}";

    public static final String EMBEDDED = "{\"endpoint\":null,\"message\":null,\"status\":true,\"state\":\"READY\"," +
            "\"project\":{\"testPackageBase\":\"org.apache.usergrid.chop.example\",\"chopVersion\":\"1.0-SNAPSHOT\"," +
            "\"createTimestamp\":\"2014.01.09.03.40.37\",\"vcsVersion\":\"986d9ed1229fb2e02774b7341947516019e623af\"," +
            "\"vcsRepoUrl\":\"https://jim.rybacki@stash.safehaus.org/scm/chop/main.git\"," +
            "\"groupId\":\"org.apache.usergrid.chop\",\"artifactId\":\"chop-example\",\"projectVersion\":\"1.0-SNAPSHOT\"," +
            "\"md5\":\"b40d4527906beed4f69cd559b1cccda2\",\"loadKey\":\"tests/986d23af/runner.war\"," +
            "\"loadTime\":\"1389217238403\"}}";

    @Test
    public void testBoth() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Project value = mapper.readValue( EXAMPLE, Project.class );
        assertEquals( "org.apache.usergrid.chop.example", value.getTestPackageBase() );
        assertEquals( "1.0-SNAPSHOT", value.getChopVersion() );
        assertEquals( "2013.12.24.06.14.22", value.getCreateTimestamp() );
        assertEquals( "3d2ccc1b2b96a45936e58b782c4c2d5f8c1ba76e", value.getVcsVersion() );
        assertEquals( "org.apache.usergrid.chop", value.getGroupId() );
        assertEquals( "chop-example", value.getArtifactId() );
        assertEquals( "1.0-SNAPSHOT", value.getVersion() );
        assertEquals( "1f31add62c0f1da0eb5aa74d4c441e2c", value.getMd5() );
        assertEquals( "tests/3d2ccc1b2b96a45936e58b782c4c2d5f8c1ba76e/runner.war", value.getLoadKey() );
        assertEquals( "2013.12.24.06.14.24", value.getLoadTime() );

        String serialized = mapper.writeValueAsString( value );
        assertEquals( EXAMPLE, serialized );
    }


    @Test
    public void testEmbedded() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        BaseResult result = mapper.readValue( EMBEDDED, BaseResult.class );
        assertNotNull( result );
        assertNotNull( result.getProject() );
    }
}
