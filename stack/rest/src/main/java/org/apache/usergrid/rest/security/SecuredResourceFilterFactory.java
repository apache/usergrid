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
package org.apache.usergrid.rest.security;


import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.rest.exceptions.SecurityException;
import org.apache.usergrid.rest.security.annotations.RequireAdminUserAccess;
import org.apache.usergrid.rest.security.annotations.RequireApplicationAccess;
import org.apache.usergrid.rest.security.annotations.RequireOrganizationAccess;
import org.apache.usergrid.rest.security.annotations.RequireSystemAccess;
import org.apache.usergrid.rest.utils.PathingUtils;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;
import org.apache.usergrid.services.ServiceManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.apache.usergrid.rest.exceptions.SecurityException.mappableSecurityException;
import static org.apache.usergrid.security.shiro.Realm.ROLE_SERVICE_ADMIN;
import static org.apache.usergrid.security.shiro.utils.SubjectUtils.*;


@Resource
public class SecuredResourceFilterFactory implements DynamicFeature {

    private static final Logger logger = LoggerFactory.getLogger( SecuredResourceFilterFactory.class );

    private @Context UriInfo uriInfo;

    EntityManagerFactory emf;
    ServiceManagerFactory smf;

    Properties properties;

    ManagementService management;


    @Inject
    public SecuredResourceFilterFactory() {
        logger.info( "SecuredResourceFilterFactory is installed" );
    }


    @Autowired
    public void setEntityManagerFactory( EntityManagerFactory emf ) {
        this.emf = emf;
    }


    public EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }


    @Autowired
    public void setServiceManagerFactory( ServiceManagerFactory smf ) {
        this.smf = smf;
    }


    public ServiceManagerFactory getServiceManagerFactory() {
        return smf;
    }


    @Autowired
    @Qualifier("properties")
    public void setProperties( Properties properties ) {
        this.properties = properties;
    }


    @Autowired
    public void setManagementService( ManagementService management ) {
        this.management = management;
    }


    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
        Method am = resourceInfo.getResourceMethod();

        logger.debug( "configure {} method {}",
            resourceInfo.getResourceClass().getSimpleName(), resourceInfo.getResourceMethod().getName() );

        if ( am.isAnnotationPresent( RequireApplicationAccess.class ) ) {
            featureContext.register( ApplicationFilter.class );
        }
        else if ( am.isAnnotationPresent( RequireOrganizationAccess.class ) ) {

            featureContext.register( OrganizationFilter.class );
        }
        else if ( am.isAnnotationPresent( RequireSystemAccess.class ) ) {
            featureContext.register( SystemFilter.class );
        }
        else if ( am.isAnnotationPresent( RequireAdminUserAccess.class ) ) {
            featureContext.register( SystemFilter.AdminUserFilter.class );
        }

    }

    public static abstract class AbstractFilter implements ContainerRequestFilter {

        private UriInfo uriInfo;

        public AbstractFilter( UriInfo uriInfo ) {
            this.uriInfo = uriInfo;
        }

        @Override
        public void filter(ContainerRequestContext request) throws IOException {

            logger.debug( "Filtering {}", request.getUriInfo().getRequestUri().toString() );

            if ( request.getMethod().equalsIgnoreCase( "OPTIONS" ) ) {
                logger.debug( "Skipping option request" );
            }

            MultivaluedMap<java.lang.String, java.lang.String> params = uriInfo.getPathParameters();
            logger.debug( "Params: {}", params.keySet() );

            authorize( request );
        }


        public abstract void authorize( ContainerRequestContext request );


        public Identifier getApplicationIdentifier() {
            Identifier application = null;

            MultivaluedMap<java.lang.String, java.lang.String> pathParams = uriInfo.getPathParameters();
            String applicationIdStr = pathParams.getFirst( "applicationId" );
            if ( isNotEmpty( applicationIdStr ) ) {
                application = Identifier.from( applicationIdStr );
            }
            else {
                String applicationName = PathingUtils.assembleAppName( uriInfo.getPathParameters() );
                if ( logger.isDebugEnabled() ) {
                    logger.debug( "Pulled applicationName {}", applicationName );
                }
                application = Identifier.fromName( applicationName );
            }

            return application;
        }


        public Identifier getOrganizationIdentifier() {
            Identifier organization = null;

            MultivaluedMap<java.lang.String, java.lang.String> pathParams = uriInfo.getPathParameters();
            String organizationIdStr = pathParams.getFirst( "organizationId" );
            if ( isNotEmpty( organizationIdStr ) ) {
                organization = Identifier.from( organizationIdStr );
            }
            else {
                String organizationName = pathParams.getFirst( "organizationName" );
                organization = Identifier.fromName( organizationName );
            }

            return organization;
        }


        public Identifier getUserIdentifier() {

            MultivaluedMap<java.lang.String, java.lang.String> pathParams = uriInfo.getPathParameters();
            String userIdStr = pathParams.getFirst( "userId" );
            if ( isNotEmpty( userIdStr ) ) {
                return Identifier.from( userIdStr );
            }
            String username = pathParams.getFirst( "username" );
            if ( username != null ) {
                return Identifier.fromName( username );
            }
            String email = pathParams.getFirst( "email" );
            if ( email != null ) {
                return Identifier.fromEmail( email );
            }
            return null;
        }
    }

    @Resource
    public static class OrganizationFilter extends AbstractFilter {

        @Inject
        public OrganizationFilter( UriInfo uriInfo ) {
            super(uriInfo);
        }

        @Override
        public void authorize( ContainerRequestContext request ) {
            logger.debug( "OrganizationFilter.authorize" );

            if ( !isPermittedAccessToOrganization( getOrganizationIdentifier() ) ) {
                logger.debug("No organization access authorized");
                throw mappableSecurityException( "unauthorized", "No organization access authorized" );
            }

            logger.debug( "OrganizationFilter.authorize - leaving" );
        }
    }


    @Resource
    public static class ApplicationFilter extends AbstractFilter {

        EntityManagerFactory emf;
        ManagementService management;

        @Autowired
        public void setEntityManagerFactory( EntityManagerFactory emf ) {
            this.emf = emf;
        }


        public EntityManagerFactory getEntityManagerFactory() {
            return emf;
        }

        @Autowired
        public void setManagementService( ManagementService management ) {
            this.management = management;
        }

        @Inject
        public ApplicationFilter( UriInfo uriInfo ) {
            super(uriInfo);
        }

        @Override
        public void authorize( ContainerRequestContext request ) {
            logger.debug( "ApplicationFilter.authorize" );
            if ( SubjectUtils.isAnonymous() ) {
                ApplicationInfo application = null;
                try {
                    // TODO not safe. could load arbitrary application
                    application = management.getApplicationInfo( getApplicationIdentifier() );
                }
                catch ( Exception e ) {
                    e.printStackTrace();
                }
                EntityManager em = getEntityManagerFactory().getEntityManager( application.getId() );
                Map<String, String> roles = null;
                try {
                    roles = em.getRoles();
                    logger.debug( "found roles {}", roles );
                }
                catch ( Exception e ) {
                    logger.error( "Unable retrieve roles", e );
                }
                if ( ( roles != null ) && roles.containsKey( "guest" ) ) {
                    loginApplicationGuest( application );
                }
                else {
                    throw mappableSecurityException( "unauthorized", "No application guest access authorized" );
                }
            }
            if ( !isPermittedAccessToApplication( getApplicationIdentifier() ) ) {
                throw mappableSecurityException( "unauthorized", "No application access authorized" );
            }
        }
    }


    @Resource
    public static class SystemFilter extends AbstractFilter {

        @Inject
        public SystemFilter(UriInfo uriInfo) {
            super( uriInfo );
        }


        @Override
        public void authorize(ContainerRequestContext request) {
            logger.debug( "SystemFilter.authorize" );
            try {
                if (!request.getSecurityContext().isUserInRole( ROLE_SERVICE_ADMIN )) {
                    logger.debug( "You are not the system admin." );
                    throw mappableSecurityException( "unauthorized", "No system access authorized",
                        SecurityException.REALM );
                }
            } catch (IllegalStateException e) {
                logger.debug( "This is an invalid state", e );
                if ((request.getSecurityContext().getUserPrincipal() == null) ||
                    !ROLE_SERVICE_ADMIN.equals( request.getSecurityContext().getUserPrincipal().getName() )) {
                    throw mappableSecurityException( "unauthorized", "No system access authorized",
                        SecurityException.REALM );
                }
            }
        }

        @Resource
        public static class AdminUserFilter extends AbstractFilter {

            @Inject
            public AdminUserFilter(UriInfo uriInfo) {
                super( uriInfo );
            }

            @Override
            public void authorize(ContainerRequestContext request) {
                logger.debug( "AdminUserFilter.authorize" );
                if (!isUser( getUserIdentifier() )) {
                    throw mappableSecurityException( "unauthorized", "No admin user access authorized" );
                }
            }
        }

    }
}
