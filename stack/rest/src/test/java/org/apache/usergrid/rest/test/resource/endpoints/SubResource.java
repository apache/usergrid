/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.rest.test.resource.endpoints;

import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.test.resource.state.ClientContext;

/**
 * Classy class class.
 */
public class SubResource extends NamedResource {

    private SubResource(final String path, final ClientContext context, final UrlResource parent) {
        super( path, context, parent );
    }

    public static NamedResource getInstance(final String path, final UrlResource parent ){
        return new SubResource(path,parent.getContext(),parent);
    }
}
