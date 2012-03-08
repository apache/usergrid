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
package org.usergrid.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Query.FilterPredicate;
import org.usergrid.persistence.Query.SortDirection;
import org.usergrid.persistence.cassandra.QueryProcessor;
import org.usergrid.utils.JsonUtils;

public class QueryTest {

	private static final Logger logger = LoggerFactory
			.getLogger(QueryTest.class);

	@SuppressWarnings("unchecked")
	@Test
	public void testQuery() throws Exception {
		logger.info("testQuery");

		Query q = new Query();
		q.addFilter("blah");
		q.addFilter("a=5");
		q.addFilter("b='hello'");
		q.addFilter("c < 7");
		q.addFilter("d gt 5");
		q.addFilter("e in 5,6");
		q.addFilter("f=6.0");
		q.addFilter("g=.05");
		q.addFilter("loc within .05 of 5,6");

		Iterator<FilterPredicate> i = q.getFilterPredicates().iterator();

		FilterPredicate f = i.next();
		testPredicate(f, "a", Query.FilterOperator.EQUAL, new Long(5));

		f = i.next();
		testPredicate(f, "b", Query.FilterOperator.EQUAL, "hello");

		f = i.next();
		testPredicate(f, "c", Query.FilterOperator.LESS_THAN, new Long(7));

		f = i.next();
		testPredicate(f, "d", Query.FilterOperator.GREATER_THAN, new Long(5));

		f = i.next();
		testPredicate(f, "e", Query.FilterOperator.IN,
				Arrays.asList(new Long(5), new Long(6)));

		f = i.next();
		testPredicate(f, "f", Query.FilterOperator.EQUAL, new Float(6));

		f = i.next();
		testPredicate(f, "g", Query.FilterOperator.EQUAL, new Float(.05));

		f = i.next();
		testPredicate(f, "loc", Query.FilterOperator.WITHIN,
				Arrays.asList(new Float(.05), new Long(5), new Long(6)));

		q = Query.fromQL("select * where a = 5");
		i = q.getFilterPredicates().iterator();
		f = i.next();
		testPredicate(f, "a", Query.FilterOperator.EQUAL, new Long(5));
		logger.info(q.toString());

		q = Query.fromQL("select * where a = 5 and b = \'hello\'");
		i = q.getFilterPredicates().iterator();
		f = i.next();
		testPredicate(f, "a", Query.FilterOperator.EQUAL, new Long(5));
		f = i.next();
		testPredicate(f, "b", Query.FilterOperator.EQUAL, "hello");
		logger.info(q.toString());

		q = Query.fromQL("select * where a = 5 and b = \'hello\' and c<7");
		i = q.getFilterPredicates().iterator();
		f = i.next();
		testPredicate(f, "a", Query.FilterOperator.EQUAL, new Long(5));
		f = i.next();
		testPredicate(f, "b", Query.FilterOperator.EQUAL, "hello");
		f = i.next();
		testPredicate(f, "c", Query.FilterOperator.LESS_THAN, new Long(7));
		logger.info(q.toString());

		q = Query.fromQL("order by a asc");
		assertNotNull(q.getSortPredicates());
		assertEquals(1, q.getSortPredicates().size());
		assertEquals("a", q.getSortPredicates().get(0).getPropertyName());

		q = Query.fromQL("order by a,b desc");
		assertNotNull(q.getSortPredicates());
		assertEquals(2, q.getSortPredicates().size());
		assertEquals("a", q.getSortPredicates().get(0).getPropertyName());
		assertEquals("b", q.getSortPredicates().get(1).getPropertyName());
		assertEquals(SortDirection.DESCENDING, q.getSortPredicates().get(1)
				.getDirection());

		q = Query.fromQL("select * where loc within 5 of 6,7");
		i = q.getFilterPredicates().iterator();
		f = i.next();
		testPredicate(f, "loc", Query.FilterOperator.WITHIN,
				Arrays.asList(new Long(5), new Long(6), new Long(7)));
		logger.info(q.toString());

	}

	public void testPredicate(FilterPredicate f, String name,
			Query.FilterOperator op, Object val) {
		logger.info("Checking filter: " + f);
		assertEquals("Predicate property name not correct", name,
				f.getPropertyName());
		assertEquals("first predicate operator not correct", op,
				f.getOperator());
		assertEquals("first predicate value not correct", val, f.getValue());

	}

	@Test
	public void testFromJson() {
		String s = "{\"filter\":\"a contains 'ed'\"}";
		Query q = Query.fromJsonString(s);
		assertNotNull(q);
		logger.info(JsonUtils.mapToFormattedJsonString(q.getFilterPredicates()));

		s = "asdfasdg";
		q = Query.fromJsonString(s);
		assertNull(q);
	}

	@Test
	public void testQueryProcessor() {
		logger.info("testQueryProcessor");
		Query q = new Query();
		q.addFilter("a<10");
		q.addFilter("a>5");
		QueryProcessor qp = new QueryProcessor(q);
		testIntRange(5, false, 10, false, qp, 1);

		q = new Query();
		q.addFilter("a<=10");
		q.addFilter("a>=5");
		qp = new QueryProcessor(q);
		testIntRange(5, true, 10, true, qp, 1);

		q = new Query();
		q.addFilter("a<=10");
		q.addFilter("a>3");
		q.addFilter("a>=5");
		qp = new QueryProcessor(q);
		testIntRange(3, false, 10, true, qp, 1);

		q = new Query();
		q.addFilter("a<=10");
		q.addFilter("a>5");
		q.addFilter("a>=5");
		qp = new QueryProcessor(q);
		testIntRange(5, true, 10, true, qp, 1);

		q = new Query();
		q.addFilter("a<=10");
		q.addFilter("a>5");
		q.addFilter("a>=5");
		q.addFilter("a<10");
		qp = new QueryProcessor(q);
		testIntRange(5, true, 10, true, qp, 1);

		q = new Query();
		q.addFilter("name > 'bob'");
		q.addFilter("name <= 'david'");
		qp = new QueryProcessor(q);
		testStringRange("bob", false, "david", true, qp, 1);
	}

	public void testIntRange(int start, boolean startInclusive, int finish,
			boolean finishInclusive, QueryProcessor qp, int count) {
		assertEquals(count, qp.getSlices().size());
		assertEquals(BigInteger.valueOf(start), qp.getSlices().get(0)
				.getStart().getValue());
		assertEquals(startInclusive, qp.getSlices().get(0).getStart()
				.isInclusive());
		assertEquals(BigInteger.valueOf(finish), qp.getSlices().get(0)
				.getFinish().getValue());
		assertEquals(finishInclusive, qp.getSlices().get(0).getFinish()
				.isInclusive());
	}

	public void testStringRange(String start, boolean startInclusive,
			String finish, boolean finishInclusive, QueryProcessor qp, int count) {
		assertEquals(count, qp.getSlices().size());
		assertEquals(start, qp.getSlices().get(0).getStart().getValue());
		assertEquals(startInclusive, qp.getSlices().get(0).getStart()
				.isInclusive());
		assertEquals(finish, qp.getSlices().get(0).getFinish().getValue());
		assertEquals(finishInclusive, qp.getSlices().get(0).getFinish()
				.isInclusive());
	}
}
