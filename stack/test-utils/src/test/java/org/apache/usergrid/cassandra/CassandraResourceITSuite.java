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
package org.apache.usergrid.cassandra;


import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * An example TestSuite to demonstrate how to use the new CassandraResource ExternalResource. Note that this suite fires
 * up a CassandraResource and so does the first test class: in fact it fires up two together besides the third one fired
 * up by the suite. This demonstrates how the parallelism works along with the instance isolation.
 */
@RunWith(ConcurrentSuite.class)
@Suite.SuiteClasses({
        SpringResourceTest.class,           // <== itself fires up instances
        AnotherCassandraResourceIT.class,      // <== uses the existing suite instance
        YetAnotherCassandraResourceIT.class,   // <== uses the existing suite instance
        OkThisIsTheLastIT.class                // <== uses the existing suite instance
})
@Concurrent()
public class CassandraResourceITSuite {
    @ClassRule
    public static SpringResource springResource = SpringResource.setPortsAndStartSpring();
}
