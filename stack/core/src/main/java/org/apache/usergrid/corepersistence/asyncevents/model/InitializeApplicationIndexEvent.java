/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.corepersistence.asyncevents.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.usergrid.corepersistence.index.ReplicatedIndexLocationStrategy;
import org.apache.usergrid.persistence.index.IndexLocationStrategy;

/**
 * event to init app index
 */

public class InitializeApplicationIndexEvent extends AsyncEvent {


    @JsonProperty
    protected IndexLocationStrategy indexLocationStrategy;

    /**
     * Do not delete!  Needed for Jackson
     */
    @SuppressWarnings( "unused" )
    public InitializeApplicationIndexEvent(){

        super();
    }

    public InitializeApplicationIndexEvent(String sourceRegion, final IndexLocationStrategy indexLocationStrategy) {
        super(sourceRegion);
        this.indexLocationStrategy = indexLocationStrategy;

    }

    @JsonDeserialize(as=ReplicatedIndexLocationStrategy.class)
    public IndexLocationStrategy getIndexLocationStrategy() {
        return indexLocationStrategy;
    }
}
