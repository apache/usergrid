/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.tools.apidoc.swagger;


import java.util.List;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.apache.usergrid.utils.JsonUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion.NON_NULL;


public class Api {
    String path;
    String description;
    List<ApiOperation> operations;


    public Api() {
    }


    @JsonSerialize(include = NON_NULL)
    public String getPath() {
        return path;
    }


    public void setPath( String path ) {
        this.path = path;
    }


    @JsonSerialize(include = NON_NULL)
    public String getDescription() {
        return description;
    }


    public void setDescription( String description ) {
        this.description = description;
    }


    @JsonSerialize(include = NON_NULL)
    public List<ApiOperation> getOperations() {
        return operations;
    }


    public void setOperations( List<ApiOperation> operations ) {
        this.operations = operations;
    }


    @Override
    public String toString() {
        return JsonUtils.mapToJsonString( this );
    }


    public Element createWADLResource( Document doc, ApiListing listing ) {
        Element resource = doc.createElement( "resource" );
        if ( path != null ) {
            resource.setAttribute( "path", path );
        }

        if ( ( operations != null ) && !operations.isEmpty() ) {
            for ( ApiOperation operation : operations ) {
                resource.appendChild( operation.createWADLMethod( doc, this ) );
            }
        }

        return resource;
    }
}
