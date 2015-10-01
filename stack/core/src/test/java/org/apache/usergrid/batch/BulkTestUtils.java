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


import org.junit.Ignore;

import org.apache.usergrid.batch.repository.JobDescriptor;


/** @author zznate */
@Ignore("Not a test")
public class BulkTestUtils {

    public static JobFactory getBulkJobFactory() {
        return new MyBulkJobFactory();
    }


    private static class MyBulkJobFactory implements JobFactory {
        /* (non-Javadoc)
         * @see org.apache.usergrid.batch.JobFactory#jobsFrom(org.apache.usergrid.batch.repository.JobDescriptor)
         */
        @Override
        public Job jobsFrom( JobDescriptor descriptor ) {
            return  new MyBulkJob();
        }
    }


    private static class MyBulkJob implements Job {
        @Override
        public void execute( JobExecution execution ) throws Exception {
            // do some stuff

        }


        @Override
        public void dead( final JobExecution execution ) throws Exception {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
