/*
 * Copyright 2014 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid;

import com.google.common.io.Files;
import java.io.File;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Simple wrapper for starting "embedded" Tomcat as it's own process, for testing.
 */
public class TomcatMain {
    
    private static final Logger log = LoggerFactory.getLogger( TomcatMain.class );

    public static void main(String[] args) throws Exception {

        String webappsPath = args[0];
        int port = Integer.parseInt( args[1] );

        File dataDir = Files.createTempDir();
        dataDir.deleteOnExit();

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(dataDir.getAbsolutePath());
        tomcat.setPort(port);
        tomcat.getConnector().setAttribute("maxThreads", "1000");
        tomcat.addWebapp("/", new File(webappsPath).getAbsolutePath());

        log.info("-----------------------------------------------------------------");
        log.info("Starting Tomcat port {} dir {}", port, webappsPath);
        log.info("-----------------------------------------------------------------");
        tomcat.start();

        while ( true ) {
            Thread.sleep(1000);
        }
    }

}
