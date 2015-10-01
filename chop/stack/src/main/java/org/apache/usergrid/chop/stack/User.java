/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.stack;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;


public class User {

    private String username;
    private String password;


    public User( String username, String password ) {
        this.username = username;
        this.password = password;
    }


    public String getUsername() {
        return username;
    }


    public String getPassword() {
        return password;
    }


    public void setPassword( String password ) {
        this.password = password;
    }


    @Override
    public String toString() {
        return new ToStringBuilder( this, ToStringStyle.SHORT_PREFIX_STYLE )
                .append( "username", username )
                .toString();
    }


    @Override
    public boolean equals( final Object obj ) {
        if( this == obj ) {
            return true;
        }
        return obj != null &&
                obj instanceof User &&
                ( ( User ) obj ).username.equals( this.username );
    }


    @Override
    public int hashCode() {
        return Math.abs( new HashCodeBuilder().append( username ).toHashCode() );
    }
}
