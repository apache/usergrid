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

package org.apache.usergrid.rest.test.resource.endpoints.mgmt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.usergrid.rest.test.resource.endpoints.NamedResource;
import org.apache.usergrid.rest.test.resource.endpoints.UrlResource;
import org.apache.usergrid.rest.test.resource.state.ClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringReader;


/**
 * Management end-point for getting list of applications in organization.
 */
public class ApplicationsResource extends NamedResource {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationsResource.class);
    ObjectMapper mapper = new ObjectMapper();

    public ApplicationsResource( final ClientContext context, final UrlResource parent ) {
        super( "apps", context, parent );
    }

    public ManagementResponse getOrganizationApplications() throws IOException {

        String responseString = this.getTarget()
            .queryParam( "access_token", context.getToken().getAccessToken() )
            .request()
            .get(String.class);

        logger.info("Response: " + responseString);

        return mapper.readValue(
            new StringReader(responseString), ManagementResponse.class);
    }



    public ApplicationResource app( final String appName ){
        return new ApplicationResource( appName,context,this );
    }

}
