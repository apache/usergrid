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

package org.apache.usergrid.persistence.core.metrics;


import com.codahale.metrics.Timer;

import rx.Observable;


/**
 * A wrapper class that will allows timing around an observable.  Simply pass an observable and we will set a start and
 * stop issue.
 */
public class ObservableTimer {


    private final Timer timer;
    private Timer.Context context;


    /**
     * Intentionally private, use the factory
     *
     * @param timer
     */
    private ObservableTimer( final Timer timer ) {this.timer = timer;}


    /**
     * Start the timer
     */
    public void start() {
        context = timer.time();
    }


    /**
     * Stop the timer
     */
    public void stop() {
        context.stop();
    }


    /**
     * Time the obserable with the specified timer
     */
    public static <T> Observable<T> time( final Observable<T> observable, final Timer timer ) {
        final ObservableTimer proxy = new ObservableTimer( timer );

        //attach to the observable
        return observable.doOnSubscribe( () -> proxy.start() ).doOnCompleted( () -> proxy.stop() );
    }
}
