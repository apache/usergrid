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


import org.junit.rules.ExternalResource;
import org.apache.usergrid.cassandra.AvailablePortFinder;

import org.apache.commons.lang.math.RandomUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;
import static org.apache.usergrid.persistence.index.impl.EsProvider.LOCAL_ES_PORT_PROPNAME;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * Startup ElasticSearch as a forked process (or not).
 */
public class ElasticSearchResource extends ExternalResource {
    private static final Logger log = LoggerFactory.getLogger(ElasticSearchResource.class);

    public static final ElasticSearchResource instance = new ElasticSearchResource();

    private static final Object mutex = new Object();
    private int port;
    private boolean started = false;
    private Properties properties;
    private String startupType = "forked";

    Process process = null;


    protected ElasticSearchResource() {
        try {
            String[] locations = { "usergrid-properties-context.xml" };
            ConfigurableApplicationContext appContext = 
                    new ClassPathXmlApplicationContext( locations );
            
            properties = (Properties)appContext.getBean("properties");

        } catch (Exception ex) {
            throw new RuntimeException("Error getting properties", ex);
        }

        startupType = properties.getProperty("elasticsearch.startup"); 
    }


    @Override
    protected void after() {    
        log.info("Entering after");

        synchronized (mutex) {

            if ( process != null ) {
                log.info("----------------------------------------------------------------------");
                log.info("Destroying ElasticSearch running on port " + port);
                log.info("----------------------------------------------------------------------");
                process.destroy();
                started = false;
            } 
        }

        log.info("Leaving after");
    }


    @Override
    protected void before() throws Throwable {
        log.info("Entering before");

        if ( "forked".equals( startupType ) ) {

            synchronized (mutex) {

                if (started) {
                    log.info("NOT starting ElasticSearch because it is already started");
                    return;
                }

                process = startElasticSearchProcess();
            }
        }

        log.info("Leaving before");
    }


    private Process startElasticSearchProcess() throws IOException {

        port = AvailablePortFinder.getNextAvailable(4000 + RandomUtils.nextInt(10));
        System.setProperty( LOCAL_ES_PORT_PROPNAME, port+"" );

        String javaHome = (String)System.getenv("JAVA_HOME");

        String maxMemory = "-Xmx1000m";

        ProcessBuilder pb = new ProcessBuilder(javaHome + "/bin/java", maxMemory, 
                "org.apache.usergrid.ElasticSearchMain", 
                properties.getProperty("elasticsearch.cluster_name"), port + "");

        // ensure process gets same classpath we have
        String classpath = System.getProperty("java.class.path");
//        List<String> path = new ArrayList<String>();
//        String parts[] = classpath.split( File.pathSeparator );
//        for ( String part : parts ) {
//            path.add(part);
//        }
//        // plus our special properties directory
//        String newClasspath = StringUtils.join( path, File.pathSeparator );

        Map<String, String> env = pb.environment();
        StringBuilder sb = new StringBuilder();
        sb.append( classpath );
        env.put("CLASSPATH", sb.toString());

        //pb.directory(new File("."));
        pb.redirectErrorStream(true);

        final Process p = pb.start();

        //log.debug("Started ElasticSearch process with classpath = " + newClasspath );

        // use thread to log ElasticSearch output
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
                    log.error("Error reading from ElasticSearch process", ex);
                    return;
                } 
            }
        }).start();

        started = true;

        Runtime.getRuntime().addShutdownHook( new Thread() {
            @Override
            public void run() {
                after();
            }
        } );

        return p;
    }


    public int getPort() {
        return port;
    }
}
