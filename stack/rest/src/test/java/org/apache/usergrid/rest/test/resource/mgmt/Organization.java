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
package org.apache.usergrid.rest.test.resource.mgmt;


import java.util.UUID;

import org.apache.usergrid.rest.test.resource.EntityResource;
import org.apache.usergrid.rest.test.resource.NamedResource;
import org.apache.usergrid.rest.test.resource.app.UsersCollection;


/**
 * A resource for testing queues
 *
 * @author tnine
 */
public class Organization extends EntityResource {

    /**
     * @param entityId
     * @param parent
     */
    public Organization( UUID entityId, NamedResource parent ) {
        super( entityId, parent );
    }


    /**
     * @param entityName
     * @param parent
     */
    public Organization( String entityName, NamedResource parent ) {
        super( entityName, parent );
    }


    public ApplicationsCollection apps() {
        return new ApplicationsCollection( this );
    }


    public UsersCollection users() {
        return new UsersCollection( this );
    }
}
