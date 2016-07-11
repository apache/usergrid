/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.model.util;


import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.UUIDTimer;
import com.fasterxml.uuid.impl.TimeBasedGenerator;


/**
 * TODO replace this with the Astyanax generator libs
 */
public class UUIDGenerator {


    private static final Random random = new Random();
    private static final UUIDTimer timer;

    /**
     * Lame, but required
     */
    static {
        try {
            timer = new UUIDTimer( random, null );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Couldn't initialize timer", e );
        }
    }


    private static final TimeBasedGenerator generator =
        new TimeBasedGenerator( EthernetAddress.fromInterface(), timer );


    /** Create a new time uuid */
    public static UUID newTimeUUID() {
        return generator.generate();
    }
}
