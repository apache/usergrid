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


public class SCPCommand implements Command {

    private String sourceFilePath;
    private String destinationFilePath;


    public SCPCommand( String sourceFilePath, String destinationFilePath ) {
        this.sourceFilePath = sourceFilePath;
        this.destinationFilePath = destinationFilePath;
    }


    public String getSourceFilePath() {
        return sourceFilePath;
    }


    public String getDestinationFilePath() {
        return destinationFilePath;
    }


    @Override
    public CommandType getType() {
        return CommandType.SCP;
    }


    @Override
    public String getDescription() {
        return new StringBuilder()
                .append( "scp " )
                .append( sourceFilePath )
                .append( " to " )
                .append( destinationFilePath )
                .toString();
    }
}
