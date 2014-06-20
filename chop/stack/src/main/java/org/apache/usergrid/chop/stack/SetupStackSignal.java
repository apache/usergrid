/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.usergrid.chop.stack;


public enum SetupStackSignal {

    START( 0 ), STOP( 1 ), RESET( 2 ), DESTROY( 3 ), DEPLOY( 4 ), SETUP( 5 );

    private final int signalID;


    private SetupStackSignal( int signalID ) {
        this.signalID = signalID;
    }


    public int getSignalID() {
        return signalID;
    }


    public SetupStackSignal get( int id ) {
        switch ( id ) {
            case 0:
                return START;
            case 1:
                return STOP;
            case 2:
                return RESET;
            case 3:
                return DESTROY;
            case 4:
                return DEPLOY;
            case 5:
                return SETUP;
        }

        throw new RuntimeException( "Should never get here!" );
    }
}
