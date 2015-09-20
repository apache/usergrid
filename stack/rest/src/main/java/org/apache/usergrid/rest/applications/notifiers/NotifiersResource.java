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
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.rest.AbstractContextResource;
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
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

import static org.apache.usergrid.services.ServiceParameter.addParameter;

@Component("org.apache.usergrid.rest.applications.notifiers.NotifiersResource")
@Scope("prototype")
@Produces(MediaType.APPLICATION_JSON)
public class NotifiersResource extends ServiceResource {

    private static final Logger logger = LoggerFactory
            .getLogger(NotifiersResource.class);

    @Override
    @Path("{entityId: [A-Fa-f0-9]{8}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{4}-[A-Fa-f0-9]{12}}")
    public AbstractContextResource addIdParameter(@Context UriInfo ui,
            @PathParam("entityId") PathSegment entityId) throws Exception {

        logger.info("NotifiersResource.addIdParameter");

        UUID itemId = UUID.fromString(entityId.getPath());

        addParameter(getServiceParameters(), itemId);

        addMatrixParams(getServiceParameters(), ui, entityId);

        return getSubResource(NotifierResource.class).init(  Identifier.fromUUID(itemId));
    }

    @Override
    @Path("{itemName}")
    public AbstractContextResource addNameParameter(@Context UriInfo ui,
            @PathParam("itemName") PathSegment itemName) throws Exception {

        logger.info("NotifiersResource.addNameParameter");

        logger.info("Current segment is " + itemName.getPath());

        if (itemName.getPath().startsWith("{")) {
            Query query = Query.fromJsonString(itemName.getPath());
            if (query != null) {
                addParameter(getServiceParameters(), query);
            }
            addMatrixParams(getServiceParameters(), ui, itemName);

            return getSubResource(ServiceResource.class);
        }

        addParameter(getServiceParameters(), itemName.getPath());

        addMatrixParams(getServiceParameters(), ui, itemName);
        Identifier id = Identifier.from(itemName.getPath());
        if (id == null) {
            throw new IllegalArgumentException(
                    "Not a valid Notifier identifier: " + itemName.getPath());
        }
        return getSubResource(NotifierResource.class).init(id);
    }

    /* Multipart POST create with uploaded p12Certificate */
    @POST
    @RequireApplicationAccess
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Override
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public ApiResponse executeMultiPartPost(
            @Context UriInfo ui,
            @QueryParam("callback") @DefaultValue("callback") String callback,
            FormDataMultiPart multiPart)
            throws Exception {

        logger.debug("ServiceResource.uploadData");

        String name =         getValueOrNull(multiPart, "name");
        String provider =     getValueOrNull(multiPart, "provider");
        String environment =  getValueOrNull(multiPart, "environment");
        String certPassword = getValueOrNull(multiPart, "certificatePassword");

        InputStream is = null;
        if (multiPart.getField("p12Certificate") != null) {
            is = multiPart.getField("p12Certificate").getEntityAs(InputStream.class);
        }


        HashMap<String, Object> certProps = new LinkedHashMap<String, Object>();
        certProps.put("name", name);
        certProps.put("provider", provider);
        certProps.put("environment", environment);
        certProps.put("certificatePassword", certPassword);
        if (is != null) {
            byte[] certBytes = IOUtils.toByteArray(is);
            certProps.put("p12Certificate", certBytes);
        }

        ApiResponse response = createApiResponse();
        response.setAction("post");
        response.setApplication(services.getApplication());
        response.setParams(ui.getQueryParameters());
        ServicePayload payload = getPayload(certProps);
        executeServiceRequest(ui, response, ServiceAction.POST, payload);

        return response;
    }

    private String getValueOrNull(FormDataMultiPart multiPart, String name) {
        if (multiPart.getField(name) != null) {
            return multiPart.getField(name).getValue();
        }
        return null;
    }
}
