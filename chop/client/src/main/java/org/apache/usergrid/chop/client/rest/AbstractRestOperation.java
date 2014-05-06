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
package org.apache.usergrid.chop.client.rest;


import javax.ws.rs.core.MediaType;

import org.apache.usergrid.chop.api.CoordinatorFig;
import org.apache.usergrid.chop.api.Project;
import org.apache.usergrid.chop.api.Runner;

import com.google.common.base.Preconditions;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

import static org.apache.usergrid.chop.client.rest.RestRequests.addParams;


/**
 * Abstract rest operation with boilerplate.
 */
public abstract class AbstractRestOperation<R> implements RestOperation<R> {
    private WebResource resource;
    private R result;
    private final String path;
    private final HttpOp op;



    public AbstractRestOperation( HttpOp op, WebResource resource ) {
        Preconditions.checkNotNull( op, "The 'op' MUST NOT be null." );
        Preconditions.checkNotNull( resource, "The 'resource' MUST NOT be null." );

        this.path = null;
        this.op = op;
        this.resource = resource;
    }


    public AbstractRestOperation( HttpOp op, String path, Runner runner ) {
        Preconditions.checkNotNull( op, "The 'op' MUST NOT be null." );
        Preconditions.checkNotNull( path, "The 'path' MUST NOT be null." );
        Preconditions.checkNotNull( runner, "The 'runner' MUST NOT be null." );

        this.path = path;
        this.op = op;
        this.resource = Client.create().resource( runner.getUrl() ).path( getPath() );

        resource = RestRequests.addParams(resource, runner);
    }


    public AbstractRestOperation( HttpOp op, WebResource resource, CoordinatorFig coordinator, Project project, Runner runner ) {
        Preconditions.checkNotNull( op, "The 'op' MUST NOT be null." );
        Preconditions.checkNotNull( resource, "The 'resource' MUST NOT be null." );
        Preconditions.checkNotNull( coordinator, "The 'coordinator' MUST NOT be null." );
        Preconditions.checkNotNull( runner, "The 'runner' MUST NOT be null." );
        Preconditions.checkNotNull( project, "The 'project' MUST NOT be null." );

        this.path = null;
        this.op = op;
        this.resource = resource;

        if ( runner != null ) {
            this.resource = RestRequests.addParams(resource, runner);
        }

        if ( coordinator != null ) {
            this.resource = RestRequests.addParams(resource, runner);
        }

        if ( project != null ) {
            this.resource = RestRequests.addParams(resource, project);
        }
    }


    public HttpOp getOp() {
        return op;
    }


    protected R setResult( R result ) {
        this.result = result;
        return result;
    }

    @Override
    public R getResult() {
        return result;
    }


    @Override
    public WebResource getResource() {
        return resource;
    }


    @Override
    public String getPath() {
        return path;
    }


    @Override
    public WebResource queryParameter( String key, String value ) {
        return resource = resource.queryParam( key, value );
    }


    @Override
    public R execute( Class<? extends R> clazz ) {
        switch ( op ) {

            case GET:
                return setResult( getResource().accept(
                        MediaType.APPLICATION_JSON_TYPE ).get( clazz ) );
            case POST:
                return setResult( getResource().accept(
                        MediaType.APPLICATION_JSON_TYPE ).post( clazz ) );
            case PUT:
                return setResult( getResource().accept(
                        MediaType.APPLICATION_JSON_TYPE ).put( clazz ) );
            case DELETE:
                return setResult( getResource().accept(
                        MediaType.APPLICATION_JSON_TYPE ).delete( clazz ) );
            default:
                throw new IllegalStateException( "Unknown HTTP operation type " + op );
        }
    }
}
