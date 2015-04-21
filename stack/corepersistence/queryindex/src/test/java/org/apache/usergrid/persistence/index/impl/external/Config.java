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

import org.apache.commons.lang3.math.NumberUtils;

import java.util.Properties;
import java.util.UUID;

public final class Config {

    private static final String ELASTIC_SEARCH = "elasticsearch.";

    private static final String CLUSTER_NAME = ELASTIC_SEARCH + "cluster.name";
    private static final String HOSTS = ELASTIC_SEARCH + "hosts";
    private static final String PORT = ELASTIC_SEARCH + "port";
    private static final String INDEX_ALIAS = ELASTIC_SEARCH + "index.alias";
    private static final String NUMBER_OF_SHARDS = ELASTIC_SEARCH + "number.of.shards";
    private static final String NUMBER_OF_REPLICA = ELASTIC_SEARCH + "number.of.replica";
    private static final String CLIENT = ELASTIC_SEARCH + "client";

    private static final String DATA = "data.";

    private static final String FILE_PATH = DATA + "file.path";
    private static final String DATA_SIZE = DATA + "size";
    private static final String READ_BATCH_SIZE = DATA + "read.batch.size";
    private static final String PROCESS_DATA_SIZE = DATA + "process.batch.size";

    private static final String MAX_THREADS = "max.threads";

    private static final String USERGRID = "usergrid.";

    private static final String MANAGEMENT_APP_UUID = USERGRID + "management.app.uuid";
    private static final String COLLECTION_TYPE = USERGRID + "type";

    private static final Properties properties = new Properties();

    public static void init() {
        try {
            properties.load(Config.class.getClassLoader().getResourceAsStream("es-distribution-test.properties"));
            System.out.println("Properties: "+properties);
        } catch (Exception ex) {
            System.out.println("Unable to read properties file!" + ex);
        }
    }

    public static String[] getHosts() {
        return properties.getProperty(HOSTS, "localhost").split(",");
    }

    public static int getPort() {
        return NumberUtils.toInt(properties.getProperty(PORT), 9300);
    }

    public static String getClusterName() {
        return properties.getProperty(CLUSTER_NAME, "usergrid-test");
    }

    public static String getIndexAlias() {
        return properties.getProperty(INDEX_ALIAS, "usergrid-load-test");
    }

    public static int getNumberOfShards() {
        return NumberUtils.toInt(properties.getProperty(NUMBER_OF_SHARDS), 5);
    }

    public static int getNumberOfReplica() {
        return NumberUtils.toInt(properties.getProperty(NUMBER_OF_REPLICA), 1);
    }

    public static String getClient() {
        return properties.getProperty(CLIENT, "transport");
    }

    public static String getDataFilePath() {
        return properties.getProperty(FILE_PATH);
    }

    public static int getDataSize() {
        return NumberUtils.toInt(properties.getProperty(DATA_SIZE), 10000);
    }

    public static int getReadBatchSize() {
        return NumberUtils.toInt(properties.getProperty(READ_BATCH_SIZE), 20000);
    }

    public static int getProcessBatchSize() {
        return NumberUtils.toInt(properties.getProperty(PROCESS_DATA_SIZE), 100);
    }

    public static int getMaxThreads() {
        return NumberUtils.toInt(properties.getProperty(MAX_THREADS), 100);
    }

    public static boolean isConfigLoaded() {
        return !properties.isEmpty();
    }

    public static UUID getManagementAppUuid() {
        return UUID.fromString(properties.getProperty(MANAGEMENT_APP_UUID, "a1df03ed-e729-11e4-b073-6ee9d818ea1b"));
    }

    public static String getCollectionType() {
        return properties.getProperty(COLLECTION_TYPE, "my-type");
    }
}
