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

import static org.usergrid.utils.ClassUtils.cast;
import static org.usergrid.utils.ListUtils.firstBoolean;
import static org.usergrid.utils.ListUtils.firstInteger;
import static org.usergrid.utils.ListUtils.firstLong;
import static org.usergrid.utils.ListUtils.isEmpty;
import static org.usergrid.utils.MapUtils.toMapList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Query.CounterFilterPredicate;
import org.usergrid.utils.JsonUtils;

public class CounterQuery {

	public static final Logger logger = LoggerFactory.getLogger(CounterQuery.class);

	public static final int DEFAULT_MAX_RESULTS = 10;

	private int limit = 0;
	boolean limitSet = false;

	private Long startTime;
	private Long finishTime;
	private boolean pad;
	private CounterResolution resolution = CounterResolution.ALL;
	private List<String> categories;
	private List<CounterFilterPredicate> counterFilters;

	public CounterQuery() {
	}

	public CounterQuery(CounterQuery q) {
		if (q != null) {
			limit = q.limit;
			limitSet = q.limitSet;
			startTime = q.startTime;
			finishTime = q.finishTime;
			resolution = q.resolution;
			pad = q.pad;
			categories = q.categories != null ? new ArrayList<String>(
					q.categories) : null;
			counterFilters = q.counterFilters != null ? new ArrayList<CounterFilterPredicate>(
					q.counterFilters) : null;
		}
	}

	public static CounterQuery newQueryIfNull(CounterQuery query) {
		if (query == null) {
			query = new CounterQuery();
		}
		return query;
	}

	public static CounterQuery fromJsonString(String json) {
		Object o = JsonUtils.parse(json);
		if (o instanceof Map) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Map<String, List<String>> params = cast(toMapList((Map) o));
			return fromQueryParams(params);
		}
		return null;
	}

	public static CounterQuery fromQueryParams(Map<String, List<String>> params) {

		CounterQuery q = null;
		Integer limit = null;
		Long startTime = null;
		Long finishTime = null;
		Boolean pad = null;
		CounterResolution resolution = null;
		List<CounterFilterPredicate> counterFilters = null;
		List<String> categories = null;

		List<String> l = null;

		limit = firstInteger(params.get("limit"));
		startTime = firstLong(params.get("start_time"));
		finishTime = firstLong(params.get("end_time"));

		l = params.get("resolution");
		if (!isEmpty(l)) {
			resolution = CounterResolution.fromString(l.get(0));
		}

		categories = params.get("category");

		l = params.get("counter");
		if (!isEmpty(l)) {
			counterFilters = CounterFilterPredicate.fromList(l);
		}

		pad = firstBoolean(params.get("pad"));

		if (limit != null) {
			q = newQueryIfNull(q);
			q.setLimit(limit);
		}

		if (startTime != null) {
			q = newQueryIfNull(q);
			q.setStartTime(startTime);
		}

		if (finishTime != null) {
			q = newQueryIfNull(q);
			q.setFinishTime(finishTime);
		}

		if (resolution != null) {
			q = newQueryIfNull(q);
			q.setResolution(resolution);
		}

		if (categories != null) {
			q = newQueryIfNull(q);
			q.setCategories(categories);
		}

		if (counterFilters != null) {
			q = newQueryIfNull(q);
			q.setCounterFilters(counterFilters);
		}

		if (pad != null) {
			q = newQueryIfNull(q);
			q.setPad(pad);
		}

		return q;
	}

	public int getLimit() {
		return getLimit(DEFAULT_MAX_RESULTS);
	}

	public int getLimit(int defaultMax) {
		if (limit <= 0) {
			if (defaultMax > 0) {
				return defaultMax;
			} else {
				return DEFAULT_MAX_RESULTS;
			}
		}
		return limit;
	}

	public void setLimit(int limit) {
		limitSet = true;
		this.limit = limit;
	}

	public CounterQuery withLimit(int limit) {
		limitSet = true;
		this.limit = limit;
		return this;
	}

	public boolean isLimitSet() {
		return limitSet;
	}

	public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	public CounterQuery withStartTime(Long startTime) {
		this.startTime = startTime;
		return this;
	}

	public Long getFinishTime() {
		return finishTime;
	}

	public void setFinishTime(Long finishTime) {
		this.finishTime = finishTime;
	}

	public CounterQuery withFinishTime(Long finishTime) {
		this.finishTime = finishTime;
		return this;
	}

	public boolean isPad() {
		return pad;
	}

	public void setPad(boolean pad) {
		this.pad = pad;
	}

	public CounterQuery withPad(boolean pad) {
		this.pad = pad;
		return this;
	}

	public void setResolution(CounterResolution resolution) {
		this.resolution = resolution;
	}

	public CounterResolution getResolution() {
		return resolution;
	}

	public CounterQuery withResolution(CounterResolution resolution) {
		this.resolution = resolution;
		return this;
	}

	public List<String> getCategories() {
		return categories;
	}

	public CounterQuery addCategory(String category) {
		if (categories == null) {
			categories = new ArrayList<String>();
		}
		categories.add(category);
		return this;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}

	public CounterQuery withCategories(List<String> categories) {
		this.categories = categories;
		return this;
	}

	public List<CounterFilterPredicate> getCounterFilters() {
		return counterFilters;
	}

	public CounterQuery addCounterFilter(String counter) {
		CounterFilterPredicate p = CounterFilterPredicate.fromString(counter);
		if (p == null) {
			return this;
		}
		if (counterFilters == null) {
			counterFilters = new ArrayList<CounterFilterPredicate>();
		}
		counterFilters.add(p);
		return this;
	}

	public void setCounterFilters(List<CounterFilterPredicate> counterFilters) {
		this.counterFilters = counterFilters;
	}

	public CounterQuery withCounterFilters(
			List<CounterFilterPredicate> counterFilters) {
		this.counterFilters = counterFilters;
		return this;
	}

}
