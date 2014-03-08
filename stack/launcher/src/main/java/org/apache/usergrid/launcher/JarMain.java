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
package org.apache.usergrid.launcher;


/**
 * The jar file executable main method in this class uses the graphical App
 * when no arguments are provided, but switches to using the standalone cli
 * Server class when arguments are provided. To launch the standalone cli
 * version the -nogui parameter must be provided first. This way -db and
 * -init need not be provided to launch in cli mode.
 */
public class JarMain {

    public static void main( String [] args ) {

        // With no arguments we just start up the graphical launcher
        if ( args.length == 0 ) {
            App.main( args );
        }
        else {
            Server.main( args );
        }
    }
}
