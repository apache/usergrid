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
package org.usergrid.count;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.usergrid.count.common.Count;


/**
 * A simple Batcher implementation that keeps a sum of the number of {@link Count} operations which have been applied.
 * Counters are aggregated by name.
 *
 * @author zznate
 */
public class SimpleBatcher extends AbstractBatcher {
    private Logger log = LoggerFactory.getLogger( SimpleBatcher.class );

    private boolean blockingSubmit = false;


    public void setBlockingSubmit( boolean blockingSubmit ) {
        this.blockingSubmit = blockingSubmit;
    }
}
