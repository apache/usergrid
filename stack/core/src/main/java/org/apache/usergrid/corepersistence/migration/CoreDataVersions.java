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

package org.apache.usergrid.corepersistence.migration;


/**
 * Versions of data as they exist across our system
 */
public enum CoreDataVersions {

    // even though this didn't really come first in time, we need to run this first
    // in order to bring our system up to date so that our new migration module can proceed.

    INITIAL(0),
    ID_MAP_FIX(1),
    APPINFO_FIX(2);


    private final int version;


    private CoreDataVersions( final int version ) {this.version = version;}


    public int getVersion() {
        return version;
    }
}
