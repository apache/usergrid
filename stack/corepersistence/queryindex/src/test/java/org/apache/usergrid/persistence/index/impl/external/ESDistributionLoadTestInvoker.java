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
package org.apache.usergrid.persistence.index.impl.external;

import java.io.File;


public class ESDistributionLoadTestInvoker {

    public static void main(String args[]) {
        Config.init();
        if (!Config.isConfigLoaded()) {
            System.out.println("Config not loaded! Please check your config file");
            System.exit(1);
        }
        String filePath = Config.getDataFilePath();
        if (filePath == null || filePath.trim().isEmpty()) {
            System.out.println("Data file path can not be empty!");
            System.exit(1);
        }
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("Data file is not valid!");
        }

        ESDistributionLoadTest esDistributionLoadTest = new ESDistributionLoadTest();
        esDistributionLoadTest.start();
    }
}
