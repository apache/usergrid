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

import static java.util.UUID.nameUUIDFromBytes;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.usergrid.utils.ConversionUtils.getInt;
import static org.usergrid.utils.ConversionUtils.getLong;
import static org.usergrid.utils.ListUtils.first;
import static org.usergrid.utils.UUIDUtils.isUUID;
import static org.usergrid.utils.UUIDUtils.tryGetUUID;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.usergrid.persistence.Query;
import org.usergrid.utils.ConversionUtils;

public class QueueQuery extends Query {

	UUID consumerId;
	long lastTimestamp;
	UUID lastMessageId;
	int previousCount = 0;
	int nextCount = 1;
	QueuePosition position = null;
	boolean _synchronized;
	boolean update = true;

	public QueueQuery() {
	}

	public QueueQuery(Query q) {
		super(q);
	}

	public QueueQuery(QueueQuery q) {
		super(q);
		if (q != null) {
			consumerId = q.consumerId;
			lastTimestamp = q.lastTimestamp;
			lastMessageId = q.lastMessageId;
			previousCount = q.previousCount;
			nextCount = q.nextCount;
			position = q.position;
			_synchronized = q._synchronized;
			update = q.update;
		}
	}

	public static QueueQuery newQueryIfNull(QueueQuery query) {
		if (query == null) {
			query = new QueueQuery();
		}
		return query;
	}

	public static QueueQuery fromQL(String ql) {
		if (ql == null) {
			return null;
		}
		QueueQuery query = null;
		Query q = Query.fromQL(ql);
		if (q != null) {
			query = new QueueQuery(q);
		}
		return query;
	}

	public static QueueQuery fromQueryParams(Map<String, List<String>> params) {

		QueueQuery query = null;

		Query q = Query.fromQueryParams(params);
		if (q != null) {
			query = new QueueQuery(q);
		}

		String consumer = first(params.get("consumer"));

		if (consumer != null) {
			query = newQueryIfNull(query);
			query.setConsumerId(getConsumerId(consumer));
		}

		UUID last = tryGetUUID(first(params.get("last")));
		if (last != null) {
			query = newQueryIfNull(query);
			query.setLastMessageId(last);
		}

		if (params.containsKey("time")) {
			query = newQueryIfNull(query);
			long t = getLong(first(params.get("time")));
			if (t > 0) {
				query.setLastTimestamp(t);
			}
		}

		if (params.containsKey("prev")) {
			query = newQueryIfNull(query);
			query.setPreviousCount(getInt(first(params.get("prev"))));
		}

		if (params.containsKey("next")) {
			query = newQueryIfNull(query);
			query.setNextCount(getInt(first(params.get("next"))));
		}

		if (params.containsKey("pos")) {
			query = newQueryIfNull(query);
			QueuePosition pos = QueuePosition.find(first(params.get("pos")));
			if (pos != null) {
				query.setPosition(pos);
			}
		}

		if (params.containsKey("update")) {
			query = newQueryIfNull(query);
			query.setUpdate(ConversionUtils.getBoolean(first(params
					.get("update"))));
		}

		if (params.containsKey("synchronized")) {
			query = newQueryIfNull(query);
			query.setSynchronized(ConversionUtils.getBoolean(first(params
					.get("synchronized"))));
		}

		if ((query != null) && (consumer != null)) {
			query.setPositionIfUnset(QueuePosition.CONSUMER);
		}

		return query;
	}

	public UUID getConsumerId() {
		return consumerId;
	}

	public void setConsumerId(UUID consumerId) {
		this.consumerId = consumerId;
	}

	public QueueQuery withConsumerId(UUID consumerId) {
		this.consumerId = consumerId;
		setPositionIfUnset(QueuePosition.CONSUMER);
		return this;
	}

	public QueueQuery withConsumer(String consumer) {
		consumerId = getConsumerId(consumer);
		setPositionIfUnset(QueuePosition.CONSUMER);
		return this;
	}

	public long getLastTimestamp() {
		return lastTimestamp;
	}

	public void setLastTimestamp(long lastTimestamp) {
		this.lastTimestamp = lastTimestamp;
	}

	public QueueQuery withLastTimestamp(long lastTimestamp) {
		this.lastTimestamp = lastTimestamp;
		return this;
	}

	public UUID getLastMessageId() {
		return lastMessageId;
	}

	public void setLastMessageId(UUID lastMessageId) {
		this.lastMessageId = lastMessageId;
	}

	public QueueQuery withLastMessageId(UUID lastMessageId) {
		this.lastMessageId = lastMessageId;
		return this;
	}

	public int getPreviousCount() {
		return previousCount;
	}

	public void setPreviousCount(int previousCount) {
		this.previousCount = previousCount;
	}

	public QueueQuery withPreviousCount(int previousCount) {
		this.previousCount = previousCount;
		return this;
	}

	public int getNextCount() {
		return nextCount;
	}

	public void setNextCount(int nextCount) {
		this.nextCount = nextCount;
	}

	public QueueQuery withNextCount(int nextCount) {
		this.nextCount = nextCount;
		return this;
	}

	public QueuePosition getPosition() {
		if (position != null) {
			return position;
		}
		return QueuePosition.LAST;
	}

	public boolean isPositionSet() {
		return position != null;
	}

	public void setPosition(QueuePosition position) {
		this.position = position;
	}

	public void setPositionIfUnset(QueuePosition position) {
		if (this.position == null) {
			this.position = position;
		}
	}

	public QueueQuery withPosition(QueuePosition position) {
		this.position = position;
		return this;
	}

	public QueueQuery withPositionInUnset(QueuePosition position) {
		if (this.position == null) {
			this.position = position;
		}
		return this;
	}

	public static UUID getConsumerId(String consumer) {
		if (consumer == null) {
			return null;
		}
		if (isUUID(consumer)) {
			return UUID.fromString(consumer);
		} else if (isNotBlank(consumer)) {
			return nameUUIDFromBytes(("consumer:" + consumer).getBytes());
		}
		return null;
	}

	public boolean isSynchronized() {
		return _synchronized;
	}

	public void setSynchronized(boolean _synchronized) {
		this._synchronized = _synchronized;
	}

	public QueueQuery withSynchronized(boolean _synchronized) {
		this._synchronized = _synchronized;
		return this;
	}

	public boolean isUpdate() {
		return update;
	}

	public void setUpdate(boolean update) {
		this.update = update;
	}

	public QueueQuery withUpdate(boolean update) {
		this.update = update;
		return this;
	}

	@Override
	public String toString() {
		return "QueueQuery [consumerId=" + consumerId + ", lastTimestamp="
				+ lastTimestamp + ", lastMessageId=" + lastMessageId
				+ ", previousCount=" + previousCount + ", nextCount="
				+ nextCount + ", limit=" + limit + ", position=" + position
				+ ", update=" + update + "]";
	}

}
