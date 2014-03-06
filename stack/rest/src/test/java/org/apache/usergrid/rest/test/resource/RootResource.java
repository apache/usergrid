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
package org.apache.usergrid.rest.test.resource;


import com.sun.jersey.api.client.WebResource;


/** @author tnine */
public class RootResource extends NamedResource {

    private WebResource resource;
    private String token;


    /**
     * @param parent
     */
    public RootResource( WebResource resource, String token ) {
        super( null );
        this.resource = resource;
        this.token = token;
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.rest.test.resource.NamedResource#resource()
     */
    @Override
    protected WebResource resource() {
        return this.resource;
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.rest.test.resource.NamedResource#token()
     */
    @Override
    protected String token() {
        return this.token;
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.rest.resource.NamedResource#addToUrl(java.lang.StringBuilder)
     */
    @Override
    public void addToUrl( StringBuilder buffer ) {
        //do nothing on purpose, callers will append "/" for the root
    }
}
