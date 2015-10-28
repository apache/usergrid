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

import org.apache.usergrid.persistence.exceptions.DuplicateUniquePropertyExistsException;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;


@Provider
public class DuplicateUniquePropertyExistsExceptionMapper
        extends AbstractExceptionMapper<DuplicateUniquePropertyExistsException> {

    //when you get this exception then fire a repair task that will grab a thread from a thread pool and async run the repair task
    // That task will then go through and double checkk all the entities and verify that their ref's and entities match up.


    //npe, delete entity ref
    //mismatched pointers, update the entity ref with whatever is in the collection. ( retrieve via query ) .
    @Override
    public Response toResponse( DuplicateUniquePropertyExistsException e ) {
        return toResponse( BAD_REQUEST, e );
    }
}
