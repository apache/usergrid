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
package org.apache.usergrid.rest;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import net.tanesha.recaptcha.ReCaptcha;
import net.tanesha.recaptcha.ReCaptchaFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.mq.QueueManagerFactory;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.rest.exceptions.RedirectionException;
import org.apache.usergrid.security.tokens.TokenService;
import org.apache.usergrid.services.ServiceManagerFactory;
import org.glassfish.jersey.server.CloseableService;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.*;
import javax.xml.ws.spi.http.HttpContext;
import java.io.IOException;
import java.util.List;
import java.util.Map;


public abstract class AbstractContextResource {

    protected static final TypeReference<Map<String, Object>> mapTypeReference = new TypeReference<Map<String, Object>>() {
    };
    protected static final TypeReference<List<Object>> listTypeReference = new TypeReference<List<Object>>() {
    };
    protected static final ObjectMapper mapper = new ObjectMapper();


    protected AbstractContextResource parent;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected Request request;

    @Context
    protected SecurityContext sc;

    @Context
    protected HttpContext hc;

    @Context
    protected CloseableService cs;

    @Context
    protected HttpServletRequest httpServletRequest;

    @Context
    protected ResourceContext resourceContext;

    @Autowired
    protected EntityManagerFactory emf;

    @Autowired
    protected ServiceManagerFactory smf;

    @Autowired
    protected ManagementService management;

    @Autowired
    protected ServerEnvironmentProperties properties;

    @Autowired
    protected QueueManagerFactory qmf;

    @Autowired
    protected Injector injector;

    @Autowired
    protected TokenService tokens;

    private static final Logger logger = LoggerFactory.getLogger(AbstractContextResource.class);


    public AbstractContextResource() {
    }


    public AbstractContextResource getParent() {
        return parent;
    }


    public void setParent(AbstractContextResource parent) {
        this.parent = parent;
    }


    public <T extends AbstractContextResource> T getSubResource(Class<T> t) {
        logger.debug("getSubResource: " + t.getCanonicalName());
        T subResource = resourceContext.getResource(t);
        subResource.setParent(this);
        return subResource;
    }


    public PathSegment getFirstPathSegment(String name) {
        if (name == null) {
            return null;
        }
        List<PathSegment> segments = uriInfo.getPathSegments();
        for (PathSegment segment : segments) {
            if (name.equals(segment.getPath())) {
                return segment;
            }
        }
        return null;
    }


    public boolean useReCaptcha() {
        return StringUtils.isNotBlank(properties.getRecaptchaPublic())
            && StringUtils.isNotBlank(properties.getRecaptchaPrivate());
    }


    public String getReCaptchaHtml() {
        if (!useReCaptcha()) {
            return "";
        }
        ReCaptcha c = ReCaptchaFactory.newSecureReCaptcha(
            properties.getRecaptchaPublic(), properties.getRecaptchaPrivate(), false);
        return c.createRecaptchaHtml(null, null);
    }


    public void sendRedirect(String location) {
        if (StringUtils.isNotBlank(location)) {
            throw new RedirectionException(location);
        }
    }


    public Viewable handleViewable(String template, Object model) {

        String className = this.getClass().getName().toLowerCase();
        String packageName = AbstractContextResource.class.getPackage().getName();

        String template_property = "usergrid.view" +
            StringUtils.removeEnd(className.toLowerCase(), "resource")
                .substring(packageName.length()) + "." + template.toLowerCase();

        String redirect_url = properties.getProperty(template_property);

        if (StringUtils.isNotBlank(redirect_url)) {
            logger.debug("Redirecting to URL: ", redirect_url);
            sendRedirect(redirect_url);
        }
        logger.debug("Dispatching to viewable with template: {}",
            template, template_property);

        Viewable viewable = new Viewable(template, model);
        return viewable;
    }


    protected ApiResponse createApiResponse() {
        return new ApiResponse(properties);
    }

    protected EntityManagerFactory getEmf(){
        return emf;
    }
    /**
     * Next three new methods necessary to work around inexplicable problems with EntityHolder.
     * This problem happens consistently when you deploy "two-dot-o" to Tomcat:
     * https://groups.google.com/forum/#!topic/usergrid/yyAJdmsBfig
     */
    protected Object readJsonToObject(String content) throws IOException {

        JsonNode jsonNode = mapper.readTree(content);
        Object jsonObject;
        if (jsonNode.isArray()) {
            jsonObject = mapper.readValue(content, listTypeReference);
        } else {
            jsonObject = mapper.readValue(content, mapTypeReference);
        }
        return jsonObject;
    }
}
