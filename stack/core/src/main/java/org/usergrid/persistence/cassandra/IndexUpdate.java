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
package org.usergrid.persistence.cassandra;

import static java.nio.ByteBuffer.wrap;
import static java.util.Arrays.asList;
import static org.usergrid.utils.JsonUtils.toJsonNode;
import static org.usergrid.utils.UUIDUtils.getTimestampInMicros;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.mutation.Mutator;

import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;

import com.fasterxml.uuid.UUIDComparator;

public class IndexUpdate {

	private static final Logger logger = LoggerFactory
			.getLogger(IndexUpdate.class);

	public static final byte VALUE_CODE_BYTES = 0;
	public static final byte VALUE_CODE_UTF8 = 1;
	public static final byte VALUE_CODE_UUID = 2;
	public static final byte VALUE_CODE_INT = 3;
	public static final byte VALUE_CODE_MAX = 127;

	public static int INDEX_STRING_VALUE_LENGTH = 1024;

	private Mutator<ByteBuffer> batch;
	private Entity entity;
	private String entryName;
	private Object entryValue;
	private final List<IndexEntry> prevEntries = new ArrayList<IndexEntry>();
	private final List<IndexEntry> newEntries = new ArrayList<IndexEntry>();
	private final Set<String> indexesSet = new LinkedHashSet<String>();
	private boolean schemaHasProperty;
	private boolean isMultiValue;
	private boolean removeListEntry;
	private long timestamp;
	private final UUID timestampUuid;
	private UUID associatedId;

	public IndexUpdate(Mutator<ByteBuffer> batch, Entity entity,
			String entryName, Object entryValue, boolean schemaHasProperty,
			boolean isMultiValue, boolean removeListEntry, UUID timestampUuid) {
		this.batch = batch;
		this.entity = entity;
		this.entryName = entryName;
		this.entryValue = entryValue;
		this.schemaHasProperty = schemaHasProperty;
		this.isMultiValue = isMultiValue;
		this.removeListEntry = removeListEntry;
		timestamp = getTimestampInMicros(timestampUuid);
		this.timestampUuid = timestampUuid;
	}

	public Mutator<ByteBuffer> getBatch() {
		return batch;
	}

	public void setBatch(Mutator<ByteBuffer> batch) {
		this.batch = batch;
	}

	public Entity getEntity() {
		return entity;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	public UUID getId() {
		if (associatedId != null) {
			return associatedId;
		}
		return entity.getUuid();
	}

	public String getEntryName() {
		return entryName;
	}

	public void setEntryName(String entryName) {
		this.entryName = entryName;
	}

	public Object getEntryValue() {
		return entryValue;
	}

	public void setEntryValue(Object entryValue) {
		this.entryValue = entryValue;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public UUID getTimestampUuid() {
		return timestampUuid;
	}

	public List<IndexEntry> getPrevEntries() {
		return prevEntries;
	}

	public void addPrevEntry(String path, Object value, UUID timestamp) {
		IndexEntry entry = new IndexEntry(path, value, timestamp);
		prevEntries.add(entry);

	}

	public List<IndexEntry> getNewEntries() {
		return newEntries;
	}

	public void addNewEntry(String path, Object value) {
		IndexEntry entry = new IndexEntry(path, value, timestampUuid);
		newEntries.add(entry);
	}

	public Set<String> getIndexesSet() {
		return indexesSet;
	}

	public void addIndex(String index) {
		logger.debug("Indexing {}",  index);
		indexesSet.add(index);
	}

	public boolean isSchemaHasProperty() {
		return schemaHasProperty;
	}

	public void setSchemaHasProperty(boolean schemaHasProperty) {
		this.schemaHasProperty = schemaHasProperty;
	}

	public boolean isMultiValue() {
		return isMultiValue;
	}

	public void setMultiValue(boolean isMultiValue) {
		this.isMultiValue = isMultiValue;
	}

	public boolean isRemoveListEntry() {
		return removeListEntry;
	}

	public void setRemoveListEntry(boolean removeListEntry) {
		this.removeListEntry = removeListEntry;
	}

	public void setAssociatedId(UUID associatedId) {
		this.associatedId = associatedId;
	}

	public UUID getAssociatedId() {
		return associatedId;
	}

	public class IndexEntry {
		private final byte code;
		private String path;
		private final Object value;
		private final UUID timestampUuid;

		public IndexEntry(String path, Object value, UUID timestampUuid) {
			this.path = path;
			this.value = value;
			code = indexValueCode(value);
			this.timestampUuid = timestampUuid;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public Object getValue() {
			return value;
		}

		public byte getValueCode() {
			return code;
		}

		public UUID getTimestampUuid() {
			return timestampUuid;
		}

		public DynamicComposite getIndexComposite() {
			return new DynamicComposite(code, value, getId(), timestampUuid);
		}

		public DynamicComposite getIndexComposite(Object... ids) {
			return new DynamicComposite(code, value, asList(ids), timestampUuid);
		}

	}
	
	public static class UniqueIndexEntry {
        private final byte code;
        private String path;
        private final Object value;

        public UniqueIndexEntry(String path, Object value) {
            this.path = path;
            this.value = value;
            code = indexValueCode(value);
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Object getValue() {
            return value;
        }

        public byte getValueCode() {
            return code;
        }

      
        public DynamicComposite getIndexComposite() {
            return new DynamicComposite(code, value);
        }


    }

	private static String prepStringForIndex(String str) {
		str = str.trim().toLowerCase();
		str = str.substring(0,
				Math.min(INDEX_STRING_VALUE_LENGTH, str.length()));
		return str;
	}

	/**
	 * @param obj
	 * @return
	 */
	public static Object toIndexableValue(Object obj) {
		if (obj == null) {
			return null;
		}

		if (obj instanceof String) {
			return prepStringForIndex((String) obj);
		}

		// UUIDs, and BigIntegers map to Cassandra UTF8Type and IntegerType
		if ((obj instanceof UUID) || (obj instanceof BigInteger)) {
			return obj;
		}

		// For any numeric values, turn them into a long
		// and make them BigIntegers for IntegerType
		if (obj instanceof Number) {
			return BigInteger.valueOf(((Number) obj).longValue());
		}

		if (obj instanceof Boolean) {
			return BigInteger.valueOf(((Boolean) obj) ? 1L : 0L);
		}

		if (obj instanceof Date) {
			return BigInteger.valueOf(((Date) obj).getTime());
		}

		if (obj instanceof byte[]) {
			return wrap((byte[]) obj);
		}

		if (obj instanceof ByteBuffer) {
			return obj;
		}

		JsonNode json = toJsonNode(obj);
		if ((json != null) && json.isValueNode()) {
			if (json.isBigInteger()) {
				return json.getBigIntegerValue();
			} else if (json.isNumber() || json.isBoolean()) {
				return BigInteger.valueOf(json.getValueAsLong());
			} else if (json.isTextual()) {
				return prepStringForIndex(json.getTextValue());
			} else if (json.isBinary()) {
				try {
					return wrap(json.getBinaryValue());
				} catch (IOException e) {
				}
			}
		}

		return null;
	}

	public static boolean validIndexableValue(Object obj) {
		return toIndexableValue(obj) != null;
	}

	public static boolean validIndexableValueOrJson(Object obj) {
		if ((obj instanceof Map) || (obj instanceof List)
				|| (obj instanceof JsonNode)) {
			return true;
		}
		return toIndexableValue(obj) != null;
	}

	public static byte indexValueCode(Object obj) {
		obj = toIndexableValue(obj);
		if (obj instanceof String) {
			return VALUE_CODE_UTF8;
		} else if (obj instanceof UUID) {
			return VALUE_CODE_UUID;
		} else if (obj instanceof BigInteger) {
			return VALUE_CODE_INT;
		} else if (obj instanceof Number) {
			return VALUE_CODE_INT;
		} else {
			return VALUE_CODE_BYTES;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int compareIndexedValues(Object o1, Object o2) {
		o1 = toIndexableValue(o1);
		o2 = toIndexableValue(o2);
		if ((o1 == null) && (o2 == null)) {
			return 0;
		} else if (o1 == null) {
			return -1;
		} else if (o2 == null) {
			return 1;
		}
		int c1 = indexValueCode(o1);
		int c2 = indexValueCode(o2);
		if (c1 == c2) {
			if (o1 instanceof UUID) {
				UUIDComparator.staticCompare((UUID) o1, (UUID) o2);
			} else if (o1 instanceof Comparable) {
				return ((Comparable) o1).compareTo(o2);
			}
		}
		return c1 - c2;
	}

}
