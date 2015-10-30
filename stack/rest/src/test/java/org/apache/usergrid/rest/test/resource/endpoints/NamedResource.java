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


import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.apache.usergrid.rest.test.resource.model.ApiResponse;
import org.apache.usergrid.rest.test.resource.model.Entity;
import org.apache.usergrid.rest.test.resource.model.QueryParameters;
import org.apache.usergrid.rest.test.resource.model.Token;
import org.apache.usergrid.rest.test.resource.state.ClientContext;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * Base class that is extended by named endpoints.
 * The NamedResource stores the parent of the class, the context in which the class operates and then Name of this resource
 */
public class NamedResource implements UrlResource {

    public static String SLASH = "/";

    protected final String name;
    protected final ClientContext context;

    /**
     * Stores the path of the parent that called it. i.e If we had a ApplicationResource
     * (an instance of a namedResource) this would contain the OrganizationResource.
     */
    protected final UrlResource parent;

    public NamedResource(final String name, final ClientContext context, final UrlResource parent) {
        this.name = name;
        this.context = context;
        this.parent = parent;
    }

    @Override
    public String getPath() {
        return name + getMatrix();
    }

    @Override
    public WebTarget getTarget() {
        return getTarget(false);
    }

    @Override
    public ClientContext getContext() {
        return context;
    }

    public WebTarget getTarget(boolean useToken) {
        return getTarget(useToken, null);
    }

    public WebTarget getTarget(boolean useToken, Token token) {
        WebTarget resource = parent.getTarget().path(getPath());
        token = token != null ? token : this.context.getToken();
        //error checking
        if (token == null) {
            return resource;
        }
        return useToken ? resource.queryParam("access_token", token.getAccessToken()) : resource;
    }

    protected WebTarget addParametersToResource(WebTarget resource, final QueryParameters parameters) {

        if (parameters == null) {
            return resource;
        }
        if (parameters.getQuery() != null) {
            resource = resource.queryParam("ql", parameters.getQuery());
        }

        if (parameters.getCursor() != null) {
            resource = resource.queryParam("cursor", parameters.getCursor());
        }

        if (parameters.getStart() != null) {
            resource = resource.queryParam("start", parameters.getStart().toString());
        }

        if (parameters.getLimit() != null) {
            resource = resource.queryParam("limit", parameters.getLimit().toString());
        }

        if (parameters.getConnections() != null) {
            resource = resource.queryParam("connections", parameters.getConnections());
        }
        //We can also post the params as queries
        if (parameters.getFormPostData().size() > 0) {
            Map<String, String> formData = parameters.getFormPostData();
            Set<String> keySet = formData.keySet();
            Iterator<String> keyIterator = keySet.iterator();


            while (keyIterator.hasNext()) {
                String key = keyIterator.next();
                String value = formData.get(key);
                resource = resource.queryParam(key, value);
            }
        }
        return resource;
    }

    protected String getMatrixValue(final QueryParameters parameters) {

        StringBuilder sb = new StringBuilder();
        if (parameters == null) {
            return null;
        }
        if (parameters.getQuery() != null) {
            sb.append(";");
            sb.append("ql").append("=").append(parameters.getQuery());
        }

        if (parameters.getCursor() != null) {
            sb.append(";");
            sb.append("cursor").append("=").append(parameters.getCursor());
        }
        if (parameters.getStart() != null) {
            sb.append(";");
            sb.append("start").append("=").append(parameters.getStart());
        }
        if (parameters.getLimit() != null) {
            sb.append(";");
            sb.append("limit").append("=").append(parameters.getLimit());
        }
        //We can also post the params as queries
        if (parameters.getFormPostData().size() > 0) {
            Map<String, String> formData = parameters.getFormPostData();
            Set<String> keySet = formData.keySet();
            Iterator<String> keyIterator = keySet.iterator();


            while (keyIterator.hasNext()) {
                if (sb.length() > 0)
                    sb.append(";");
                String key = keyIterator.next();
                String value = formData.get(key);
                sb.append(key).append("=").append(value);
            }
        }
        return sb.toString();
    }

    /**
     * Need to refactor all instances of tokens to either be passed in or manually set during the test.
     * There isn't any reason we would want a rest forwarding framework to set something on behave of the user.
     *
     * @param map
     * @return
     */
    //For edge cases like Organizations and Tokens
    public ApiResponse post(Map map) {
        return post(true, ApiResponse.class, map, null, false);

    }

    //For edge cases like Organizations and Tokens
    public ApiResponse post(boolean useToken, Map map, QueryParameters queryParameters) {
        return post(useToken, ApiResponse.class, map, queryParameters, false);

    }

    /**
     * Need to refactor all instances of tokens to either be passed in or manually set during the test.
     * There isn't any reason we would want a rest forwarding framework to set something on behave of the user.
     * For edge cases like Organizations and Tokens
     */
    public <T> T post(Class<T> type) {
        return post(true, type, null, null, false);

    }

    /**
     * Need to refactor all instances of tokens to either be passed in or manually set during the test.
     * There isn't any reason we would want a rest forwarding framework to set something on behave of the user.
     * For edge cases like Organizations and Tokens.
     */
    public <T> T post(Class<T> type, Entity requestEntity) {
        return post(true, type, requestEntity, null, false);

    }

    /**
     * Need to refactor all instances of tokens to either be passed in or manually set during the test.
     * There isn't any reason we would want a rest forwarding framework to set something on behave of the user.
     * For edge cases like Organizations and Tokens
     */
    public <T> T post(Class<T> type, Map requestEntity) {
        return post(true, type, requestEntity, null, false);

    }

    public <T> T post(boolean useToken, Class<T> type, Map requestEntity) {
        return post(useToken, type, requestEntity, null, false);

    }

    /**
     * Used to test POST using form payloads.
     */
    public <T> T post(Class<T> type, Form form) {
        GenericType<T> gt = new GenericType<>((Class) type);
        return getTarget().request()
            .accept(MediaType.APPLICATION_JSON)
            .post(javax.ws.rs.client.Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), gt);
    }


    // Used for empty posts
    public <T> T post(boolean useToken, Class<T> type, Map entity, final QueryParameters queryParameters) {
        WebTarget resource = getTarget(useToken);
        resource = addParametersToResource(resource, queryParameters);

        Invocation.Builder builder = resource.request()
            .accept(MediaType.APPLICATION_JSON);

        // it's OK for the entity to be null
        GenericType<T> gt = new GenericType<>((Class) type);
        return builder.post(javax.ws.rs.client.Entity.json(entity), gt);
    }

    // Used for empty posts
    public <T> T post(boolean useToken, Class<T> type, Map entity,
                      final QueryParameters queryParameters, boolean useBasicAuthentication) {

        WebTarget resource = getTarget(useToken);
        resource = addParametersToResource(resource, queryParameters);

        GenericType<T> gt = new GenericType<>((Class) type);

        if (useBasicAuthentication) {
            HttpAuthenticationFeature feature = HttpAuthenticationFeature.basicBuilder()
                .credentials("superuser", "superpassword").build();
            return resource.register(feature).request()
                .accept(MediaType.APPLICATION_JSON)
                .post(javax.ws.rs.client.Entity.json(entity), gt);
        }

        return resource.request()
            .accept(MediaType.APPLICATION_JSON)
            .post(javax.ws.rs.client.Entity.json(entity), gt);
    }

    //For edge cases like Organizations and Tokens without any payload
    public <T> T get(Class<T> type) {
        return get(type, null, true);

    }

    //For edge cases like Organizations and Tokens without any payload
    public <T> T get(Class<T> type, boolean useToken) {
        return get(type, null, useToken);
    }


    public <T> T get(Class<T> type, QueryParameters queryParameters) {
        return get(type, queryParameters, true);
    }

    public <T> T get(Class<T> type, QueryParameters queryParameters, boolean useToken) {

        WebTarget resource = getTarget(useToken);
        if (queryParameters != null) {
            resource = addParametersToResource(resource, queryParameters);
        }

        GenericType<T> gt = new GenericType<>((Class) type);
        return resource.request()
            .accept(MediaType.APPLICATION_JSON)
            .get(gt);
    }

    public String getMatrix() {
        return "";
    }

    public ApiResponse post(boolean useToken, MultiPart multiPartForm) {
        WebTarget resource = getTarget(useToken);
        return resource.request().post(
            javax.ws.rs.client.Entity.entity(multiPartForm, multiPartForm.getMediaType()), ApiResponse.class);
    }

    public ApiResponse post(MultiPart multiPartForm) {
        return post(true, multiPartForm);
    }

    public ApiResponse put(boolean useToken, byte[] data, MediaType type) {
        WebTarget resource = getTarget(useToken);
        return resource.request().put(
            javax.ws.rs.client.Entity.entity(data, type), ApiResponse.class);
    }

    public ApiResponse put(byte[] data, MediaType type) {
        return put(true, data, type);
    }

    public ApiResponse put(boolean useToken, FormDataMultiPart multiPartForm) {
        WebTarget resource = getTarget(useToken);
        return resource.request().put(
            javax.ws.rs.client.Entity.entity(multiPartForm, multiPartForm.getMediaType()), ApiResponse.class);
    }

    public ApiResponse put(FormDataMultiPart multiPartForm) {
        return put(true, multiPartForm);
    }

    public InputStream getAssetAsStream(boolean useToken) {
        WebTarget resource = getTarget(useToken);
        return resource.request().accept(MediaType.APPLICATION_OCTET_STREAM_TYPE).get(InputStream.class);
    }

    public InputStream getAssetAsStream() {
        return getAssetAsStream(true);
    }

    public ApiResponse delete() {
        return delete(true);
    }

    public ApiResponse delete(boolean useToken) {
        return getTarget(useToken).request().delete(ApiResponse.class);
    }

    public ApiResponse delete(boolean useToken, QueryParameters queryParameters) {
        WebTarget resource = getTarget(useToken);

        if (queryParameters != null) {
            resource = addParametersToResource(resource, queryParameters);
        }
        return resource.request().delete(ApiResponse.class);
    }

    public NamedResource getSubResource(String path) {
        return new NamedResource(path,context,this);
    }
}
