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
package org.apache.usergrid.rest.management.users;


import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.management.exceptions.ManagementException;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.RootResource;
import org.apache.usergrid.rest.exceptions.AuthErrorInfo;
import org.apache.usergrid.rest.exceptions.RedirectionException;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;
import org.apache.usergrid.security.tokens.cassandra.TokenServiceImpl;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.usergrid.rest.exceptions.SecurityException.mappableSecurityException;
import static org.apache.usergrid.security.tokens.cassandra.TokenServiceImpl.USERGRID_EXTERNAL_SSO_PROVIDER_URL;


@Component( "org.apache.usergrid.rest.management.users.UsersResource" )
@Produces( {
        MediaType.APPLICATION_JSON, "application/javascript", "application/x-javascript", "text/ecmascript",
        "application/ecmascript", "text/jscript"
} )
public class UsersResource extends AbstractContextResource {

    private static final Logger logger = LoggerFactory.getLogger( UsersResource.class );

    String errorMsg;
    UserInfo user;


    public UsersResource() {
        if (logger.isTraceEnabled()) {
            logger.trace("ManagementUsersResource initialized");
        }
    }


    @Path(RootResource.USER_ID_PATH)
    public UserResource getUserById( @Context UriInfo ui, @PathParam( "userId" ) String userIdStr ) throws Exception {

        return getUserResource(management.getAdminUserByUuid( UUID.fromString( userIdStr ) ), "user id", userIdStr);
    }


    @Path( "{username}" )
    public UserResource getUserByUsername( @Context UriInfo ui, @PathParam( "username" ) String username )
            throws Exception {

        if ( "me".equals( username ) ) {
            UserInfo user = SubjectUtils.getAdminUser();
            if ( ( user != null ) && ( user.getUuid() != null ) ) {
                return getSubResource( UserResource.class ).init( management.getAdminUserByUuid( user.getUuid() ) );
            }
            throw mappableSecurityException( "unauthorized", "No admin identity for access credentials provided" );
        }

        return getUserResource(management.getAdminUserByUsername(username), "username", username);
    }

    private UserResource getUserResource(UserInfo user, String type, String value) throws ManagementException {
        if (user == null) {
            throw new ManagementException("Could not find user for " + type + ": " + value);
        }
        return getSubResource(UserResource.class).init( user );
    }


    @Path(RootResource.EMAIL_PATH)
    public UserResource getUserByEmail( @Context UriInfo ui, @PathParam( "email" ) String email ) throws Exception {

        return getUserResource(management.getAdminUserByEmail( email ), "email", email);
    }


    @POST
    @Consumes( MediaType.APPLICATION_FORM_URLENCODED )
    @JSONP
    @Produces( {MediaType.APPLICATION_JSON, "application/javascript" })
    public ApiResponse createUser( @Context UriInfo ui, @FormParam( "username" ) String username,
                                       @FormParam( "name" ) String name, @FormParam( "email" ) String email,
                                       @FormParam( "password" ) String password,
                                       @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
            throws Exception {

        if ( tokens.isExternalSSOProviderEnabled() ) {
            throw new IllegalArgumentException(  "External SSO integration is enabled, admin users registering without an org" +
                " must do so via provider: "+ properties.getProperty(TokenServiceImpl.USERGRID_EXTERNAL_SSO_PROVIDER) );
        }

        // email is only required parameter
        if (StringUtils.isBlank(email)) {
            throw new IllegalArgumentException( "email form parameter is required" );
        }

        if ( "me".equals( username ) ) {
            throw new IllegalArgumentException( "Username 'me' is reserved" );
        }

        // if username not provided, email will be used
        logger.info( "Create user: {}", (StringUtils.isNotBlank(username) ? username : email) );

        ApiResponse response = createApiResponse();
        response.setAction( "create user" );


        UserInfo user = null;
        if ( tokens.isExternalSSOProviderEnabled() ){
            //autoactivating user, since the activation is done via the external sso provider.
            user = management.createAdminUser(null,username,name,email,password,true,false);
        }
        else {
            user = management.createAdminUser(null, username, name, email, password, false, false);
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if ( user != null ) {
            result.put( "user", user );
            response.setData( result );
            response.setSuccess();
        }
        else {
            throw mappableSecurityException( AuthErrorInfo.BAD_CREDENTIALS_SYNTAX_ERROR );
        }

        return response;
    }

	/*
     * @POST
	 *
	 * @Consumes(MediaType.MULTIPART_FORM_DATA) public JSONWithPadding
	 * createUserFromMultipart(@Context UriInfo ui,
	 *
	 * @FormDataParam("username") String username,
	 *
	 * @FormDataParam("name") String name,
	 *
	 * @FormDataParam("email") String email,
	 *
	 * @FormDataParam("password") String password) throws Exception {
	 *
	 * return createUser(ui, username, name, email, password); }
	 */


    @GET
    @Path( "resetpw" )
    @Produces( MediaType.TEXT_HTML )
    public Viewable showPasswordResetForm( @Context UriInfo ui ) {
        return handleViewable( "resetpw_email_form", this );
    }


    @POST
    @Path( "resetpw" )
    @Consumes( "application/x-www-form-urlencoded" )
    @Produces( MediaType.TEXT_HTML )
    public Viewable handlePasswordResetForm( @Context UriInfo ui, @FormParam( "email" ) String email,
                                             @FormParam( "recaptcha_challenge_field" ) String challenge,
                                             @FormParam( "recaptcha_response_field" ) String uresponse ) {

        try {
            if ( isBlank( email ) ) {
                errorMsg = "No email provided, try again...";
                return handleViewable( "resetpw_email_form", this );
            }

            //we don't require recaptcha - only use it if it is present in the props file
            boolean reCaptchaPassed = false;
            if ( useReCaptcha() ) {

                ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
                reCaptcha.setPrivateKey(properties.getRecaptchaPrivate());

                ReCaptchaResponse reCaptchaResponse =
                        reCaptcha.checkAnswer(httpServletRequest.getRemoteAddr(), challenge, uresponse);

                if (reCaptchaResponse.isValid()) {
                    reCaptchaPassed = true;
                }
            } else {
                reCaptchaPassed = true;
            }

            if (reCaptchaPassed) {
                user = management.findAdminUser(email);
                if (user != null) {
                    management.startAdminUserPasswordResetFlow(null, user);
                    return handleViewable("resetpw_email_success", this);
                } else {
                    errorMsg = "We don't recognize that email, try again...";
                    return handleViewable("resetpw_email_form", this);
                }
            } else {
                errorMsg = "Incorrect Captcha, try again...";
                return handleViewable("resetpw_email_form", this);
            }

        }
        catch ( RedirectionException e ) {
            throw e;
        }
        catch ( Exception e ) {
            return handleViewable( "error", e );
        }
    }


    public String getErrorMsg() {
        return errorMsg;
    }


    public UserInfo getUser() {
        return user;
    }
}
