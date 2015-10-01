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
package org.apache.usergrid.rest.security.shiro.filters;


import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.security.tokens.TokenService;
import org.apache.usergrid.services.ServiceManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import javax.xml.ws.spi.http.HttpContext;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.usergrid.utils.StringUtils.stringOrSubstringAfterFirst;
import static org.apache.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;

public abstract class SecurityFilter implements ContainerRequestFilter {

    public static final String AUTH_OAUTH_2_ACCESS_TOKEN_TYPE = "BEARER";
    public static final String AUTH_BASIC_TYPE = "BASIC";
    public static final String AUTH_OAUTH_1_TYPE = "OAUTH";

    EntityManagerFactory emf;
    ServiceManagerFactory smf;
    Properties properties;
    ManagementService management;
    TokenService tokens;

    @Context
    UriInfo uriInfo;

    @Context
    HttpContext hc;


    public EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }


    @Autowired
    public void setEntityManagerFactory( EntityManagerFactory emf ) {
        this.emf = emf;
    }


    public ServiceManagerFactory getServiceManagerFactory() {
        return smf;
    }


    @Autowired
    public void setServiceManagerFactory( ServiceManagerFactory smf ) {
        this.smf = smf;
    }


    public Properties getProperties() {
        return properties;
    }


    @Autowired
    @Qualifier("properties")
    public void setProperties( Properties properties ) {
        this.properties = properties;
    }


    public TokenService getTokenService() {
        return tokens;
    }


    @Autowired
    public void setTokenService( TokenService tokens ) {
        this.tokens = tokens;
    }


    public ManagementService getManagementService() {
        return management;
    }


    @Autowired
    public void setManagementService( ManagementService management ) {
        this.management = management;
    }


    public static Map<String, String> getAuthTypes( ContainerRequestContext request ) {
        String auth_header = request.getHeaderString( HttpHeaders.AUTHORIZATION );
        if ( auth_header == null ) {
            return null;
        }

        String[] auth_list = StringUtils.split( auth_header, ',' );
        if ( auth_list == null ) {
            return null;
        }
        Map<String, String> auth_types = new LinkedHashMap<String, String>();
        for ( String auth : auth_list ) {
            auth = auth.trim();
            String type = stringOrSubstringBeforeFirst( auth, ' ' ).toUpperCase();
            String token = stringOrSubstringAfterFirst( auth, ' ' );
            auth_types.put( type, token );
        }
        return auth_types;
    }
}
