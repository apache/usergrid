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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

    Process process = null;


    protected TomcatResource() {}

    @Override
    protected void after() {    
        // TODO: need something better than this, see also TomcatMain.
        process.destroy();
    }
        
    @Override
    protected void before() throws Throwable {

        if (started) {
            return;
        }

        synchronized (mutex) {

            if (started) {
                return;
            }

            port = AvailablePortFinder.getNextAvailable(9998 + RandomUtils.nextInt(10));

            String propDirPath = createPropDir();

            createPropertyFiles( propDirPath );

            process = startTomcatProcess( propDirPath );

            waitForTomcat();

            started = true;
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
        int count = 0;
        while (count++ < 60) {
            try {
                Thread.sleep(1000);
                Client c = Client.create();
                WebResource wr = c.resource("http://localhost:" + port + "/status");
                wr.get(String.class);
                log.info("Tomcat is started.");
                break;
                
            } catch (Exception e) {
                log.info("Cannot connect: " + e.getMessage());
            }
            throw new RuntimeException("Tomcat process never started.");
        }
    }

    private Process startTomcatProcess( String propDirPath ) throws IOException {

        String javaHome = (String)System.getenv("JAVA_HOME");

        ProcessBuilder pb = new ProcessBuilder(javaHome + "/bin/java",
                "org.apache.usergrid.TomcatMain", "src/main/webapp", port + "");

        // ensure Tomcat gets same classpath we have
        String classpath = System.getProperty("java.class.path");
        List<String> path = new ArrayList<String>();
        String parts[] = classpath.split( File.pathSeparator );
        for ( String part : parts ) {
            if ( part.endsWith("test-classes") ) {
                continue;
            }
            path.add(part);
        }
        // plus our special properties directory
        path.add( propDirPath );
        String newClasspath = StringUtils.join( path, File.pathSeparator );

        Map<String, String> env = pb.environment();
        StringBuilder sb = new StringBuilder();
        sb.append( newClasspath );
        env.put("CLASSPATH", sb.toString());

        //pb.directory(new File("."));
        pb.redirectErrorStream(true);

        final Process p = pb.start();

        log.debug("Started Tomcat process with classpath = " + newClasspath );

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
                } catch (IOException ex) {
                    log.error("Error reading from Tomcat process", ex);
                    return;
                }
            }
        }).start();

        return p;
    }

    private void createPropertyFiles( String propDirPath ) throws IOException {

        PrintWriter pw = new PrintWriter( 
                new FileWriter( propDirPath + File.separator + "usergrid-custom.properties"));
        
        pw.println("cassandra.url=localhost:" + cassPort);
        pw.println("usergrid.mongo.disable=true");
        pw.println("swagger.basepath=http://sometestvalue");
        pw.println("usergrid.counter.batch.size=1");
        pw.println("usergrid.test=true");
        pw.println("usergrid.sysadmin.login.name=superuser");
        pw.println("usergrid.sysadmin.login.email=superuser@usergrid.com");
        pw.println("usergrid.sysadmin.login.password=superpassword");
        pw.println("usergrid.sysadmin.login.allowed=true");
        pw.flush();
        pw.close();
        
        pw = new PrintWriter( 
                new FileWriter( propDirPath + File.separator + "corepersistence.properties"));
        
        pw.println("cassandra.hosts=127.0.0.1");
        pw.println("cassandra.port=" + cassPort);
        pw.println("cassandra.version=1.2");
        pw.println("cassandra.cluster_name=Usergrid");
        pw.println("cassandra.connections=20");
        pw.println("cassandra.timeout=5000");
        
        pw.println("collections.keyspace=Usergrid_Applications");
        pw.println("collections.keyspace.strategy.options=replication_factor:1");
        pw.println("collections.keyspace.strategy.class=org.apache.cassandra.locator.SimpleStrategy");
        pw.println("collection.stage.transient.timeout=6");
        
        pw.println("elasticsearch.hosts=127.0.0.1");
        pw.println("elasticsearch.port=" + esPort);
        pw.println("elasticsearch.cluster_name=usergrid_test");
        pw.println("elasticsearch.index_prefix=usergrid");
        
        pw.println("index.query.limit.default=1000");
        pw.flush();
        pw.close();
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
}
