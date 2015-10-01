/*
 *
 *  *
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
 *  *
 *
 */

package org.apache.usergrid.persistence.collection.util;


import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.usergrid.persistence.model.util.UUIDGenerator;


public class VersionGenerator {

    /**
     * Generate a list of version uuids for the size specified.  Returns a list of UUID ordered from high to low.
     * @param size The size to generate
     * @return
     */
    public static List<UUID> generateVersions(int size){


        //generate from high to low
        UUID[] versions = new UUID[size];

        for(int i = size -1; i > -1; i--){
            versions[i] = UUIDGenerator.newTimeUUID();
        }


        return Arrays.asList(versions);

    }
}
