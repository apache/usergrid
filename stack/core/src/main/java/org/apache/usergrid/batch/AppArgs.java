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
package org.apache.usergrid.batch;


import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;


/** @author zznate */
public class AppArgs {

    @Parameter(names = "-host", description = "The Cassandra host to which we will connect")
    private String host = "127.0.0.1";

    @Parameter(names = "-port", description = "The port which we will connect")
    private int port = 9160;

    @Parameter(names = "-workerThreads", description = "The number of worker threads")
    private int workerThreads = 4;

    @Parameter(names = "-sleepFor", description = "Number of seconds to sleep between checks of the work queue")
    private int sleepFor = 2;

    @Parameter(names = "-appContext", description = "Location of Spring Application context files")
    private String appContext;


    public static AppArgs parseArgs( String[] args ) {
        AppArgs appArgs = new AppArgs();
        JCommander jcommander = new JCommander( appArgs, args );
        return appArgs;
    }


    public String getHost() {
        return host;
    }


    public int getPort() {
        return port;
    }


    public int getWorkerThreads() {
        return workerThreads;
    }


    public int getSleepFor() {
        return sleepFor;
    }


    public String getAppContext() {
        return appContext;
    }
}
