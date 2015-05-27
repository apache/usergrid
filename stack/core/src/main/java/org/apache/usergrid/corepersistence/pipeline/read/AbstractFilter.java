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

package org.apache.usergrid.corepersistence.pipeline.read;


import org.apache.usergrid.corepersistence.pipeline.PipelineContext;
import org.apache.usergrid.corepersistence.pipeline.PipelineOperation;


/**
 * Basic functionality for our commands to handle cursor IO
 * @param <T> the input type
 * @param <R> The output Type
 */
public abstract class AbstractFilter<T, R> implements PipelineOperation<T, R> {


    protected PipelineContext pipelineContext;


    @Override
    public void setContext( final PipelineContext pipelineContext ) {
        this.pipelineContext = pipelineContext;
    }



}
