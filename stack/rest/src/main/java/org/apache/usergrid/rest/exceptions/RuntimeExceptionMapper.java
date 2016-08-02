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
package org.apache.usergrid.rest.exceptions;


import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;


@Provider
public class RuntimeExceptionMapper extends AbstractExceptionMapper<RuntimeException> {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeExceptionMapper.class);

    @Override
    public Response toResponse( RuntimeException e ) {

        if(logger.isTraceEnabled()) {
            logger.trace("Error during runtime processing", e.getMessage());
        }

        return toResponse( INTERNAL_SERVER_ERROR, e );
    }
}
