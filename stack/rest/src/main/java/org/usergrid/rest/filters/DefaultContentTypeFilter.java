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
package org.usergrid.rest.filters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import scala.util.parsing.json.JSONType;

import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.core.header.MediaTypes;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

/**
 * @author tnine
 * 
 */
public class DefaultContentTypeFilter implements ContainerRequestFilter {

    //non whitespace/non white space [possible whitespace][possible comma] ;
    private static final Pattern REPLACE_TYPES = Pattern.compile("(\\S+/\\S+\\s*,?)+;?.*");

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.sun.jersey.spi.container.ContainerRequestFilter#filter(com.sun.jersey
     * .spi.container.ContainerRequest)
     */
    @Override
    public ContainerRequest filter(ContainerRequest request) {

        String value = request.getHeaderValue(HttpHeaders.ACCEPT);
       
        if(value != null && !value.contains(MediaType.APPLICATION_JSON)){   
            request.getRequestHeaders().putSingle(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        }

       

        return request;
    }
}
