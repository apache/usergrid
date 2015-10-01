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
package org.apache.usergrid.rest.test;


import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.base.Optional;
import org.apache.usergrid.persistence.EntityManager;

import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.apache.usergrid.rest.AbstractContextResource;


/**
 * Refresh index of an application, FOR TESTING PURPOSES ONLY. Only works with usergrid.test=true.
 */
@Component
@Scope("prototype")
@Path("/refreshindex")
@Produces({ MediaType.APPLICATION_JSON })
public class RefreshIndexResource extends AbstractContextResource {
    static final Logger logger = LoggerFactory.getLogger( RefreshIndexResource.class );

    public RefreshIndexResource() {}

    @POST
    public Response refresh(
            @QueryParam("org_name") String orgName,
            @QueryParam("app_name") String appName,
            @QueryParam("app_id") String appIdString ) throws IOException, Exception {

        try {

            // only works in test mode
            Properties props = management.getProperties();
            String testProp = ( String ) props.get( "usergrid.test" );
            if ( testProp == null || !Boolean.parseBoolean( testProp ) ) {
                throw new UnsupportedOperationException();
            }

            UUID appid = UUIDUtils.tryExtractUUID(appIdString);
            if(appid == null){
                throw new IllegalArgumentException("app id is null");
            }
            // refresh the system apps or app lookup below may fail
            EntityManager em = this.getEmf().getEntityManager(appid);
            em.refreshIndex();

        } catch (Exception e) {
            logger.error("Error in refresh", e);
            return Response.serverError().build();
        }

        return Response.ok().build();
    }
}
