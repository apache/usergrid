/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.webapp.elasticsearch;


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


@FigSingleton
public interface ElasticSearchFig extends GuicyFig {

    String CLUSTER_NAME_DEFAULT = "elasticsearch";
    String CLUSTER_NAME_KEY = "es.cluster.name";

    @Default(CLUSTER_NAME_DEFAULT)
    @Key(CLUSTER_NAME_KEY)
    String getClusterName();


    String SERVERS_KEY = "es.transport.host";
    String SERVERS_DEFAULT = "localhost";

    @Default(SERVERS_DEFAULT)
    @Key(SERVERS_KEY)
    String getTransportHost();


    String PORT_DEFAULT = "9300";
    String PORT_KEY = "es.transport.port";

    @Default(PORT_DEFAULT)
    @Key(PORT_KEY)
    int getTransportPort();


    String HTTP_PORT_DEFAULT = "9200";
    String HTTP_PORT_KEY = "es.http.port";

    @Default(HTTP_PORT_DEFAULT)
    @Key(HTTP_PORT_KEY)
    int getHttpPort();


    String DATA_DIR_DEFAULT = "target/data";
    String DATA_DIR_KEY = "es.data.directory";

    @Default(DATA_DIR_DEFAULT)
    @Key("es.data.directory")
    String getDataDir();
}
