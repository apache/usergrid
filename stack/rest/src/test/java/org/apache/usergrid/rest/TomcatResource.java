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


import java.io.File;

import org.junit.rules.ExternalResource;
import org.apache.usergrid.cassandra.AvailablePortFinder;

import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang.math.RandomUtils;

import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** @author tnine */
public class TomcatResource extends ExternalResource {

    private static final Logger log = LoggerFactory.getLogger( TomcatResource.class );

    public static final TomcatResource instance = new TomcatResource();
    private static Object mutex = new Object();
    private static final String CONTEXT = "/";
    private String webAppsPath;
    private int port;
    private Tomcat tomcat;

    protected TomcatResource(){
    }

    @Override
    protected void before() throws Throwable {
        if ( tomcat != null ) {
            return;
        }

        synchronized ( mutex ) {
            //second into mutex
            if ( tomcat != null ) {
                return;
            }

            File dataDir = Files.createTempDir();
            dataDir.deleteOnExit();

            port = AvailablePortFinder.getNextAvailable( 9998 + RandomUtils.nextInt(10)  );

            tomcat = new Tomcat();
            tomcat.setBaseDir( dataDir.getAbsolutePath() );
            tomcat.setPort( port );
            tomcat.addWebapp( CONTEXT, new File( getWebAppsPath() ).getAbsolutePath() );

            log.info("-----------------------------------------------------------------");
            log.info("Starting Tomcat port {} dir {}", port, dataDir.getAbsolutePath());
            log.info("-----------------------------------------------------------------");
            tomcat.start();
        }
    }


    /**
     * Get the port tomcat runs on
     * @return
     */
    public int getPort(){
        return port;
    }

    public String getWebAppsPath() {
        return webAppsPath;
    }

    public void setWebAppsPath(String webAppsPath) {
        this.webAppsPath = webAppsPath;
    }
}
