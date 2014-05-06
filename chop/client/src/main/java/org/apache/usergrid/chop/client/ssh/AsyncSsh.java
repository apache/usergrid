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
package org.apache.usergrid.chop.client.ssh;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.usergrid.chop.api.SshValues;


/**
 * Executes the collection of ssh or scp commands, on a separate thread for each remote end
 */
public class AsyncSsh {

    private Collection<SshValues> sshValues;

    private Collection<Command> commands;


    public AsyncSsh( Collection<SshValues> sshValues, Collection<Command> commands ) {
        this.commands = commands;
        this.sshValues = sshValues;
    }


    public Collection<ResponseInfo> executeAll() throws InterruptedException, ExecutionException {

        Collection<Job> jobs = new HashSet<Job>( sshValues.size() );
        for( SshValues sshValue: sshValues ) {
            jobs.add( new Job( commands, sshValue ) );
        }

        ExecutorService service = Executors.newFixedThreadPool( sshValues.size() + 1 );
        List<Future<ResponseInfo>> futureResponses = service.invokeAll( jobs );
        service.shutdown();

        Collection<ResponseInfo> responses = new ArrayList<ResponseInfo>( sshValues.size() );

        for( Future<ResponseInfo> response: futureResponses ) {
            responses.add( response.get() );
        }
        return responses;
    }
}
