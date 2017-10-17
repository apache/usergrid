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

package org.apache.usergrid.persistence.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by peterajohnson on 10/16/17.
 */
public class DebugUtils {

    private static ThreadLocal<MessageId> MESSAGE = new ThreadLocal<>();

    public static final Logger logger = LoggerFactory.getLogger( DebugUtils.class );

    static public void startRequest() {
        MESSAGE.set(new MessageId());
    }

    static public void endRequest() {
        MESSAGE.set(null);
    }

    static public long timeTaken() {
        MessageId id = MESSAGE.get();
        if (id == null) {
            return -1L;
        }
        return id.timeTakenMS();
    }

    static public MessageId getMessageId() {
        MessageId id = MESSAGE.get();
        return id;
    }


    static public String getLogMessage() {
        MessageId id = MESSAGE.get();
        String msg;
        if (id == null) {
            msg = "MESSAGE_ID=null";
        } else {
            msg = id.getLogMessage();
        }
        return msg;
    }



    public static class MessageId {
        private UUID uuid = UUID.randomUUID();
        private long startTime = System.nanoTime();

        MessageId() {
        }

        public long timeTakenMS() {
            long now = System.nanoTime();
            return TimeUnit.NANOSECONDS.toMillis(now - startTime);
        }

        public String getLogMessage() {
            return " MESSAGE_ID="  + uuid + " time=" + timeTakenMS() + "ms.";
        }
    }

}


