/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid;


import org.apache.usergrid.corepersistence.migration.AppInfoMigrationPlugin;
import org.apache.usergrid.management.ApplicationCreator;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.export.ExportService;
import org.apache.usergrid.management.importer.ImportService;
import org.apache.usergrid.security.providers.SignInProviderFactory;
import org.apache.usergrid.security.tokens.TokenService;
import org.apache.usergrid.services.ServiceManagerFactory;

import java.util.Properties;


public interface ServiceITSetup extends CoreITSetup {
    ManagementService getMgmtSvc();

    ApplicationCreator getAppCreator();

    ServiceManagerFactory getSmf();

    TokenService getTokenSvc();

    Properties getProps();

    ExportService getExportService();

    ImportService getImportService();

    void refreshIndex();

    /**
     * Convenience method to set a property in the Properties object returned by getProps();
     *
     * @param key the property key
     * @param value the value of the property to set
     *
     * @return the previous value of the property
     */
    Object set( String key, String value );

    /**
     * Convenience method to get a property in the Properties object returned by getProps().
     *
     * @param key the property key
     *
     * @return value the value of the property
     */
    String get( String key );

    SignInProviderFactory getProviderFactory();

    AppInfoMigrationPlugin getAppInfoMigrationPlugin();
}
