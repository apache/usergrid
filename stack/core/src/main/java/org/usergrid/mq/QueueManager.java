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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.usergrid.persistence.CounterQuery;
import org.usergrid.persistence.CounterResolution;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Results;

public interface QueueManager {

	public Queue getQueue(String queuePath);

	public Queue updateQueue(String queuePath, Map<String, Object> properties);

	public Queue updateQueue(String queuePath, Queue queue);

	public Message postToQueue(String queuePath, Message message);

	public List<Message> postToQueue(String queuePath, List<Message> messages);

	public QueueResults getFromQueue(String queuePath, QueueQuery query);

	public Message getMessage(UUID messageId);

	public UUID getNewConsumerId();

	public QueueSet getQueues(String firstQueuePath, int limit);

	public QueueSet subscribeToQueue(String publisherQueuePath,
			String subscriberQueuePath);

	public QueueSet unsubscribeFromQueue(String publisherQueuePath,
			String subscriberQueuePath);

	public QueueSet addSubscribersToQueue(String publisherQueuePath,
			List<String> subscriberQueuePaths);

	public QueueSet removeSubscribersFromQueue(String publisherQueuePath,
			List<String> subscriberQueuePaths);

	public QueueSet subscribeToQueues(String subscriberQueuePath,
			List<String> publisherQueuePaths);

	public QueueSet unsubscribeFromQueues(String subscriberQueuePath,
			List<String> publisherQueuePaths);

	public QueueSet getSubscribers(String publisherQueuePath,
			String firstSubscriberQueuePath, int limit);

	public QueueSet getSubscriptions(String subscriberQueuePath,
			String firstSubscriptionQueuePath, int limit);

	public QueueSet searchSubscribers(String publisherQueuePath, Query query);

	public QueueSet getChildQueues(String publisherQueuePath,
			String firstQueuePath, int count);

	public abstract void incrementAggregateQueueCounters(String queuePath,
			String category, String counterName, long value);

	public abstract Results getAggregateQueueCounters(String queuePath,
			String category, String counterName, CounterResolution resolution,
			long start, long finish, boolean pad);

	public abstract Results getAggregateQueueCounters(String queuePath,
			CounterQuery query) throws Exception;

	public abstract Set<String> getQueueCounterNames(String queuePath)
			throws Exception;

	public abstract void incrementQueueCounters(String queuePath,
			Map<String, Long> counts);

	public abstract void incrementQueueCounter(String queuePath, String name,
			long value);

	public abstract Map<String, Long> getQueueCounters(String queuePath)
			throws Exception;

}
