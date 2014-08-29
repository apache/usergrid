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

    DESTROY( 0 ), DEPLOY( 1 ), SETUP( 2 ), FAIL ( 3 ), COMPLETE ( 4 );

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
                return DESTROY;
            case 1:
                return DEPLOY;
            case 2:
                return SETUP;
            case 3:
                return FAIL;
            case 4:
                return COMPLETE;
        }

        throw new RuntimeException( "Should never get here!" );
    }
}
