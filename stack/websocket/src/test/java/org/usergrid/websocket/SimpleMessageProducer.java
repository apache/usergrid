/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.websocket;

import java.util.Date;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

public class SimpleMessageProducer {

	private static final Logger logger = LoggerFactory
			.getLogger(SimpleMessageProducer.class);

	@Autowired
	protected JmsTemplate jmsTemplate;

	protected int numberOfMessages = 100;

	public void sendMessages() throws JMSException {

		for (int i = 0; i < numberOfMessages; ++i) {

			final StringBuilder payload = new StringBuilder();
			payload.append("Message [").append(i).append("] sent at: ")
					.append(new Date());
			final int j = i;
			jmsTemplate.send(new MessageCreator() {
				@Override
				public Message createMessage(Session session)
						throws JMSException {
					TextMessage message = session.createTextMessage(payload
							.toString());
					message.setIntProperty("messageCount", j);
					logger.info("Sending message number [" + j + "]");
					return message;
				}
			});
		}
	}
}
