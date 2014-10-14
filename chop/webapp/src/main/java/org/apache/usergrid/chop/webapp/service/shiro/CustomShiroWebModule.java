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
package org.apache.usergrid.chop.webapp.service.shiro;

import com.google.inject.Key;
import com.google.inject.Singleton;
import org.apache.shiro.guice.web.ShiroWebModule;

import javax.servlet.ServletContext;

@SuppressWarnings("unchecked")
public class CustomShiroWebModule extends ShiroWebModule {

    public CustomShiroWebModule( ServletContext servletContext ) {
        super( servletContext );
    }

    @Override
    protected void configureShiroWeb() {

        addFilterChain( "/logout", LOGOUT );
        addFilterChain( "/VAADIN/*" );
        addFilterChain( "/auth/**", Key.get(RestFilter.class ) );
        bindRealm().to( ShiroRealm.class ).in( Singleton.class );

//        bindConstant().annotatedWith(Names.named("shiro.loginUrl")).to("/login.jsp");
//        bindConstant().annotatedWith(Names.named("shiro.globalSessionTimeout")).to(3600000L);//1 hour
//        bindConstant().annotatedWith(Names.named("shiro.usernameParam")).to("user");
//        bindConstant().annotatedWith(Names.named("shiro.passwordParam")).to("pass");
//        bindConstant().annotatedWith(Names.named("shiro.successUrl")).to("/index.jsp");
//        bindConstant().annotatedWith(Names.named("shiro.failureKeyAttribute")).to("shiroLoginFailure");
//        bindConstant().annotatedWith(Names.named("shiro.unauthorizedUrl")).to("/denied.jsp");
//        bindConstant().annotatedWith(Names.named("shiro.redirectUrl")).to("/login.jsp");
    }
}
