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
public class JobNotFoundException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -67437852214725320L;

    private static final String DEF_MSG = "Unable to find the job with name %s";


    public JobNotFoundException( String jobName ) {
        super( String.format( DEF_MSG, jobName ) );
    }
}
