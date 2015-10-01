/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid;


import org.apache.usergrid.persistence.model.util.UUIDGenerator;


/**
 * Simple class to manipulate UUIDs into strings for unique strings when testing
 */
public class TestHelper {

    /**
     * Generate a unique name for an organization
     * @return
     */
    public static String uniqueOrg(){
        return "org" + newUUIDString();
    }


    /**
     * Generate a unique name for an application
     * @return
     */
    public static String uniqueApp(){
        return "app" + newUUIDString();
    }


    /**
     * Generate a unique username
     * @return
     */
    public static String uniqueUsername(){
        return "user" + newUUIDString();
    }


    /**
     * Generate a unique email
     * @return
     */
   public static String uniqueEmail(){
       return "user" + newUUIDString() + "@apache.org";
   }



    /**
     * Generate a new UUID, and remove all the '-' characters from the resulting string.
     * @return
     */
    public static String newUUIDString() {
        return UUIDGenerator.newTimeUUID().toString().replace( "-", "" );
    }
}
