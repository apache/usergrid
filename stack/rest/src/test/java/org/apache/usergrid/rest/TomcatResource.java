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

import com.google.common.io.Files;
import java.io.File;

import org.junit.rules.ExternalResource;
import org.apache.usergrid.cassandra.AvailablePortFinder;

import org.apache.commons.lang.math.RandomUtils;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.ServletException;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * Simple singleton to get tests running again. A temporary solution until we start using
 * Arquillian
 *
 */
public class TomcatResource  {
    private static final Logger log = LoggerFactory.getLogger(TomcatResource.class);

    public static TomcatResource instance;


    private Tomcat tomcat = null;
    private int port;



    public static synchronized TomcatResource getInstance(){
      if(instance == null){
          instance = new TomcatResource();
      }

        return instance;
    }

    private TomcatResource(){
        try {
            startTomcatEmbedded();
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Unable to delete tomcat" );
        }
    }

    private void waitForTomcat() throws RuntimeException {
        String url = "http://localhost:" + port + "/status";
        int count = 0;
        while (count++ < 30) {
            try {
                Thread.sleep(1000);
                Client c = Client.create();
                WebResource wr = c.resource( url );
                wr.get( String.class );
                log.info("Tomcat is started.");
                return;

            } catch (Exception e) {
                log.info("Waiting for Tomcat on url {}", url);
            }
        }


        throw new RuntimeException("Tomcat process never started.");

    }


    private void startTomcatEmbedded() throws ServletException, LifecycleException {

        File dataDir = Files.createTempDir();
        dataDir.deleteOnExit();

        port = AvailablePortFinder.getNextAvailable( 9998 + RandomUtils.nextInt( 10 ) );


        tomcat = new Tomcat();
        tomcat.setBaseDir( dataDir.getAbsolutePath() );
        tomcat.setPort( port );
        tomcat.getConnector().setAttribute( "maxThreads", "2000" );
        tomcat.addWebapp( "/", new File( "target/ROOT" ).getAbsolutePath() );

        log.info( "-----------------------------------------------------------------" );
        log.info( "Starting Tomcat embedded port {} dir {}", port, dataDir.getAbsolutePath() );
        log.info( "-----------------------------------------------------------------" );
        tomcat.start();

        waitForTomcat();
    }


    /**
     * Get the port Tomcat runs on.
     */
    public int getPort() {
        return port;
    }

}
