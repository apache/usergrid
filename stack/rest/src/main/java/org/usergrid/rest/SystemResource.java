/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.rest;

import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.usergrid.rest.security.annotations.RequireSystemAccess;

import com.sun.jersey.api.json.JSONWithPadding;

@Path("/system")
@Component
@Scope("singleton")
@Produces({ MediaType.APPLICATION_JSON, "application/javascript",
        "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript" })
public class SystemResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory
            .getLogger(SystemResource.class);

    public SystemResource() {
        logger.info("SystemResource initialized");
    }

    @RequireSystemAccess
    @GET
    @Path("database/setup")
    public JSONWithPadding getSetup(@Context UriInfo ui,
            @QueryParam("callback") @DefaultValue("callback") String callback)
            throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction("cassandra setup");

        logger.info("Setting up Cassandra");

        Map<String, String> properties = emf.getServiceProperties();
        if (properties != null) {
            response.setError("System properties are initialized, database is set up already.");
            return new JSONWithPadding(response, callback);
        }

        try {
            emf.setup();
        } catch (Exception e) {
            logger.error(
                    "Unable to complete core database setup, possibly due to it being setup already",
                    e);
        }

        try {
            management.setup();
        } catch (Exception e) {
            logger.error(
                    "Unable to complete management database setup, possibly due to it being setup already",
                    e);
        }

        response.setSuccess();

        return new JSONWithPadding(response, callback);
    }

    @RequireSystemAccess
    @GET
    @Path("hello")
    public JSONWithPadding hello(@Context UriInfo ui,
            @QueryParam("callback") @DefaultValue("callback") String callback)
            throws Exception {
        logger.info("Saying hello");

        ApiResponse response = createApiResponse();
        response.setAction("Greetings Professor Falken");
        response.setSuccess();

        return new JSONWithPadding(response, callback);
    }

    @RequireSystemAccess
    @GET
    @Path("superuser/setup")
    public JSONWithPadding getSetupSuperuser(@Context UriInfo ui,
            @QueryParam("callback") @DefaultValue("callback") String callback)
            throws Exception {

        ApiResponse response = createApiResponse();
        response.setAction("superuser setup");

        logger.info("Setting up Superuser");

        try {
            management.provisionSuperuser();
        } catch (Exception e) {
            logger.error("Unable to complete superuser setup", e);
        }

        response.setSuccess();

        return new JSONWithPadding(response, callback);
    }

}
