package com.usergrid.count.common;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JacksonInject;

import static org.codehaus.jackson.annotate.JsonAutoDetect.*;

/**
 * Loosely models a 'count' of things to
 * @author zznate
 */
@JsonAutoDetect(creatorVisibility = Visibility.PUBLIC_ONLY)
public class Count {
    @JsonProperty
    private final String keyName;
    @JsonProperty
    private final String columnName;
    @JsonProperty
    private int value;


    @JsonCreator
    public Count(@JsonProperty(value = "keyName") String keyName,
                 @JsonProperty(value="columnName") String columnName,
                 @JsonProperty(value="value") int value) {
        this.keyName = keyName;
        this.columnName = columnName;
        this.value = value;
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
        return keyName + ":" + columnName;
    }

    public long getValue() {
        return value;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getKeyName() {
        return keyName;
    }

    @Override
    public String toString() {
        return "Counter Name: ".concat(getCounterName()).concat(" value: ").concat(Integer.toString(value));
    }
}
