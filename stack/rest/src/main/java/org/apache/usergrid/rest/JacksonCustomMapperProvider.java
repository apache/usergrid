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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Provider
@Component
@Scope("singleton")
@Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
public class JacksonCustomMapperProvider implements ContextResolver<ObjectMapper> {

    private static final Logger logger = LoggerFactory.getLogger( JacksonCustomMapperProvider.class );

    ObjectMapper mapper = new ObjectMapper();
    

    public JacksonCustomMapperProvider() {
        logger.info( "JacksonCustomMapperProvider installed" );
        mapper.configure( SerializationFeature.INDENT_OUTPUT, true); // pretty print 
    }


    @Override
    public ObjectMapper getContext( Class<?> aClass ) {
        return mapper;
    }
}
