package com.usergrid.count.common;

import me.prettyprint.cassandra.serializers.SerializerTypeInferer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.TypeInferringSerializer;
import me.prettyprint.hector.api.Serializer;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JacksonInject;

import java.nio.ByteBuffer;

import static org.codehaus.jackson.annotate.JsonAutoDetect.*;

/**
 * Loosely models a 'count' of things to
 * @author zznate
 */
@JsonAutoDetect(creatorVisibility = Visibility.PUBLIC_ONLY)
public class Count<K,C> {
    private static final StringSerializer se = StringSerializer.get();

    @JsonProperty
    private final String tableName;
    @JsonProperty
    private final K keyName;
    @JsonProperty
    private final C columnName;
    @JsonProperty
    private long value;

    private Serializer<K> keySerializer;
    private Serializer<C> columnNameSerializer;


    @JsonCreator
    public Count(@JsonProperty(value = "tableName") String tableName,
                 @JsonProperty(value = "keyName") K keyName,
                 @JsonProperty(value="columnName") C columnName,
                 @JsonProperty(value="value") long value) {
        this.tableName = tableName;
        this.keyName = keyName;
        this.columnName = columnName;
        this.value = value;
        this.keySerializer = SerializerTypeInferer.getSerializer(keyName);
        this.columnNameSerializer = SerializerTypeInferer.getSerializer(columnName);
    }

    public Count apply(Count count) {
        if (!StringUtils.equals(count.getCounterName(), getCounterName()) ) {
            throw new IllegalArgumentException("Attempt to apply a counter with a different name");
        }
        this.value += count.getValue();
        return this;
    }

    /**
     * the counter name should uniquely identify the entity being counted.
     * @return
     */
    @JsonIgnore
    public String getCounterName() {
        return tableName + ":" + getKeyName().toString()
                + ":" + getColumnName().toString();
    }

    public long getValue() {
        return value;
    }

    public C getColumnName() {
        return columnName;
    }

    public K getKeyName() {
        return keyName;
    }

    @JsonIgnore
    public ByteBuffer getKeyNameBytes() {
        return keySerializer.toByteBuffer(keyName);
    }

    @JsonIgnore
    public ByteBuffer getColumnNameBytes() {
        return columnNameSerializer.toByteBuffer(columnName);
    }

    @JsonIgnore
    public Serializer<K> getKeySerializer() {
      return keySerializer;
    }

    @JsonIgnore
    public Serializer<C> getColumnNameSerializer() {
      return columnNameSerializer;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public String toString() {
        return "Counter Name: ".concat(getCounterName()).concat(" value: ").concat(Long.toString(value));
    }
}
