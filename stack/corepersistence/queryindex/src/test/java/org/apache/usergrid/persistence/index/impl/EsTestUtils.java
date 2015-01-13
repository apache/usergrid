/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.index.impl;


import org.apache.usergrid.persistence.index.EntityIndex;


/**
 * Utilities to make testing ES easier
 */
public class EsTestUtils {


    /**
     * Checks to see if we have pending tasks in the cluster.  If so waits until they are finished.  Adding
     * new types can cause lag even after refresh since the type mapping needs applied
     * @param index
     */
    public static void waitForTasks(final EntityIndex index){

        while(index.getPendingTasks() > 0){
            try {
                Thread.sleep( 100 );
            }
            catch ( InterruptedException e ) {
                //swallow
            }
        }
    }
}
