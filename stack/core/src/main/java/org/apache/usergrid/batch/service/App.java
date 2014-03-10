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
package org.apache.usergrid.batch.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.google.common.base.CharMatcher;


/**
 * Entry point for CLI functions of Usergrid batch framework
 * <p/>
 * To run this with the built-in examples, invoke it thusly from the top level of the project directory:
 * <p/>
 * mvn -e exec:java -Dexec.mainClass="org.apache.usergrid.batch.App" -Dexec.args="-appContext
 * src/test/resources/appContext.xml"
 *
 * @author zznate
 */
public class App {

    private static Logger logger = LoggerFactory.getLogger( App.class );

    private ApplicationContext appContext;
    private final org.apache.usergrid.batch.AppArgs appArgs;


    public static void main( String[] args ) {
        org.apache.usergrid.batch.AppArgs appArgs = org.apache.usergrid.batch.AppArgs.parseArgs( args );
        if ( logger.isDebugEnabled() ) {
            logger.debug( "Invoked App with appArgs: {}", appArgs.toString() );
        }

        App app = new App( appArgs );

        app.loadContext();

        logger.info( "Context loaded, invoking execute() ..." );
        app.doExecute();
    }


    App( org.apache.usergrid.batch.AppArgs appArgs ) {
        this.appArgs = appArgs;
    }


    private void loadContext() {
        logger.info( "loading context" );
        // spring context
        int index = CharMatcher.is( ':' ).indexIn( appArgs.getAppContext() );
        if ( index > 0 ) {
            appContext = new ClassPathXmlApplicationContext( appArgs.getAppContext().substring( ++index ) );
        }
        else {
            appContext = new FileSystemXmlApplicationContext( appArgs.getAppContext() );
        }
    }


    private void doExecute() {
        JobSchedulerService bjss = appContext.getBean( "bulkJobScheduledService", JobSchedulerService.class );
        logger.info( "starting scheduledService..." );
        bjss.startAndWait();
        logger.info( "scheduledService started." );
    }
}
