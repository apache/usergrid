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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;


/**
 * Custom serializer for ProjectFigs.
 */
public class ProjectSerializer extends JsonSerializer<Project> {

    @Override
    public void serialize( final Project value, final JsonGenerator jgen, final SerializerProvider provider )
            throws IOException {
        jgen.writeStartObject();

        jgen.writeStringField( "testPackageBase", value.getTestPackageBase() );

        jgen.writeStringField( "chopVersion", value.getChopVersion() );

        jgen.writeStringField( "createTimestamp", value.getCreateTimestamp() );

        jgen.writeStringField( "vcsVersion", value.getVcsVersion() );

        jgen.writeStringField( "vcsRepoUrl", value.getVcsRepoUrl() );

        jgen.writeStringField( "groupId", value.getGroupId() );

        jgen.writeStringField( "artifactId", value.getArtifactId() );

        jgen.writeStringField( "projectVersion", value.getVersion() );

        jgen.writeStringField( "md5", value.getMd5() );

        jgen.writeStringField( "loadKey", value.getLoadKey() );

        jgen.writeStringField( "loadTime", value.getLoadTime() );

        jgen.writeEndObject();
    }
}
