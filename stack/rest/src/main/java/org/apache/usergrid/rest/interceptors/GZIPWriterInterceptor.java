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

package org.apache.usergrid.rest.interceptors;

import org.glassfish.jersey.server.ContainerRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.inject.*;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.*;
import javax.ws.rs.ext.Provider;

/**
 * If the request had an ACCEPT_ENCODING header containing 'gzip' then
 * gzip the response and add CONTENT_ENCODING gzip header
 *
 * * If the request had an CONTENT_ENCODING header containing 'gzip' then
 *  unzip the request and remove the CONTENT_ENCODING gzip header
 *  Created by peterajohnson on 11/1/17.
 */
@Provider
public class GZIPWriterInterceptor implements ReaderInterceptor, WriterInterceptor {

    final private static String GZIP = "gzip";
    @Inject
    private javax.inject.Provider<ContainerRequest> requestProvider;

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException,WebApplicationException {
        ContainerRequest request = requestProvider.get();

        if (request != null) {
            List<String> aeHeaders = request.getRequestHeader(HttpHeaders.ACCEPT_ENCODING);
            if (aeHeaders != null && aeHeaders.size() > 0) {
                String acceptEncodingHeader = aeHeaders.get(0);
                if (acceptEncodingHeader.contains(GZIP)) {
                    OutputStream outputStream = context.getOutputStream();
                    context.setOutputStream(new GZIPOutputStream(outputStream));
                    context.getHeaders().putSingle(HttpHeaders.CONTENT_ENCODING, GZIP);
                }
            }
        }
        context.proceed();
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        String encoding = context.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
        if (GZIP.equalsIgnoreCase(encoding)) {
            GZIPInputStream is = new GZIPInputStream(context.getInputStream());
            context.getHeaders().remove(HttpHeaders.CONTENT_ENCODING);
            context.setInputStream(is);
        }

        return context.proceed();
    }
}
