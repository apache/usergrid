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
package org.apache.usergrid.rest.security.shiro.session;


import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.EnumerationUtils;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.Session;


/**
 * Session that is only tied to an HttpServletRequest. This can be used for applications that prefer to remain
 * stateless.
 */
public class HttpServletRequestSession implements Session {
    private final HttpServletRequest request;
    private final String host;
    private final UUID uuid;
    private final Date start;


    public HttpServletRequestSession( HttpServletRequest request, String host ) {
        this.request = request;
        this.host = host;
        uuid = UUID.randomUUID();
        start = new Date();
    }


    @Override
    public Serializable getId() {
        return uuid;
    }


    @Override
    public Date getStartTimestamp() {
        return start;
    }


    @Override
    public Date getLastAccessTime() {
        // the user only makes one request that involves this session
        return start;
    }


    @Override
    public long getTimeout() throws InvalidSessionException {
        return -1;
    }


    @Override
    public void setTimeout( long maxIdleTimeInMillis ) throws InvalidSessionException {
        // ignore this - the session ends with the request and that's that...
    }


    @Override
    public String getHost() {
        return host;
    }


    @Override
    public void touch() throws InvalidSessionException {
        // do nothing - we don't timeout
    }


    @Override
    public void stop() throws InvalidSessionException {
        // do nothing - i don't have a use case for this and the structure to
        // support it, while not huge, adds
        // significant complexity
    }


    @SuppressWarnings({ "unchecked" })
    @Override
    public Collection<Object> getAttributeKeys() throws InvalidSessionException {
        return EnumerationUtils.toList( request.getAttributeNames() );
    }


    @Override
    public Object getAttribute( Object key ) throws InvalidSessionException {
        return request.getAttribute( stringify( key ) );
    }


    @Override
    public void setAttribute( Object key, Object value ) throws InvalidSessionException {
        request.setAttribute( stringify( key ), value );
    }


    @Override
    public Object removeAttribute( Object objectKey ) throws InvalidSessionException {
        String key = stringify( objectKey );
        Object formerValue = request.getAttribute( key );
        request.removeAttribute( key );
        return formerValue;
    }


    private String stringify( Object key ) {
        return key == null ? null : key.toString();
    }
}
