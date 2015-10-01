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

/**
 * Class to help with input verification
 */
public class Verify {

    /**
     * Class to help with verification
     * @param value
     * @param message
     */
    public static void isNull(Object value, String message){
        if(value != null){
            throw new IllegalArgumentException(message  );
        }
    }


    /**
     * Verifies that a string exists and must have characters
     * @param string
     * @param message
     */
    public static void stringExists(String string, String message){
        if(string == null){
           throw new NullPointerException( message );
        }

        if(string.length() == 0){
            throw new IllegalArgumentException( message );
        }
    }
}
