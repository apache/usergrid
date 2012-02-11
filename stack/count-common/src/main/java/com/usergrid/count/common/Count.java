package com.usergrid.count.common;

import org.apache.commons.lang.StringUtils;

/**
 * Loosely models a 'count' of things to
 * @author zznate
 */
public class Count {
    private final String keyName;
    private final String columnName;
    private int value;

    public Count(String keyName, String columnName, int value) {
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
