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

package org.apache.usergrid.persistence.core.migration.util;


import com.netflix.astyanax.connectionpool.exceptions.BadRequestException;


public class AstayanxUtils {

    /**
     * Return true if the exception is an instance of a missing keysapce
     * @param rethrowMessage The message to add to the exception if rethrown
     * @param cassandraException The exception from cassandar
     * @return
     */
    public static void isKeyspaceMissing(final String rethrowMessage,  final Exception cassandraException ) {

        if ( cassandraException instanceof BadRequestException ) {

            //check if it's b/c the keyspace is missing, if so
            final String message = cassandraException.getMessage();

            //no op, just swallow
            if(message.contains( "why:Keyspace" ) && message.contains( "does not exist" )){
                return;
            };
        }

       throw new RuntimeException( rethrowMessage, cassandraException );
    }
}
