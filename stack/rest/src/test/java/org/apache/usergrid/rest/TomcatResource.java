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
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.usergrid.cassandra.AvailablePortFinder;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.servlet.ServletException;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Start and stop embedded Tomcat.
 */
public class TomcatResource extends ExternalResource {
    private static final Logger log = LoggerFactory.getLogger(TomcatResource.class);

    public static final TomcatResource instance = new TomcatResource();

    private static final Object mutex = new Object();
    private String webAppsPath = "src/main/webapp";
    private int port;
    private boolean started = false;

    private static AtomicInteger clientCount = new AtomicInteger(0);

    Tomcat tomcat = null;
    Process process = null;


    public TomcatResource() {
        try {
            String[] locations = { "usergrid-properties-context.xml" };
            ConfigurableApplicationContext appContext =
                new ClassPathXmlApplicationContext( locations );

        } catch (Exception ex) {
            throw new RuntimeException("Error getting properties", ex);
        }
    }


    @Override
    protected void after() {
        log.info("Entering after");

        synchronized (mutex) {

            if ( clientCount.decrementAndGet() < 1 ) {

                log.info("----------------------------------------------------------------------");
                log.info("Destroying Tomcat running on port " + port);
                log.info("----------------------------------------------------------------------");

                if ( process != null ) {
                    process.destroy();

                } else if ( tomcat != null ) {
                    try {
                        tomcat.stop();
                        tomcat.destroy();
                    } catch (LifecycleException ex) {
                        log.error("Error stopping Tomcat", ex);
                    }
                }
                started = false;

            } else {
                log.info("NOT stopping Tomcat because it is still in use by {}", clientCount.get());
            }
        }

        log.info("Leaving after");
    }


    @Override
    protected void before() throws Throwable {
        log.info("Entering before");

        synchronized (mutex) {

            clientCount.incrementAndGet();

            if (started) {
                log.info("NOT starting Tomcat because it is already started");
                log.info("Leaving before: {} users of Tomcat", clientCount.get());
                return;
            }

            startTomcatEmbedded();

            log.info("Leaving before: Started Tomcat, now {} users", clientCount.get());
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
                wr.get(String.class);
                log.info("Tomcat is started.");
                started = true;
                break;

            } catch (Exception e) {
                log.info("Waiting for Tomcat on url {}", url);
            }
        }
        if ( !started ) {
            throw new RuntimeException("Tomcat process never started.");
        }
    }


    private void startTomcatEmbedded() throws ServletException, LifecycleException {

        File dataDir = Files.createTempDir();
        dataDir.deleteOnExit();

        port = AvailablePortFinder.getNextAvailable( 9998 + RandomUtils.nextInt(10)  );

        tomcat = new Tomcat();
        tomcat.setBaseDir( dataDir.getAbsolutePath() );
        tomcat.setPort( port );
        tomcat.getConnector().setAttribute("maxThreads", "2000");
        tomcat.addWebapp( "/", new File( getWebAppsPath() ).getAbsolutePath() );

        log.info("-----------------------------------------------------------------");
        log.info("Starting Tomcat embedded port {} dir {}", port, dataDir.getAbsolutePath());
        log.info("-----------------------------------------------------------------");
        tomcat.start();

        waitForTomcat();

        mutex.notifyAll();
    }

    public int getPort() {
        return port;
    }

    public String getWebAppsPath() {
        return webAppsPath;
    }

    public void setWebAppsPath(String webAppsPath) {
        this.webAppsPath = webAppsPath;
    }

}
