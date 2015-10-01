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
 * Enumeration of versions for the application and organization data stored by the system.
 * Specifically by the CpEntityManagerImpl and MangementSystemImpl classes.
 */
public enum AppInfoVersions {

    INITIAL(0),
    APPINFO_FIX(1); // Migration of appinfo to application_info

    private final int version;


    private AppInfoVersions( final int version ) {this.version = version;}


    public int getVersion() {
        return version;
    }
}
