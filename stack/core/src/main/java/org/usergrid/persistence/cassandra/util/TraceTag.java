package org.usergrid.persistence.cassandra.util;

import java.util.UUID;

/**
 * @author zznate
 */
public class TraceTag {

    private final UUID tag;
    private final String name;
    private final String traceName;

    private TraceTag(UUID tag, String name) {
        this.tag = tag;
        this.name = name;
        traceName = this.tag.toString() + "-" + this.name;
    }

    public static TraceTag getInstance(UUID tag, String name) {
        return new TraceTag(tag, name);
    }

    public String get() {
        return traceName;
    }

    @Override
    public String toString() {
        return get();
    }

}
