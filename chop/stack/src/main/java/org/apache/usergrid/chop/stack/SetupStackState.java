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
package org.apache.usergrid.chop.stack;


/**
 * Represents the setup state of a stack
 */
public enum SetupStackState {
    SetUp ( "Already set up" ),
    SettingUp ( "Already being set up" ),
    SetupFailed ( "Stack was registered, however its setup failed. Call setup again to restart." ),
    NotSetUp ( "Jar is deployed already but no stack set up with it" ),
    Destroying ( "Currently being destroyed. Wait until it is finished to set up again..." ),
    JarNotFound( "No runner jars found with given parameters, deploy first" );
    private final String message;


    SetupStackState (String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
