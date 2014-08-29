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
 * Setup Tomcat to run Usergrid from src/main/webapps plus inherited (Maven) classpath. 
 * Caller must provide Cassandra and ElasticSearch ports. 
 * Generates Usergrid properties files.
 */
public class TomcatResource extends ExternalResource {
    private static final Logger log = LoggerFactory.getLogger(TomcatResource.class);

    public static final TomcatResource instance = new TomcatResource();

    private static final Object mutex = new Object();
    private String webAppsPath;
    private int port;
    private int esPort;
    private int cassPort;
    private boolean started = false;
    private Properties properties;
    private boolean forkTomcat = true;

    private static AtomicInteger clientCount = new AtomicInteger(0);

    Tomcat tomcat = null;
    Process process = null;


    protected TomcatResource() {
        try {
            String[] locations = { "usergrid-properties-context.xml" };
            ConfigurableApplicationContext appContext = 
                    new ClassPathXmlApplicationContext( locations );
            
            properties = (Properties)appContext.getBean("properties");

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

        String esStartup = properties.getProperty("tomcat.startup");
        if ( "forked".equals( esStartup )) {
            forkTomcat = true;
        } else {
            forkTomcat = false;
        }

        synchronized (mutex) {

            clientCount.incrementAndGet();

            if (started) {
                log.info("NOT starting Tomcat because it is already started");
                log.info("Leaving before: {} users of Tomcat", clientCount.get());
                return;
            }

            if ( forkTomcat ) {
                process = startTomcatProcess();
            } else {
                startTomcatEmbedded();
            }

            log.info("Leaving before: Started Tomcat, now {} users", clientCount.get());
        }

    }


    private String createPropDir() {
        String propDirName = "target" + File.separator + "tomcat_" + port;
        File newDir = new File( propDirName );
        newDir.mkdirs();
        String propDirPath = newDir.getAbsolutePath();
        return propDirPath;
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

            String threads = (String)properties.get("tomcat.threads");

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


    private Process startTomcatProcess() throws IOException {

        port = AvailablePortFinder.getNextAvailable(9998 + RandomUtils.nextInt(10));

        String propDirPath = createPropDir();

        createPropertyFilesForForkedTomcat( propDirPath );

        String javaHome = (String)System.getenv("JAVA_HOME");

        String logConfig = "-Dlog4j.configuration=file:./src/test/resources/log4j.properties";
        String maxMemory = "-Xmx1000m";

        ProcessBuilder pb = new ProcessBuilder(javaHome + "/bin/java", maxMemory, logConfig,
                "org.apache.usergrid.TomcatMain", "src/main/webapp", port + "");

        // ensure Tomcat gets same classpath we have, but with...
        String classpath = System.getProperty("java.class.path");
        List<String> path = new ArrayList<String>();

        // our properties dir at the start
        path.add( propDirPath );

        String parts[] = classpath.split( File.pathSeparator );
        for ( String part : parts ) {
            if ( part.endsWith("test-classes") ) {
                continue;
            }
            path.add(part);
        }
        String newClasspath = StringUtils.join( path, File.pathSeparator );

        Map<String, String> env = pb.environment();
        StringBuilder sb = new StringBuilder();
        sb.append( newClasspath );
        env.put("CLASSPATH", sb.toString());

        //pb.directory(new File("."));
        pb.redirectErrorStream(true);

        final Process p = pb.start();

        //log.debug("Started Tomcat process with classpath = " + newClasspath );

        // use thread to log Tomcat output
        new Thread( new Runnable() {
            @Override
            public void run() {
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = null;
                try {
                    while ((line = br.readLine()) != null) {
                        log.info(line);
                    }

                } catch (Exception ex) {
                    log.error("Error reading from Tomcat process", ex);
                    return;
                } 
            }
        }).start();


        waitForTomcat();

        Runtime.getRuntime().addShutdownHook( new Thread() {
            @Override
            public void run() {
                after();
            }
        } );

        mutex.notifyAll();

        return p;
    }


    private void createPropertyFilesForForkedTomcat( String propDirPath ) throws IOException {

        PrintWriter pw = new PrintWriter( 
            new FileWriter( propDirPath + File.separator + "usergrid-custom.properties"));
        
        pw.println("cassandra.url=localhost:" + cassPort);
        pw.println("cassandra.version=1.2");
        pw.println("cassandra.cluster_name=Usergrid");
        pw.println("cassandra.connections=600");
        pw.println("cassandra.timeout=5000");

        pw.println("elasticsearch.hosts=127.0.0.1");
        pw.println("elasticsearch.port=" + esPort);
        pw.println("elasticsearch.cluster_name=test_cluster");
        pw.println("elasticsearch.index_prefix=usergrid");
        pw.println("elasticsearch.startup=remote");
        pw.println("elasticsearch.force_refresh=" + properties.getProperty("elasticsearch.force_refresh"));
        
        pw.println("collections.keyspace=Usergrid_Applications");
        pw.println("collections.keyspace.strategy.options=replication_factor:1");
        pw.println("collections.keyspace.strategy.class=org.apache.cassandra.locator.SimpleStrategy");
        pw.println("collection.stage.transient.timeout=6");

        pw.println("usergrid.mongo.disable=true");
        pw.println("swagger.basepath=http://sometestvalue");
        pw.println("usergrid.counter.batch.size=1");
        pw.println("usergrid.test=true");

        pw.println("usergrid.sysadmin.login.name=superuser");
        pw.println("usergrid.sysadmin.login.email=superuser@usergrid.com");
        pw.println("usergrid.sysadmin.login.password=superpassword");
        pw.println("usergrid.sysadmin.login.allowed=true");
        
        pw.println("mail.transport.protocol=smtp");
        pw.println("mail.store.protocol=imap");
        pw.println("mail.smtp.host=usergrid.com");
        pw.println("mail.smtp.username=testuser");
        pw.println("mail.smtp.password=testpassword");
        
        pw.println("index.query.limit.default=1000");

        pw.println("usergrid.recaptcha.public=");
        pw.println("usergrid.recaptcha.private=");
        pw.println("usergrid.sysadmin.email=");

        pw.println("usergrid.management.admin_users_require_confirmation=false");
        pw.println("usergrid.management.admin_users_require_activation=false");
        pw.println("usergrid.management.notify_admin_of_activation=false");
        pw.println("usergrid.management.organizations_require_confirmation=false");
        pw.println("usergrid.management.organizations_require_activation=false");
        pw.println("usergrid.management.notify_sysadmin_of_new_organizations=false");
        pw.println("usergrid.management.notify_sysadmin_of_new_admin_users=false");

        pw.println("usergrid.setup-test-account=true");
        pw.println("usergrid.test-account.app=test-app");
        pw.println("usergrid.test-account.organization=test-organization");
        pw.println("usergrid.test-account.admin-user.username=test");
        pw.println("usergrid.test-account.admin-user.name=Test User");
        pw.println("usergrid.test-account.admin-user.email=test@usergrid.com");
        pw.println("usergrid.test-account.admin-user.password=test");
        
        pw.flush();
        pw.close();


//        // include all properties 
//        Map<String, String> allProperties = new HashMap<String, String>();
//        for ( Object name : properties.keySet() ) { 
//            allProperties.put( (String)name, properties.getProperty((String)name));
//        }
//
//        // override some properties with correct port numbers
//        allProperties.put("cassandra.url", "localhost:" + cassPort);
//        allProperties.put("elasticsearch.hosts", "127.0.0.1");
//        allProperties.put("elasticsearch.port", ""+esPort );
//
//        PrintWriter pw = new PrintWriter( 
//            new FileWriter( propDirPath + File.separator + "usergrid-custom.properties"));
//        for ( String name : allProperties.keySet() ) {
//            pw.println(name + "=" + allProperties.get( name ));
//        } 
//        pw.flush();
//        pw.close();
    }

    /**
     * Get the port Tomcat runs on.
     */
    public int getPort() {
        return port;
    }

    public String getWebAppsPath() {
        return webAppsPath;
    }

    public void setWebAppsPath(String webAppsPath) {
        this.webAppsPath = webAppsPath;
    }

    void setCassandraPort(int cassPort) {
        this.cassPort = cassPort;
    }

    void setElasticSearchPort(int esPort) {
        this.esPort = esPort;
    }

    void setProperties(Properties properties) {
        this.properties = properties;
    }
}
