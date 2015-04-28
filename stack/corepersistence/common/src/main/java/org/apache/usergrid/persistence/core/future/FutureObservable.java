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
package org.apache.usergrid.persistence.core.future;


import rx.Observable;

import java.util.concurrent.FutureTask;


/**
 * Future without the exception nastiness
 */
public class FutureObservable<T> {

    private final T returnVal;
    private final FutureTask<T> future;


    public FutureObservable(final T returnVal) {
        this.returnVal = returnVal;
        future = new FutureTask<T>( () -> returnVal );
    }

    public void done() {
        future.run();
    }

    public Observable<T> observable() {
        return !future.isDone() ? Observable.from(future) : Observable.just(returnVal);
    }
}
