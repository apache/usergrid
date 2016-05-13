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
package org.apache.usergrid.java.client;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.usergrid.java.client.UsergridEnums.UsergridHttpMethod;
import org.apache.usergrid.java.client.auth.UsergridAuth;
import org.apache.usergrid.java.client.query.UsergridQuery;
import org.apache.usergrid.java.client.utils.JsonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@SuppressWarnings("unused")
public class UsergridRequest {
    @NotNull public static final MediaType APPLICATION_JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    @NotNull private UsergridHttpMethod method;
    @NotNull private String baseUrl;
    @NotNull private MediaType contentType;

    @Nullable private UsergridQuery query;
    @Nullable private Map<String, Object> headers;
    @Nullable private Map<String, Object> parameters;
    @Nullable private Object data;
    @Nullable private UsergridAuth auth;
    @Nullable private String[] pathSegments;

    @NotNull
    public UsergridHttpMethod getMethod() { return method; }
    public void setMethod(@NotNull final UsergridHttpMethod method) { this.method = method; }

    @NotNull
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(@NotNull final String baseUrl) { this.baseUrl = baseUrl; }

    @NotNull
    public MediaType getContentType() { return contentType; }
    public void setContentType(@NotNull final MediaType contentType) { this.contentType = contentType; }

    @Nullable
    public UsergridQuery getQuery() { return query; }
    public void setQuery(@Nullable final UsergridQuery query) { this.query = query; }

    @Nullable
    public Map<String,Object> getHeaders() { return headers; }
    public void setHeaders(@Nullable final Map<String,Object> headers) { this.headers = headers; }

    @Nullable
    public Map<String,Object> getParameters() { return parameters; }
    public void setParameters(@Nullable final Map<String,Object> parameters) { this.parameters = parameters; }

    @Nullable
    public Object getData() { return data; }
    public void setData(@Nullable final Object data) { this.data = data; }

    @Nullable
    public UsergridAuth getAuth() { return auth; }
    public void setAuth(@Nullable final UsergridAuth auth) { this.auth = auth; }

    @Nullable
    public String[] getPathSegments() { return pathSegments; }
    public void setPathSegments(@Nullable final String[] pathSegments) { this.pathSegments = pathSegments; }

    private UsergridRequest() {}

    public UsergridRequest(@NotNull final UsergridHttpMethod method,
                           @NotNull final MediaType contentType,
                           @NotNull final String url,
                           @Nullable final UsergridQuery query,
                           @Nullable final UsergridAuth auth,
                           @Nullable final String... pathSegments) {
        this.method = method;
        this.contentType = contentType;
        this.baseUrl = url;
        this.query = query;
        this.auth = auth;
        this.pathSegments = pathSegments;
    }

    public UsergridRequest(@NotNull final UsergridHttpMethod method,
                           @NotNull final MediaType contentType,
                           @NotNull final String url,
                           @Nullable final UsergridAuth auth,
                           @Nullable final String... pathSegments) {
        this.method = method;
        this.contentType = contentType;
        this.baseUrl = url;
        this.auth = auth;
        this.pathSegments = pathSegments;
    }

    public UsergridRequest(@NotNull final UsergridHttpMethod method,
                           @NotNull final MediaType contentType,
                           @NotNull final String url,
                           @Nullable final Map<String, Object> params,
                           @Nullable final Object data,
                           @Nullable final UsergridAuth auth,
                           @Nullable final String... pathSegments) {
        this.method = method;
        this.contentType = contentType;
        this.baseUrl = url;
        this.parameters = params;
        this.data = data;
        this.headers = null;
        this.query = null;
        this.auth = auth;
        this.pathSegments = pathSegments;
    }

    public UsergridRequest(@NotNull final UsergridHttpMethod method,
                           @NotNull final MediaType contentType,
                           @NotNull final String url,
                           @Nullable final Map<String, Object> params,
                           @Nullable final Object data,
                           @Nullable final Map<String, Object> headers,
                           @Nullable final UsergridQuery query,
                           @Nullable final UsergridAuth auth,
                           @Nullable final String... pathSegments) {
        this.method = method;
        this.contentType = contentType;
        this.baseUrl = url;
        this.parameters = params;
        this.data = data;
        this.headers = headers;
        this.query = query;
        this.auth = auth;
        this.pathSegments = pathSegments;
    }

    @NotNull
    public Request buildRequest() {
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(this.constructUrl());
        this.addHeaders(requestBuilder);
        requestBuilder.method(this.method.toString(),this.constructRequestBody());
        return requestBuilder.build();
    }

    @NotNull
    protected HttpUrl constructUrl() {
        String url = this.baseUrl;
        if( this.query != null ) {
            url += this.query.build(false);
        }
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if( this.pathSegments != null ) {
            for( String path : this.pathSegments ) {
                urlBuilder.addPathSegments(path);
            }
        }
        if( this.parameters != null ) {
            for (Map.Entry<String, Object> param : this.parameters.entrySet()) {
                urlBuilder.addQueryParameter(param.getKey(),param.getValue().toString());
            }
        }
        return urlBuilder.build();
    }

    protected void addHeaders(@NotNull final Request.Builder requestBuilder) {
        requestBuilder.addHeader("User-Agent", UsergridRequestManager.USERGRID_USER_AGENT);
        if (this.auth != null ) {
            String accessToken = this.auth.getAccessToken();
            if( accessToken != null ) {
                requestBuilder.addHeader("Authorization", "Bearer " + accessToken);
            }
        }
        if( this.headers != null ) {
            for( Map.Entry<String,Object> header : this.headers.entrySet() ) {
                requestBuilder.addHeader(header.getKey(),header.getValue().toString());
            }
        }
    }

    @Nullable
    protected RequestBody constructRequestBody() {
        RequestBody requestBody = null;
        if (method == UsergridHttpMethod.POST || method == UsergridHttpMethod.PUT) {
            String jsonString = "";
            if( this.data != null ) {
                jsonString = JsonUtils.toJsonString(this.data);
            }
            requestBody = RequestBody.create(this.contentType,jsonString);
        }
        return requestBody;
    }
}
