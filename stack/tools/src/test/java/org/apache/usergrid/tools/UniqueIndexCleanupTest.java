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
package org.apache.usergrid.tools;


import java.io.File;
import java.io.FileFilter;

import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang.RandomStringUtils;

import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.ServiceITSuite;

import static org.junit.Assert.assertTrue;


/**
 * Created by ApigeeCorporation on 11/2/15.
 */
public class UniqueIndexCleanupTest {
        static final Logger logger = LoggerFactory.getLogger( ExportAppTest.class );

        int NUM_COLLECTIONS = 10;
        int NUM_ENTITIES = 50;
        int NUM_CONNECTIONS = 3;

        @ClassRule
        public static ServiceITSetup setup = new ServiceITSetupImpl( ServiceITSuite.cassandraResource );

        @org.junit.Test
        public void testBasicOperation() throws Exception {

            String rand = RandomStringUtils.randomAlphanumeric( 10 );

            // create app with some data

            String orgName = "org_" + rand;
            String appName = "app_" + rand;
//
//            ExportDataCreator creator = new ExportDataCreator();
//            creator.startTool( new String[] {
//                    "-organization", orgName,
//                    "-application", appName,
//                    "-host", "localhost:9160" //+ ServiceITSuite.cassandraResource.getRpcPort()
//            }, false);

            long start = System.currentTimeMillis();


            UniqueIndexCleanup uniqueIndexCleanup = new UniqueIndexCleanup();
            uniqueIndexCleanup.startTool( new String[]{
                    "-app", "942712f0-7ce2-11e5-b81a-17ac5477fa5c",
                    "-col", "users",
                    "-host", "localhost:9160"
            }, false );

            System.out.println("completed");
        }

        private static int getFileCount(File exportDir, final String ext ) {
            return exportDir.listFiles( new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getAbsolutePath().endsWith("." + ext);
                }
            } ).length;
        }
}

