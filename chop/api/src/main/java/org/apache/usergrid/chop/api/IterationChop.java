/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.api;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Annotate test classes with this annotation to run iteration based stress tests. The
 * difference here is not the amount of time your test runs but the number of iterations
 * of your tests per thread. So the total iterations will be:
 * </p>
 * threads() * drivers() * iterations()
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IterationChop {


    /**
     * The number of times (iterations) your tests should be invoked.
     *
     * @return the number of times your tests should be invoked
     */
    @JsonProperty
    long iterations() default Constants.DEFAULT_ITERATIONS;


    /**
     * The number of threads to use for concurrent invocation per runner.
     *
     * @return the number of concurrent threads per runner
     */
    @JsonProperty
    int threads() default Constants.DEFAULT_THREADS;


    /**
     * Whether or not to perform a saturation test before running this test and
     * use those discovered saturation parameters as overrides to the provided
     * parameters.
     *
     * @return whether or not to run a preliminary saturation test
     */
    @JsonProperty
    boolean saturate() default Constants.DEFAULT_SATURATE;


    /**
     * This parameter allows you to introduce a delay between test iterations.
     *
     * @return the delay between test iterations in milliseconds
     */
    @JsonProperty
    long delay() default Constants.DEFAULT_DELAY;
}
