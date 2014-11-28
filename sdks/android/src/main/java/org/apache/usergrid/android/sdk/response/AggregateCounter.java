package org.apache.usergrid.android.sdk.response;

import static org.apache.usergrid.android.sdk.utils.JsonUtils.toJsonString;

public class AggregateCounter {

	private long timestamp;
	private long value;

	public AggregateCounter() {
	}

	public AggregateCounter(long timestamp, long value) {
		this.timestamp = timestamp;
		this.value = value;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return toJsonString(this);
	}

}
