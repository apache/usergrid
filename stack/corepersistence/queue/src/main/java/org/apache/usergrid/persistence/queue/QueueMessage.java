/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.queue;

public class QueueMessage {
    private final Object body;
    private final String messageId;
    private final String handle;
    private final String type;
    private String stringBody;
    private int receiveCount;


    public QueueMessage(String messageId, String handle, Object body,String type) {
        this.body = body;
        this.messageId = messageId;
        this.handle = handle;
        this.type = type;
        this.stringBody = "";
        this.receiveCount = 1; // we'll always receive once if we're taking it off the in mem or AWS queue
    }

    public String getHandle() {
        return handle;
    }

    public Object getBody(){
        return body;
    }

    public String getMessageId() {
        return messageId;
    }


    public String getType() {
        return type;
    }

    public void setStringBody(String stringBody) {
        this.stringBody = stringBody;
    }

    public String getStringBody() {
        return stringBody;
    }

    public void setReceiveCount(int receiveCount){
        this.receiveCount = receiveCount;
    }

    public int getReceiveCount(){
        return receiveCount;
    }
}
