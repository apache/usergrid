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
package org.apache.usergrid.rest.test.resource.endpoints;


import org.apache.usergrid.rest.test.resource.state.ClientContext;

import javax.ws.rs.client.WebTarget;

/**
 * Interface that returns the path that is currently being pointed to.
 */
public interface UrlResource {

    /**
     * Get the url path to this resource
     * example: http://localhost:8080/management/orgs/<org_name>
     * @return
     */
    public String getPath();

    /**
     * Get the resource
     * @return
     */
    public WebTarget getTarget();

    /**
     * get context
     * @return
     */
    public ClientContext getContext();


}
