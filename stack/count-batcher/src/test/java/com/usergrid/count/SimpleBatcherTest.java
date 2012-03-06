/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.usergrid.count;

import com.usergrid.count.common.Count;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Unit test for simple SimpleBatcher.
 */
public class SimpleBatcherTest {

    @Before
    public void setupLocal() {

    }


    @Test
    public void testBatchSizeTrigger() {
        SimpleBatcher simpleBatcher = new SimpleBatcher(1);
        simpleBatcher.setBatchSubmitter(new Slf4JBatchSubmitter());
        simpleBatcher.setBatchSize(4);
        simpleBatcher.add(new Count("Counter","k1","counter1", 1));
        simpleBatcher.add(new Count("Counter","k1","c2", 2));
        simpleBatcher.add(new Count("Counter","k1","c3",1));
        simpleBatcher.add(new Count("Counter","k1","c3", 1));
        simpleBatcher.add(new Count("Counter","k1","c3",1));

        assertEquals(1, simpleBatcher.getBatchSubmissionCount());
        simpleBatcher.add(new Count("Counter","k1","c4",1));
        assertEquals(1, simpleBatcher.getBatchSubmissionCount());

    }

}
