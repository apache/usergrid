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
package org.usergrid.mq;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

@XmlRootElement
public class QueueResults {

	private String path;
	private UUID queue;
	private List<Message> messages = new ArrayList<Message>();
	private UUID last;
	private UUID consumer;

	public QueueResults() {

	}

	public QueueResults(Message message) {
		if (message != null) {
			messages.add(message);
		}
	}

	public QueueResults(List<Message> messages) {
		if (messages != null) {
			this.messages = messages;
		}
	}

	public QueueResults(String path, UUID queue, List<Message> messages,
			UUID last, UUID consumer) {
		this.path = path;
		this.queue = queue;
		this.messages = messages;
		this.last = last;
		this.consumer = consumer;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public UUID getQueue() {
		return queue;
	}

	public void setQueue(UUID queue) {
		this.queue = queue;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<Message> getMessages() {
		return messages;
	}

	public void setMessages(List<Message> messages) {
		if (messages == null) {
			messages = new ArrayList<Message>();
		}
		this.messages = messages;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public UUID getLast() {
		return last;
	}

	public void setLast(UUID last) {
		this.last = last;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public UUID getConsumer() {
		return consumer;
	}

	public void setConsumer(UUID consumer) {
		this.consumer = consumer;
	}

	public int size() {
		return messages.size();
	}
}
