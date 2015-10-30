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
package org.apache.usergrid.rest.exceptions;


import org.apache.usergrid.rest.ApiResponse;

import javax.ws.rs.ext.ExceptionMapper;

import static org.apache.usergrid.utils.JsonUtils.mapToJsonString;


/**
 * <p> A runtime exception representing a failure to provide correct authentication credentials. Will result in the
 * browser presenting a password challenge if a realm is provided. </p>
 */
public class SecurityException extends RuntimeException {

    public static final String REALM = "Usergrid Authentication";

    private static final long serialVersionUID = 1L;

    private String realm = null;
    private String type = null;
    private Exception root = null;


    private SecurityException( String type, String message, String realm, Exception root ) {
        super( message );
        this.type = type;
        this.realm = realm;
        this.root = root;
    }


    public String getRealm() {
        return realm;
    }


    public String getType() {
        return type;
    }

    public Exception getRoot() {
        return root;
    }

    public String getJsonResponse() {
        ApiResponse response = new ApiResponse();
        response.setError( type, getMessage(), this );
        return mapToJsonString( response );
    }


    public static RuntimeException mappableSecurityException( AuthErrorInfo errorInfo ) {
        return mappableSecurityException( errorInfo.getType(), errorInfo.getMessage() );
    }

    public static RuntimeException mappableSecurityException( AuthErrorInfo errorInfo, String message ) {
        return mappableSecurityException( errorInfo.getType(), message );
    }

    public static RuntimeException mappableSecurityException( String type, String message ) {
        return new SecurityException( type, message, null, null );
    }

    public static RuntimeException mappableSecurityException( AuthErrorInfo errorInfo, String message, String realm ) {
        return mappableSecurityException( errorInfo.getType(), message, realm );
    }

    public static RuntimeException mappableSecurityException( String type, String message, String realm ) {
        return new SecurityException( type, message, realm, null );
    }

    public static RuntimeException mappableSecurityException( Exception e, AuthErrorInfo errorInfo ) {
        return new SecurityException( errorInfo.getType(), e.getMessage(), null, e );
    }

}
