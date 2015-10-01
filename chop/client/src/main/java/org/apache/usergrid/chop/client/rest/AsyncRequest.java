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
package org.apache.usergrid.chop.client.rest;


import java.util.concurrent.Callable;


/**
 * An asynchronous request.
 */
public class AsyncRequest<A,R, O extends RestOperation<R>> implements Callable<R> {
    private final O operation;
    private final Class<R> rClass;

    private A associate;
    private Exception exception;


    public AsyncRequest( A associate, O operation, Class<R> rClass ) {
        this.operation = operation;
        this.associate = associate;
        this.rClass = rClass;
    }


    public AsyncRequest( O operation, Class<R> rClass ) {
        this.operation = operation;
        this.rClass = rClass;
    }


    public A getAssociate() {
        return associate;
    }


    public O getRestOperation() {
        return operation;
    }


    public Exception getException() {
        return exception;
    }


    public boolean failed() {
        return exception != null;
    }


    @Override
    public R call() throws Exception {
        try {
            return operation.execute( rClass );
        }
        catch ( Exception e ) {
            this.exception = e;
            throw e;
        }
    }
}
