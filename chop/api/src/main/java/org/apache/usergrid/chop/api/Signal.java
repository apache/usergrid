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
package org.apache.usergrid.chop.api;


/** The state dependent and/or impacting signals sent to a runner. */
public enum Signal {
    START( 0 ), STOP( 1 ), RESET( 2 ), LOAD( 3 ), COMPLETED( 4 );

    private final int id;


    private Signal( int id ) {
        this.id = id;
    }


    public int getId() {
        return id;
    }


    public Signal get( int id ) {
        switch ( id ) {
            case 0:
                return START;
            case 1:
                return STOP;
            case 2:
                return RESET;
            case 3:
                return LOAD;
            case 4:
                return COMPLETED;
        }

        throw new RuntimeException( "Should never get here!" );
    }
}
