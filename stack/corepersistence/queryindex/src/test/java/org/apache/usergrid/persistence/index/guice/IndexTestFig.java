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

package org.apache.usergrid.persistence.index.guice;


import java.util.UUID;

import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 * Test configuration for creating documents
 */
@FigSingleton
public interface IndexTestFig extends GuicyFig {

    @Key( "stresstest.numWorkers" )
    @Default( "16" )
    public int getNumberOfWorkers();

    @Key( "stresstest.numberOfRecords" )
    @Default( "10000" )
    public int getNumberOfRecords();

    @Key( "stresstest.bufferSize" )
    @Default( "1000" )
    public int getBufferSize();

    @Key( "stresstest.validate.wait" )
    @Default( "2000" )
    public long getValidateWait();


    @Key( "stresstest.applicationId" )
    @Default( "0df46683-cdab-11e4-83c2-d2be4de3081a" )
    public String getApplicationId();

    @Key( "stresstest.readThreads" )
    @Default( "40" )
    public int getConcurrentReadThreads();

}
