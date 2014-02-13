package org.apache.usergrid.java.client.response;

import static org.apache.usergrid.java.client.utils.JsonUtils.toJsonString;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;

public class AggregateCounterSet {
	private String name;
	private UUID user;
	private UUID group;
	private UUID queue;
	private String category;
	private List<AggregateCounter> values;

	public AggregateCounterSet(String name, UUID user, UUID group,
			String category, List<AggregateCounter> values) {
		this.name = name;
		this.user = user;
		this.group = group;
		this.category = category;
		this.values = values;
	}

	public AggregateCounterSet(String name, UUID queue, String category,
			List<AggregateCounter> values) {
		this.name = name;
		setQueue(queue);
		this.category = category;
		this.values = values;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public UUID getUser() {
		return user;
	}

	public void setUser(UUID user) {
		this.user = user;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public UUID getGroup() {
		return group;
	}

	public void setGroup(UUID group) {
		this.group = group;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public List<AggregateCounter> getValues() {
		return values;
	}

	public void setValues(List<AggregateCounter> values) {
		this.values = values;
	}

	@JsonSerialize(include = Inclusion.NON_NULL)
	public UUID getQueue() {
		return queue;
	}

	public void setQueue(UUID queue) {
		this.queue = queue;
	}

	@Override
	public String toString() {
		return toJsonString(this);
	}

}
