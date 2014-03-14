/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.cassandra.util;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;


/** @author zznate */
public class TraceTag implements Iterable<TimedOpTag> {

    private final UUID tag;
    private final String name;
    private final String traceName;
    private final List<TimedOpTag> timedOps;
    private final boolean metered;


    private TraceTag( UUID tag, String name, boolean metered ) {
        this.tag = tag;
        this.name = name;
        this.metered = metered;
        traceName = new StringBuilder( this.tag.toString() ).append( "-" ).append( this.metered ).append( "-" )
                                                            .append( this.name ).toString();
        timedOps = new ArrayList<TimedOpTag>();
    }


    public static TraceTag getInstance( UUID tag, String name ) {
        return new TraceTag( tag, name, false );
    }


    public static TraceTag getMeteredInstance( UUID tag, String name ) {
        return new TraceTag( tag, name, true );
    }


    public String getTraceName() {
        return traceName;
    }


    public void add( TimedOpTag timedOpTag ) {
        timedOps.add( timedOpTag );
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


    /** The number of {@link TimedOpTag} instances currently held */
    public int getOpCount() {
        return timedOps.size();
    }


    /** Remove the currently held {@link TimedOpTag} instances */
    public void removeOps() {
        timedOps.clear();
    }
}
