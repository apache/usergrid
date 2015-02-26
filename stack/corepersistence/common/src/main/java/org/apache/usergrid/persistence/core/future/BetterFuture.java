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

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Future without the exception nastiness
 */
public  class BetterFuture<T> extends FutureTask<T> {
    public BetterFuture(Callable<T> callable){
        super(callable);
    }

    public void done(){
        run();
    }

    public T get(){
        try {
            return super.get();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }


}
