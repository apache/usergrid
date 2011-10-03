/*******************************************************************************
 * Copyright (c) 2010, 2011 Ed Anuff and Usergrid, all rights reserved.
 * http://www.usergrid.com
 * 
 * This file is part of Usergrid Core.
 * 
 * Usergrid Core is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Usergrid Core is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Usergrid Core. If not, see <http://www.gnu.org/licenses/>.
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
