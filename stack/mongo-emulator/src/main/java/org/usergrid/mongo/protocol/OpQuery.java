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
package org.usergrid.mongo.protocol;

import static org.apache.commons.collections.MapUtils.getIntValue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.antlr.runtime.ClassicToken;
import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.usergrid.mongo.utils.BSONUtils;
import org.usergrid.persistence.Query;
import org.usergrid.persistence.Query.SortDirection;
import org.usergrid.persistence.query.tree.AndOperand;
import org.usergrid.persistence.query.tree.Equal;
import org.usergrid.persistence.query.tree.Operand;
import org.usergrid.persistence.query.tree.OrOperand;

public class OpQuery extends OpCrud {

	int flags;
	int numberToSkip;
	int numberToReturn;
	BSONObject query;
	BSONObject returnFieldSelector;

	static Set<String> operators = new HashSet<String>();
	{
		operators.add("all");
		operators.add("and");
		operators.add("elemMatch");
		operators.add("exists");
		operators.add("gt");
		operators.add("gte");
		operators.add("in");
		operators.add("lt");
		operators.add("lte");
		operators.add("mod");
		operators.add("ne");
		operators.add("nin");
		operators.add("nor");
		operators.add("not");
		operators.add("or");
		operators.add("regex");
		operators.add("size");
		operators.add("type");
		operators.add("where");
	}

	public OpQuery() {
		opCode = OP_QUERY;
	}

	public Query toNativeQuery() {
		if (query == null) {
			return null;
		}

		BasicBSONObject query_expression = null;
		BasicBSONObject sort_order = null;

		Object o = query.get("$query");
		if (!(o instanceof BasicBSONObject)) {
			o = query.get("query");
		}
		if (o instanceof BasicBSONObject) {
			query_expression = (BasicBSONObject) o;
		}

		o = query.get("$orderby");
		if (!(o instanceof BasicBSONObject)) {
			o = query.get("orderby");
		}
		if (o instanceof BasicBSONObject) {
			sort_order = (BasicBSONObject) o;
		}

		if ((query_expression == null) && (sort_order == null)) {
			return null;
		}

		if (query_expression.size() == 0) {
			if (sort_order.size() == 0) {
				return null;
			}
			if ((sort_order.size() == 1) && sort_order.containsField("_id")) {
				return null;
			}
		}

		Query q = new Query();
		if (getNumberToReturn() > 0) {
			q.setLimit(getNumberToReturn());
		}

		if (query_expression != null) {
			AndOperand and = new AndOperand();
			append(and, query_expression);
			q.setRootOperand(and);
		}

		if (sort_order != null) {
			for (String sort : sort_order.keySet()) {
				if (!"_id".equals(sort)) {
					int s = getIntValue(sort_order.toMap(), "_id", 1);
					q.addSort(sort, s >= 0 ? SortDirection.ASCENDING
							: SortDirection.DESCENDING);
				}
			}
		}

		return q;
	}

	Operand append(Operand op, BSONObject exp) {
		for (String field : exp.keySet()) {
			if (field.startsWith("$")) {
				if ("$or".equals(field)) {
					OrOperand or = new OrOperand(new ClassicToken(0, "="));
					op.addChild(or);
					append(or, (BSONObject) exp.get(field));
				}
			} else if (!field.equals("_id")) {
				Equal equality = new Equal(new ClassicToken(0, "="));
				equality.setProperty(field);
				equality.setLiteral(exp.get(field));
				op.addChild(equality);
			}
		}
		return op;
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public int getNumberToSkip() {
		return numberToSkip;
	}

	public void setNumberToSkip(int numberToSkip) {
		this.numberToSkip = numberToSkip;
	}

	public int getNumberToReturn() {
		return numberToReturn;
	}

	public void setNumberToReturn(int numberToReturn) {
		this.numberToReturn = numberToReturn;
	}

	public BSONObject getQuery() {
		return query;
	}

	public void setQuery(BSONObject query) {
		this.query = query;
	}

	public void setQuery(Map<?, ?> map) {
		query = new BasicBSONObject();
		query.putAll(map);
	}

	public BSONObject getReturnFieldSelector() {
		return returnFieldSelector;
	}

	public void setReturnFieldSelector(BSONObject returnFieldSelector) {
		this.returnFieldSelector = returnFieldSelector;
	}

	public void setReturnFieldSelector(Map<?, ?> map) {
		returnFieldSelector = new BasicBSONObject();
		returnFieldSelector.putAll(map);
	}

	@Override
	public void decode(ChannelBuffer buffer) throws IOException {
		super.decode(buffer);
		flags = buffer.readInt();
		fullCollectionName = readCString(buffer);
		numberToSkip = buffer.readInt();
		numberToReturn = buffer.readInt();
		query = BSONUtils.decoder().readObject(
				new ChannelBufferInputStream(buffer));
		if (buffer.readable()) {
			returnFieldSelector = BSONUtils.decoder().readObject(
					new ChannelBufferInputStream(buffer));
		}
	}

	@Override
	public ChannelBuffer encode(ChannelBuffer buffer) {
		int l = 28; // 7 ints * 4 bytes

		ByteBuffer fullCollectionNameBytes = getCString(fullCollectionName);
		l += fullCollectionNameBytes.capacity();

		ByteBuffer queryBytes = encodeDocument(query);
		l += queryBytes.capacity();

		ByteBuffer returnFieldSelectorBytes = encodeDocument(returnFieldSelector);
		l += returnFieldSelectorBytes.capacity();

		messageLength = l;

		buffer = super.encode(buffer);

		buffer.writeInt(flags);

		buffer.writeBytes(fullCollectionNameBytes);

		buffer.writeInt(numberToSkip);
		buffer.writeInt(numberToReturn);

		buffer.writeBytes(queryBytes);

		buffer.writeBytes(returnFieldSelectorBytes);

		return buffer;
	}

	@Override
	public String toString() {
		return "OpQuery [flags=" + flags + ", fullCollectionName="
				+ fullCollectionName + ", numberToSkip=" + numberToSkip
				+ ", numberToReturn=" + numberToReturn + ", query=" + query
				+ ", returnFieldSelector=" + returnFieldSelector + "]";
	}

}
