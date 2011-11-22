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
package org.usergrid.persistence;

import java.util.List;
import java.util.UUID;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

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
}
