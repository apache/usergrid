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
package org.apache.usergrid.websocket;


import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;


public class ExampleListener implements MessageListener {

    @Override
    public void onMessage( Message message ) {
        if ( message instanceof TextMessage ) {
            try {
                System.out.println( ( ( TextMessage ) message ).getText() );
            }
            catch ( JMSException ex ) {
                throw new RuntimeException( ex );
            }
        }
        else {
            throw new IllegalArgumentException( "Message must be of type TextMessage" );
        }
    }
}
