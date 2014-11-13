package org.apache.usergrid.android.sdk.response;

import java.util.UUID;

public class QueueInfo {

	String path;
	UUID queue;

	public QueueInfo() {
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public UUID getQueue() {
		return queue;
	}

	public void setQueue(UUID queue) {
		this.queue = queue;
	}
}
