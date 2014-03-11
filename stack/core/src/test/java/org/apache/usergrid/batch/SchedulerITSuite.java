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
package org.apache.usergrid.batch;


import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.apache.usergrid.batch.job.SchedulerRuntime1IT;
import org.apache.usergrid.batch.job.SchedulerRuntime2IT;
import org.apache.usergrid.batch.job.SchedulerRuntime3IT;
import org.apache.usergrid.batch.job.SchedulerRuntime4IT;
import org.apache.usergrid.batch.job.SchedulerRuntime5IT;
import org.apache.usergrid.batch.job.SchedulerRuntime6IT;
import org.apache.usergrid.batch.job.SchedulerRuntime7IT;
import org.apache.usergrid.batch.job.SchedulerRuntime8IT;
import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.cassandra.Concurrent;


@RunWith(Suite.class)
@Suite.SuiteClasses(
        {
                SchedulerRuntime1IT.class, SchedulerRuntime2IT.class, SchedulerRuntime3IT.class,
                SchedulerRuntime4IT.class, SchedulerRuntime5IT.class, SchedulerRuntime6IT.class,
                SchedulerRuntime7IT.class, SchedulerRuntime8IT.class
        })
@Concurrent()
@Ignore("TODO: Todd fix. Does not reliably pass on our build server.")
// TODO - this suite actually runs correctly now so we can
// remove this ignore if Todd is OK with it.
public class SchedulerITSuite {
    @ClassRule
    public static CassandraResource cassandraResource = CassandraResource.newWithAvailablePorts();
}
