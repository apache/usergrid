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
package org.apache.usergrid.batch;


/**
 * Created when a job cannot be instantiated.  This usually occurs during the deploy of new code on nodes that don't yet
 * have the job implementation.  Nodes receiving this message should log it and move on.
 *
 * @author tnine
 */
public class JobRuntimeException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1;


    /**
     *
     */
    public JobRuntimeException() {
        super();
    }


    /**
     * @param arg0
     * @param arg1
     */
    public JobRuntimeException( String arg0, Throwable arg1 ) {
        super( arg0, arg1 );
    }


    /**
     * @param arg0
     */
    public JobRuntimeException( String arg0 ) {
        super( arg0 );
    }


    /**
     * @param arg0
     */
    public JobRuntimeException( Throwable arg0 ) {
        super( arg0 );
    }
}
