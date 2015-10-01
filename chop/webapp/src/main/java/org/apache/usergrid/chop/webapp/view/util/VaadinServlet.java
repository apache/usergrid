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
package org.apache.usergrid.chop.webapp.view.util;

import com.google.inject.Singleton;

import org.apache.usergrid.chop.webapp.view.main.Login;
import org.apache.usergrid.chop.webapp.view.main.MainView;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Enumeration;
import java.util.Hashtable;

@Singleton
public class VaadinServlet extends com.vaadin.server.VaadinServlet {

    private static final Hashtable<String, String> PARAMS = getInitParams();

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {

        // Disable Vaadin debug mode
        servletConfig.getServletContext().setInitParameter("productionMode", "true");

        super.init(getServletConfig(servletConfig));
    }

    private static Hashtable<String, String> getInitParams() {

        Hashtable<String, String> ht = new Hashtable<String, String>();
        ht.put("UI", Login.class.getName());

        return ht;
    }

    private static ServletConfig getServletConfig(final ServletConfig servletConfig) {
        return new ServletConfig() {
            @Override
            public String getServletName() {
                return servletConfig.getServletName();
            }

            @Override
            public ServletContext getServletContext() {
                return servletConfig.getServletContext();
            }

            @Override
            public String getInitParameter(String s) {
                return PARAMS.get(s);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return PARAMS.keys();
            }
        };
    }
}