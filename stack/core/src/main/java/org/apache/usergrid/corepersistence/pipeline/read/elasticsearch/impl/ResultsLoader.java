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

package org.apache.usergrid.corepersistence.pipeline.read.elasticsearch.impl;


import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.index.CandidateResults;


/**
 * Interface for loading results
 */
public interface ResultsLoader {

    /**
     * Using the candidate results, load our results.  Should filter stale results
     * @param  crs The candidate result set
     * @return Results.  Null safe, but may be empty
     */
    public Results loadResults( final CandidateResults crs);

    /**
     * Post process the load operation
     */
    public void postProcess();
}
