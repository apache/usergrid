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
package org.apache.usergrid.count;


import org.junit.Before;
import org.junit.Test;

import org.apache.usergrid.count.common.Count;

import static junit.framework.Assert.assertEquals;


/** Unit test for simple SimpleBatcher. */
public class SimpleBatcherTest {

    @Before
    public void setupLocal() {

    }


    @Test
    public void testBatchSizeTrigger() {
        SimpleBatcher simpleBatcher = new SimpleBatcher();
        simpleBatcher.setBatchSubmitter( new Slf4JBatchSubmitter() );
        simpleBatcher.setBatchSize( 4 );
        simpleBatcher.add( new Count( "Counter", "k1", "counter1", 1 ) );
        simpleBatcher.add( new Count( "Counter", "k1", "c2", 2 ) );
        simpleBatcher.add( new Count( "Counter", "k1", "c3", 1 ) );
        simpleBatcher.add( new Count( "Counter", "k1", "c3", 1 ) );
        simpleBatcher.add( new Count( "Counter", "k1", "c3", 1 ) );

        assertEquals( 1, simpleBatcher.getBatchSubmissionCount() );
        simpleBatcher.add( new Count( "Counter", "k1", "c4", 1 ) );
        assertEquals( 1, simpleBatcher.getBatchSubmissionCount() );
    }
}
