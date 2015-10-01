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
package org.apache.usergrid.rest.applications.notifiers;

import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import org.apache.commons.io.IOUtils;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.applications.ServiceResource;
import org.apache.usergrid.rest.security.annotations.RequireApplicationAccess;
import org.apache.usergrid.services.ServiceAction;
import org.apache.usergrid.services.ServicePayload;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;

@Component("org.apache.usergrid.rest.applications.notifiers.NotifierResource")
@Scope("prototype")
@Produces(MediaType.APPLICATION_JSON)
public class NotifierResource extends ServiceResource {

    private static final Logger logger = LoggerFactory.getLogger(NotifierResource.class);

    private Identifier identifier;

    public NotifierResource init(Identifier identifier) throws Exception {
        this.identifier = identifier;
        return this;
    }

    /* Multipart PUT update with uploaded p12Certificate */
    @PUT
    @RequireApplicationAccess
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Override
    public ApiResponse executeMultiPartPut(@Context UriInfo ui,
        @QueryParam("callback") @DefaultValue("callback") String callback,
        FormDataMultiPart multiPart) throws Exception {

        logger.debug("NotifierResource.executePut");

        String name =         getValueOrNull(multiPart, "name");
        String provider =     getValueOrNull(multiPart, "provider");
        String certPassword = getValueOrNull(multiPart, "certificatePassword");

        InputStream is = null;
        if (multiPart.getField("p12Certificate") != null) {
            is = multiPart.getField("p12Certificate").getEntityAs(InputStream.class);
        }

        HashMap<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("name", name);
        properties.put("provider", provider);
        properties.put("environment", "production");
        properties.put("certificatePassword", certPassword);
        if (is != null) {
            byte[] certBytes = IOUtils.toByteArray(is);
            properties.put("p12Certificate", certBytes);
        }

        ApiResponse response = createApiResponse();
        response.setAction("put");
        response.setApplication(services.getApplication());
        response.setParams(ui.getQueryParameters());
        ServicePayload payload = getPayload(properties);
        executeServiceRequest(ui, response, ServiceAction.PUT, payload);

        return response;
    }

    private String getValueOrNull(FormDataMultiPart multiPart, String name) {
        if (multiPart.getField(name) != null) {
            return multiPart.getField(name).getValue();
        }
        return null;
    }

}
