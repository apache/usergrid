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
    private final boolean metered;

    private TraceTag(UUID tag, String name, boolean metered) {
        this.tag = tag;
        this.name = name;
        this.metered = metered;
        traceName = new StringBuilder(this.tag.toString())
                .append("-")
                .append(this.metered)
                .append("-")
                .append(this.name)
                .toString();
        timedOps = new ArrayList<TimedOpTag>();
    }

    public static TraceTag getInstance(UUID tag, String name) {
        return new TraceTag(tag, name, false);
    }

    public static TraceTag getMeteredInstance(UUID tag, String name) {
        return new TraceTag(tag, name, true);
    }

    public String getTraceName() {
        return traceName;
    }

    public void add(TimedOpTag timedOpTag) {
        timedOps.add(timedOpTag);
    }

    public boolean getMetered() {
        return metered;
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
