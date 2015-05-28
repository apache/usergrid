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

package org.apache.usergrid.corepersistence.pipeline;


import org.apache.usergrid.corepersistence.pipeline.PipelineContext;
import org.apache.usergrid.corepersistence.pipeline.read.FilterResult;

import rx.Observable;


/**
 * Interface for filtering commands.  All filters must take an observable of Id's as an input.  Output is then determined by subclasses.
  * This takes an input of Id, performs some operation, and emits values for further processing in the Observable
  * pipeline
 * @param <T> The input type of the filter value
 * @param <R> The output type of the filter value
 */
public interface PipelineOperation<T, R> extends Observable.Transformer<T, R> {

    void setContext(final PipelineContext pipelineContext);
}
