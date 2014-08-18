/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.usergrid.persistence.model.entity;


import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import static org.junit.Assert.assertTrue;


public class SimpleIdTest {

    @Test
    public void uuidComparison() {

        final UUID firstId = UUIDGenerator.newTimeUUID();
        final UUID secondId = UUIDGenerator.newTimeUUID();

        final String type = "test";

        SimpleId first = new SimpleId( firstId, type );
        SimpleId second = new SimpleId( secondId, type );

        assertTrue( first.compareTo( second ) < 0 );

        assertTrue( first.compareTo( first ) == 0 );

        assertTrue( second.compareTo( first ) > 0 );
    }


    @Test
    public void typeComparison() {

        final UUID uuid = UUIDGenerator.newTimeUUID();

        final String firstType = "test1";
        final String secondType = "test2";


        SimpleId first = new SimpleId( uuid, firstType );
        SimpleId second = new SimpleId( uuid, secondType );

        assertTrue( first.compareTo( second ) < 0 );

        assertTrue( first.compareTo( first ) == 0 );

        assertTrue( second.compareTo( first ) > 0 );
    }
}
