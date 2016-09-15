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
import org.apache.usergrid.rest.security.annotations.CheckPermissionsForPath;
import org.apache.usergrid.rest.security.annotations.RequireApplicationAccess;
import org.apache.usergrid.rest.utils.CertificateUtils;
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
import java.util.Map;

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
    @CheckPermissionsForPath
    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @JSONP
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @Override
    public ApiResponse executeMultiPartPut(@Context UriInfo ui,
        @QueryParam("callback") @DefaultValue("callback") String callback,
        FormDataMultiPart multiPart) throws Exception {

        if (logger.isTraceEnabled()) {
            logger.trace("NotifierResource.executeMultiPartPut");
        }

        String certInfoParam = getValueOrNull(multiPart, "certInfo");
        if (certInfoParam != null){
            throw new IllegalArgumentException("Cannot create or update with certInfo parameter.  It is derived.");
        }

        String name =         getValueOrNull(multiPart, "name");
        String provider =     getValueOrNull(multiPart, "provider");
        String certPassword = getValueOrNull(multiPart, "certificatePassword");

        InputStream is = null;
        Map<String, Object> certAttributes = null;
        String filename = null;
        byte[] certBytes = null;
        if (multiPart.getField("p12Certificate") != null) {
            filename = multiPart.getField("p12Certificate").getContentDisposition().getFileName();
            is = multiPart.getField("p12Certificate").getEntityAs(InputStream.class);
            if (is != null) {
                certBytes = IOUtils.toByteArray(is);
                certAttributes = CertificateUtils.getCertAtrributes(certBytes, certPassword);
            }
        }else{
            throw new IllegalArgumentException("Certificate is invalid .p12 file or incorrect certificatePassword");
        }

        // check to see if the certificate is valid
        if(!CertificateUtils.isValid(certAttributes)){
            throw new IllegalArgumentException("p12Certificate is expired");
        }

        HashMap<String, Object> certProps = new LinkedHashMap<String, Object>();
        certProps.put("name", name);
        certProps.put("provider", provider);
        certProps.put("environment", "production");
        certProps.put("certificatePassword", certPassword);

        if(certBytes != null && certBytes.length > 0 ){
            certProps.put("p12Certificate", certBytes);
        }
        HashMap<String, Object> certInfo = new LinkedHashMap<String, Object>();
        if (certAttributes != null){
            certInfo.put("filename", filename);
            certInfo.put("details", certAttributes);
        }
        certProps.put("certInfo", certInfo);


        ApiResponse response = createApiResponse();
        response.setAction("put");
        response.setApplication(services.getApplication());
        response.setParams(ui.getQueryParameters());
        ServicePayload payload = getPayload(certProps);
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
