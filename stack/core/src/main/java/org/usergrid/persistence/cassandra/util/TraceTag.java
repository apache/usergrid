package org.usergrid.persistence.cassandra.util;

import java.util.*;

/**
 * @author zznate
 */
public class TraceTag implements Iterable<TimedOpTag> {

    private final UUID tag;
    private final String name;
    private final String traceName;
    private final List<TimedOpTag> timedOps;

    private TraceTag(UUID tag, String name) {
        this.tag = tag;
        this.name = name;
        traceName = this.tag.toString() + "-" + this.name;
        timedOps = new ArrayList<TimedOpTag>();
    }

    public static TraceTag getInstance(UUID tag, String name) {
        return new TraceTag(tag, name);
    }

    public String getTraceName() {
        return traceName;
    }

    public void add(TimedOpTag timedOpTag) {
        timedOps.add(timedOpTag);
    }

    @Override
    public String toString() {
        return getTraceName();
    }

    @Override
    public Iterator iterator() {
        return timedOps.iterator();
    }
}
