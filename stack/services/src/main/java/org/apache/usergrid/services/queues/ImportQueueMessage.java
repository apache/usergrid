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
package org.apache.usergrid.services.queues;


import java.io.Serializable;
import java.util.UUID;


/**
 * Deserializes the Import Message that gets stored in the returned QueueMessage and
 * gets the message back. Currently based on ApplicationQueueMessage
 */
public class ImportQueueMessage implements Serializable {
    /**
     * Import specific identifiers here
     */

    //Needed to see what import job the Queue Message is a part of
    private UUID importId;

    //Needed to determine what file we are working on importing
    private String fileName;

    public ImportQueueMessage(){
    }

    public ImportQueueMessage(UUID importId, String fileName){
        this.importId = importId;
        this.fileName = fileName;
    }


    public UUID getImportId() {
        return importId;
    }


    public void setImportId( final UUID importId ) {
        this.importId = importId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName( final String fileName ) {
        this.fileName = fileName;
    }
}
