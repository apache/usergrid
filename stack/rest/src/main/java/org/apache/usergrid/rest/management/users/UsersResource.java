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


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import net.tanesha.recaptcha.ReCaptchaException;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.management.exceptions.ManagementException;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Identifier;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.rest.RootResource;
import org.apache.usergrid.rest.management.ManagementResource;
import org.apache.usergrid.rest.security.annotations.RequireSystemAccess;
import org.apache.usergrid.services.exceptions.ServiceResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.rest.AbstractContextResource;
import org.apache.usergrid.rest.ApiResponse;
import org.apache.usergrid.rest.exceptions.AuthErrorInfo;
import org.apache.usergrid.rest.exceptions.RedirectionException;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;

import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.api.view.Viewable;

import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.usergrid.rest.exceptions.SecurityException.mappableSecurityException;
import static org.apache.usergrid.utils.ConversionUtils.string;


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
        logger.info( "ManagementUsersResource initialized" );
    }


    @Path(RootResource.USER_ID_PATH)
    public UserResource getUserById( @Context UriInfo ui, @PathParam( "userId" ) String userIdStr ) throws Exception {

        return getUserResource(management.getAdminUserByUuid(UUID.fromString(userIdStr)), "user id", userIdStr);
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
            throw new ManagementException("Could not find organization for " + type + " : " + value);
        }
        return getSubResource(UserResource.class).init(user);
    }


    @Path(RootResource.EMAIL_PATH)
    public UserResource getUserByEmail( @Context UriInfo ui, @PathParam( "email" ) String email ) throws Exception {

        return getUserResource(management.getAdminUserByEmail( email ), "email", email);
    }

    @PUT
    @RequireSystemAccess
    @Path(RootResource.EMAIL_PATH+"/override")
    public JSONWithPadding setUserInfo( @Context UriInfo ui, Map<String, Object> json,
                                        @PathParam ("email") String emailFromPath,
                                        @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
            throws Exception {

        if ( json == null ) {
            return null;
        }

        String emailFromBody = string( json.remove( "email" ) );

        if(emailFromBody == null){
            throw new IllegalArgumentException( "Email json body parameter required." );
        }

        if(!emailFromBody.equalsIgnoreCase(emailFromPath)){
            throw new IllegalArgumentException( "Email provided in path must match email provided in body" );
        }

        String username = string( json.remove( "username" ) );
        String name = string( json.remove( "name" ) );

        EntityManager em = emf.getEntityManager(CassandraService.MANAGEMENT_APPLICATION_ID);
        // Use the override method as it also checks for email in Unique Index instead of only Entity Index
        EntityRef entity = em.getUserByIdentifierOverride(Identifier.fromEmail(emailFromPath));
        if(entity == null){
            entity = em.getUserByIdentifier(Identifier.fromName(emailFromPath));
        }
        if(entity == null){
            logger.error("Entity id parameter {} with {} value does not exist", "email", emailFromPath);
            throw new IllegalArgumentException("Entity not found.");
        }

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("email", emailFromPath);
        properties.put("username", username);
        properties.put("name", name);
        properties.put("uuid", entity.getUuid());
        properties.put("confirmed", true);
        properties.put("activated", true);



        // Re-create the user entity with the existing UUID found in the index
        em.create(entity.getUuid(), User.ENTITY_TYPE, properties);


        ApiResponse response = createApiResponse();
        response.setAction( "update user info" );

        return new JSONWithPadding( response, callback );
    }


    @POST
    @Consumes( MediaType.APPLICATION_FORM_URLENCODED )
    public JSONWithPadding createUser( @Context UriInfo ui, @FormParam( "username" ) String username,
                                       @FormParam( "name" ) String name, @FormParam( "email" ) String email,
                                       @FormParam( "password" ) String password,
                                       @QueryParam( "callback" ) @DefaultValue( "callback" ) String callback )
            throws Exception {

        final boolean externalTokensEnabled =
                !StringUtils.isEmpty( properties.getProperty( ManagementResource.USERGRID_CENTRAL_URL ) );

        if ( externalTokensEnabled ) {
            throw new IllegalArgumentException( "Admin Users must signup via " +
                    properties.getProperty( ManagementResource.USERGRID_CENTRAL_URL ) );
        }

        logger.info( "Create user: " + username );

        ApiResponse response = createApiResponse();
        response.setAction( "create user" );

        UserInfo user = management.createAdminUser( username, name, email, password, false, false );
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        if ( user != null ) {
            result.put( "user", user );
            response.setData( result );
            response.setSuccess();
        }
        else {
            throw mappableSecurityException( AuthErrorInfo.BAD_CREDENTIALS_SYNTAX_ERROR );
        }

        return new JSONWithPadding( response, callback );
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
    @Path("resetpw")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.TEXT_HTML)
    public Viewable handlePasswordResetForm( @Context UriInfo ui, @FormParam("email") String email,
                                             @FormParam("recaptcha_challenge_field") String challenge,
                                             @FormParam("recaptcha_response_field") String uresponse ) {

        try {
            if ( isBlank(email) ) {
                errorMsg = "No email provided, try again...";
                throw new Exception("No email provided");
            }else if (useReCaptcha()){
                ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
                reCaptcha.setPrivateKey( properties.getRecaptchaPrivate() );

                ReCaptchaResponse reCaptchaResponse =
                    reCaptcha.checkAnswer( httpServletRequest.getRemoteAddr(), challenge, uresponse );

                if(!reCaptchaResponse.isValid()){
                    errorMsg = "Incorrect Captcha, try again...";
                    throw new Exception("reCAPTCHA error message: "+reCaptchaResponse.getErrorMessage());
                }
            }
            user = management.findAdminUser(email);

            if (user == null) {
                errorMsg = "We don't recognize that email, try again...";
                throw new Exception("Unrecognized email address");
            }
            logger.info("Starting Admin User Password Reset Flow for "+user.getUuid());
            management.startAdminUserPasswordResetFlow(user);
            return handleViewable("resetpw_email_success", this);
        }
        catch ( RedirectionException e ) {
            throw e;
        }
        catch ( Exception e ) {
            logger.error(String.format("Exception in password reset form. (%s) %s ", e.getClass().getCanonicalName(), e.getMessage()));
            return handleViewable( "resetpw_email_form", this );
        }
    }

    public String getErrorMsg() {
        return errorMsg;
    }


    public UserInfo getUser() {
        return user;
    }
}
