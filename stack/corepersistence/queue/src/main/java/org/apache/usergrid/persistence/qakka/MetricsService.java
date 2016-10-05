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

package org.apache.usergrid.persistence.qakka;

import com.codahale.metrics.MetricRegistry;


public interface MetricsService {

    String SEND_TIME_TOTAL  = "qakka.send.time.total";
    String SEND_TIME_SEND   = "qakka.send.time.send";
    String SEND_TIME_WRITE  = "qakka.send.time.write";
    String GET_TIME_TOTAL   = "qakka.get.time.total";
    String GET_TIME_GET     = "qakka.get.time.get";
    String ACK_TIME_TOTAL   = "qakka.ack.time.total";
    String ACK_TIME_ACK     = "qakka.ack.time.ack";
    String TIMEOUT_TIME     = "qakka.timeout.time";
    String REFRESH_TIME     = "qakka.refresh.time";
    String ALLOCATE_TIME    = "qakka.allocate.time";

    MetricRegistry getMetricRegistry();
}
