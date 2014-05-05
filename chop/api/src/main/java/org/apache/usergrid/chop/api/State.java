/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.api;


import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;


/** The runner States and its possible state transitions: hence its state machine. */
public enum State {
    // inactive ==> (load signal) ==> ready
    INACTIVE( 3, new Signal[] { Signal.LOAD }, new Integer[] { 0 },
            "%s signal rejected. When INACTIVE only a Signal.LOAD causes a state change into the READY state." ),

    // stopped ==> (reset signal) ==> ready
    STOPPED( 2, new Signal[] { Signal.RESET }, new Integer[] { 0 },
            "%s signal rejected. When STOPPED only a Signal.RESET causes state change into the READY state." ),

    // running ==> (stop signal) ==> stopped
    // running ==> (completed signal) ==> ready
    RUNNING( 1, new Signal[] { Signal.STOP, Signal.COMPLETED }, new Integer[] { 2, 0 },
            "%s signal rejected. While RUNNING, either a Signal.STOP or Signal.COMPLETED is needed to change state." ),

    // ready ==> (load signal) ==> ready
    // ready ==> (start signal) ==> running
    READY( 0, new Signal[] { Signal.LOAD, Signal.START }, new Integer[] { 0, 1 },
            "%s signal rejected. While READY, either a Signal.LOAD or Signal.START is needed to change state." );

    private static final Logger LOG = LoggerFactory.getLogger( State.class );
    private static final String SUCCESS_MSG = "%s signal accepted, transitioning from %s state to %s";

    private final int id;
    private final Map<Signal, Integer> trantab;
    private final Set<Signal> accepts;
    private final String rejectedMessage;


    private State( int id, Signal[] signals, Integer[] states, String rejectedMessage ) {
        this.id = id;
        this.rejectedMessage = rejectedMessage;
        trantab = getTrantab( signals, states );
        accepts = new HashSet<Signal>( signals.length );
        Collections.addAll( accepts, signals );
    }


    public int getId() {
        return id;
    }


    /**
     * Check to see if the state accepts a signal: meaning is the signal a
     * valid signal to produce a state transition.
     *
     * @param signal the signal to check
     * @return true if the signal will be accepted, false otherwise
     */
    public boolean accepts( Signal signal ) {
        Preconditions.checkNotNull( signal, "Signal parameter cannot be null: state = {}", toString() );
        return accepts.contains( signal );
    }


    /**
     * Gets a informative message based on whether the signal is accepted or rejected.
     *
     * @param signal the signal received
     * @return the informative message
     */
    public String getMessage( Signal signal ) {
        Formatter formatter = new Formatter();

        if ( accepts( signal ) ) {
            return formatter.format( SUCCESS_MSG, new Object[] { signal, this, next( signal ) } ).toString();
        }

        return formatter.format( rejectedMessage, signal ).toString();
    }


    /**
     * Check to see if the state accepts a signal: in other words is the signal a
     * valid signal to produce a state transition and does that transition lead
     * to the supplied 'next' state parameter.
     *
     * @param signal the signal to check
     * @param next the next state to transit to
     * @return true if the signal will be accepted and the next state will be the
     * supplied state, false otherwise
     */
    public boolean accepts( Signal signal, State next ) {
        if ( signal == null || next == null ) {
            return false;
        }

        if ( ! accepts.contains( signal ) ) {
            return false;
        }

        Integer id = trantab.get( signal );
        if ( id == null ) {
            return false;
        }

        State realNext = get( id );

        return realNext != null && realNext.equals( next );
    }


    public State get( Integer id ) {
        Preconditions.checkNotNull( id, "The id cannot be null: state = {}", toString() );

        switch ( id ) {
            case 0:
                return READY;
            case 1:
                return RUNNING;
            case 2:
                return STOPPED;
            case 3:
                return INACTIVE;
        }

        throw new RuntimeException( "Should never get here!" );
    }


    public State next( Signal signal ) {
        Preconditions.checkNotNull( signal, "The signal cannot be null: state = {}", toString() );
        Integer id = trantab.get( signal );

        LOG.info( "Got signal {} in {} state: id = " + id, signal, toString() );

        return get( id );
    }


    private static Map<Signal, Integer> getTrantab( Signal[] signals, Integer[] states ) {
        Map<Signal, Integer> trantab = new HashMap<Signal, Integer>( signals.length );

        for ( int ii = 0; ii < signals.length; ii++ ) {
            trantab.put( signals[ii], states[ii] );
        }

        return Collections.unmodifiableMap( trantab );
    }
}
