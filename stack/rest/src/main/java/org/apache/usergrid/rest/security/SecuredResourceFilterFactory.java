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


import org.apache.shiro.subject.Subject;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.rest.exceptions.SecurityException;
import org.apache.usergrid.rest.security.annotations.*;
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
import java.net.InetAddress;
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

    private static final int PRIORITY_SUPERUSER = 1;
    private static final int PRIORITY_DEFAULT = 5000;


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

        if (logger.isTraceEnabled()) {
            logger.trace("configure {} method {}",
                resourceInfo.getResourceClass().getSimpleName(), resourceInfo.getResourceMethod().getName());
        }

        boolean sysadminLocalhostOnly =
                Boolean.parseBoolean(properties.getProperty("usergrid.sysadmin.localhost.only", "false"));

        if (sysadminLocalhostOnly) {
            // priority = PRIORITY_SUPERUSER forces this to run first
            featureContext.register( SysadminLocalhostFilter.class, PRIORITY_SUPERUSER );
        }

        if ( am.isAnnotationPresent( RequireApplicationAccess.class ) ) {
            featureContext.register( ApplicationFilter.class, PRIORITY_DEFAULT);
        }
        else if ( am.isAnnotationPresent( RequireOrganizationAccess.class ) ) {
            featureContext.register( OrganizationFilter.class, PRIORITY_DEFAULT);
        }
        else if ( am.isAnnotationPresent( RequireSystemAccess.class ) ) {
            featureContext.register( SystemFilter.class, PRIORITY_DEFAULT);
        }
        else if ( am.isAnnotationPresent( RequireAdminUserAccess.class ) ) {
            featureContext.register( SystemFilter.AdminUserFilter.class, PRIORITY_DEFAULT);
        }
        else if ( am.isAnnotationPresent( CheckPermissionsForPath.class ) ) {
            featureContext.register( PathPermissionsFilter.class, PRIORITY_DEFAULT);
        }

    }

    public static abstract class AbstractFilter implements ContainerRequestFilter {

        private UriInfo uriInfo;

        public AbstractFilter( UriInfo uriInfo ) {
            this.uriInfo = uriInfo;
        }

        @Override
        public void filter(ContainerRequestContext request) throws IOException {

            if (logger.isTraceEnabled()) {
                logger.trace("Filtering {}", request.getUriInfo().getRequestUri().toString());
            }

            if ( request.getMethod().equalsIgnoreCase( "OPTIONS" ) ) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Skipping option request");
                }
            }

            MultivaluedMap<java.lang.String, java.lang.String> params = uriInfo.getPathParameters();

            if (logger.isTraceEnabled()) {
                logger.trace("Params: {}", params.keySet());
            }

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
                if ( logger.isTraceEnabled() ) {
                    logger.trace( "Pulled applicationName {}", applicationName );
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
    public static class SysadminLocalhostFilter extends AbstractFilter {

        @Inject
        public SysadminLocalhostFilter( UriInfo uriInfo ) {
            super(uriInfo);
        }

        @Override
        public void authorize( ContainerRequestContext request ) {
            if (logger.isTraceEnabled()) {
                logger.trace("SysadminLocalhostFilter.authorize");
            }

            if ( !isServiceAdmin() && !isBasicAuthServiceAdmin(request)) {
                // not a sysadmin request
                return;
            }

            boolean isLocalhost = false;
            try {
                byte[] address = InetAddress.getByName(request.getUriInfo().getBaseUri().getHost()).getAddress();
                if (address[0] == 127) {
                    // loopback address
                    isLocalhost = true;
                } else if (address[0] == 0 && address[1] == 0 && address[2] == 0 && address[3] == 0) {
                    // 0.0.0.0, used for requests like curl 0:8080
                    isLocalhost = true;
                } else {
                    // everything else
                    isLocalhost = false;
                }
            }
            catch (Exception e) {
                // couldn't parse host, so assume not localhost
                logger.error("Unable to parse host for sysadmin request, request rejected: path = {}",
                        request.getUriInfo().getPath());
            }

            if (!isLocalhost) {
                throw mappableSecurityException( "unauthorized", "No remote sysadmin access authorized" );
            }

            if (logger.isTraceEnabled()) {
                logger.trace("SysadminLocalhostFilter.authorize - leaving");
            }
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
            if (logger.isTraceEnabled()) {
                logger.trace("OrganizationFilter.authorize");
            }

            if ( !isPermittedAccessToOrganization( getOrganizationIdentifier() ) && !isBasicAuthServiceAdmin(request) ) {
                if (logger.isTraceEnabled()) {
                    logger.trace("No organization access authorized");
                }
                throw mappableSecurityException( "unauthorized", "No organization access authorized" );
            }

            if (logger.isTraceEnabled()) {
                logger.trace("OrganizationFilter.authorize - leaving");
            }
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
            if (logger.isTraceEnabled()) {
                logger.trace("ApplicationFilter.authorize");
            }
            if ( SubjectUtils.isAnonymous() ) {
                ApplicationInfo application = null;
                try {
                    // TODO not safe. could load arbitrary application
                    application = management.getApplicationInfo( getApplicationIdentifier() );
                }
                catch ( Exception e ) {
                    logger.error("Error getting applicationInfo in authorize()", e);
                }
                EntityManager em = getEntityManagerFactory().getEntityManager( application.getId() );
                Map<String, String> roles = null;
                try {
                    roles = em.getRoles();
                    if (logger.isTraceEnabled()) {
                        logger.trace("found roles {}", roles);
                    }
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
            if ( !isPermittedAccessToApplication( getApplicationIdentifier() ) && !isBasicAuthServiceAdmin(request) ) {
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
            if (logger.isTraceEnabled()) {
                logger.trace("SystemFilter.authorize");
            }
            try {
                if (!isBasicAuthServiceAdmin(request) && !isServiceAdmin()) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("You are not the system admin.");
                    }
                    throw mappableSecurityException( "unauthorized", "No system access authorized",
                        SecurityException.REALM );
                }
            } catch (IllegalStateException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("This is an invalid state", e);
                }
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
                if (logger.isTraceEnabled()) {
                    logger.trace("AdminUserFilter.authorize");
                }
                if (!isUser( getUserIdentifier() ) && !isServiceAdmin() && !isBasicAuthServiceAdmin(request) ) {
                    throw mappableSecurityException( "unauthorized", "No admin user access authorized" );
                }
            }
        }

    }

    // This filter is created in REST from logic in org.apache.usergrid.services.AbstractService.checkPermissionsForPath
    @Resource
    public static class PathPermissionsFilter extends AbstractFilter {

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
        public PathPermissionsFilter(UriInfo uriInfo) {
            super( uriInfo );
        }


        @Override
        public void authorize( ContainerRequestContext request ) {
            if(logger.isTraceEnabled()){
                logger.debug( "PathPermissionsFilter.authorize" );
            }

            final String PATH_MSG = "---- Checked permissions for path --------------------------------------------\n"
                + "Requested path: {} \n"
                + "Requested action: {} \n" + "Requested permission: {} \n"
                + "Permitted: {} \n";

            ApplicationInfo application = null;

            try {

                application = management.getApplicationInfo( getApplicationIdentifier() );
                EntityManager em = emf.getEntityManager( application.getId() );

                if ( SubjectUtils.isAnonymous() ) {
                    Map<String, String> roles = null;
                    try {
                        roles = em.getRoles();
                        if (logger.isTraceEnabled()) {
                            logger.trace("found roles {}", roles);
                        }
                    }
                    catch ( Exception e ) {
                        logger.error( "Unable to retrieve roles", e );
                    }
                    if ( ( roles != null ) && roles.containsKey( "guest" ) ) {
                        loginApplicationGuest( application );
                    }
                    else {
                        throw mappableSecurityException( "unauthorized", "No application guest access authorized" );
                    }
                }

                Subject currentUser = SubjectUtils.getSubject();

                if ( currentUser == null ) {
                    return;
                }
                String applicationName = application.getName().toLowerCase();
                String operation = request.getMethod().toLowerCase();
                String path = request.getUriInfo().getPath().toLowerCase().replace(applicationName, "");
                String perm =  getPermissionFromPath( em.getApplicationRef().getUuid(), operation, path );

                if ( "/users/me".equals( path ) && request.getMethod().equalsIgnoreCase( "get" )) {
                    // shortcut the permissions checking, the "me" end-point is always allowed
                    logger.debug("Allowing {} access to /users/me", getSubject().toString() );
                    return;
                }

                boolean permitted = currentUser.isPermitted( perm );
                if ( logger.isDebugEnabled() ) {
                    logger.debug( PATH_MSG, path, operation, perm, permitted );
                }

                SubjectUtils.checkPermission( perm );
                Subject subject = SubjectUtils.getSubject();

                if ( logger.isDebugEnabled() ) {
                    logger.debug("Checked subject {} for perm {}", subject != null ? subject.toString() : "", perm);
                    logger.debug("------------------------------------------------------------------------------");
                }

            } catch (Exception e){
                throw mappableSecurityException( "unauthorized",
                    "Subject does not have permission to access this resource" );
            }

        }
    }

    private static boolean isBasicAuthServiceAdmin(ContainerRequestContext request){

        return request.getSecurityContext().isUserInRole( ROLE_SERVICE_ADMIN );

    }


}
