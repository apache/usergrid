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
package org.apache.usergrid.chop.webapp;


import com.google.inject.servlet.GuiceFilter;
import org.apache.commons.cli.*;
import org.safehaus.jettyjam.utils.*;


/**
 * The executable jar file main entry point is contained in this launcher class which
 * fires up an embedded jetty instance based on jettyjam configuration annotations.
 */
@JettyContext(
        enableSession = true,
        contextListeners = {@ContextListener(listener = ChopUiConfig.class)},
        filterMappings = {@FilterMapping(filter = GuiceFilter.class, spec = "/*")}
)
@JettyConnectors(
        defaultId = "https",
        httpsConnectors = {@HttpsConnector(id = "https", port = 8443)}
)
public class ChopUiJettyRunner extends JettyRunner {

    private static CommandLine cl;


    public ChopUiJettyRunner() {
        super(ChopUiJettyRunner.class.getSimpleName());
    }


    @Override
    public String getSubClass() {
        return getClass().getName();
    }


    public static void main(String[] args) throws Exception {
        processCli(args);
        ChopUiJettyRunner launcher = new ChopUiJettyRunner();
        launcher.start();
    }


    public static CommandLine getCommandLine() {
        return cl;
    }


    static void processCli(String[] args) {
        CommandLineParser parser = new PosixParser();
        Options options = getOptions();

        try {
            cl = parser.parse(options, args);
        } catch (ParseException e) {
            if (e instanceof MissingArgumentException) {
                System.out.println("Missing option: " + ((MissingArgumentException) e).getOption());
            }

            help(options);
            System.exit(1);
        }

        if (cl.hasOption('h')) {
            help(options);
            System.exit(0);
        }
    }


    static void help(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ChopUi", options);
    }


    static Options getOptions() {
        Options options = new Options();

        options.addOption("h", "help", false, "Print out help.");
        options.addOption("e", "embedded", false, "Starts an embedded ES instance.");
        options.addOption("d", "home-dir", true, "The home directory for ChopUi: path to " +
                "home directory argument.");
        options.addOption("j", "join", true, "Joins an existing ES cluster: cluster name argument.");
        options.addOption("c", "client-only", true, "Client to existing ES cluster: transport address argument " +
                "(i.e. localhost:3456)");
        options.addOption( "n", "name-of-cluster", true, "Sets the name of the ES instance/cluster." );

        return options;
    }
}
