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
package org.apache.usergrid.chop.stack;


import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;


/**
 * Represents the setup state of a stack
 */
public enum SetupStackState {
    // JarNotFound ==> (setup signal) ==> SettingUp
    // JarNotFound ==> (deploy signal) ==> NotSetUp
    JarNotFound( 5, new SetupStackSignal[] { SetupStackSignal.DEPLOY, SetupStackSignal.SETUP },
            new Integer[] { 3, 1 },
            "No runner jars found with given parameters, deploy first.",
            "%s signal rejected. When JarNotFound only DEPLOY and SETUP signal(s) which cause to transition into " +
                    "NotSetUp and SettingUp state(s) respectively" ),

    // Destroying ==> (Complete signal) ==> NotSetUp
    Destroying( 4, new SetupStackSignal[] { SetupStackSignal.COMPLETE },
            new Integer[] { 3 },
            "Currently being destroyed. Wait until it is finished to set up again.",
            "%s signal rejected. When Destroying only COMPLETE signal(s) can be sent which cause to " +
                    "transition into NotSetUp state(s) respectively" ),

    // NotSetUp ==> (deploy signal) ==> NotSetUp
    // NotSetUp ==> (setup signal) ==> SettingUp
    NotSetUp( 3, new SetupStackSignal[] { SetupStackSignal.SETUP, SetupStackSignal.DEPLOY },
            new Integer[] { 1, 3 },
            "Jar is deployed but no stack set up with it.",
            "%s signal rejected. When NotSetUp only SETUP and DEPLOY signal(s) which cause to transition into " +
                    "SettingUp and NotSetUp state(s) respectively" ),

    // SetupFailed ==> (setup deploy) ==> NotSetUp
    // SetupFailed ==> (setup signal) ==> SettingUp
    SetupFailed( 2, new SetupStackSignal[] { SetupStackSignal.SETUP, SetupStackSignal.DEPLOY },
            new Integer[] { 1, 3 },
            "Stack was registered, however its setup failed. Call setup again to restart.",
            "%s signal rejected. When SetupFailed only SETUP and DEPLOY signal(s) which cause to transition into " +
                    "SettingUp and NotSetUp state(s) respectively" ),

    // SettingUp ==> (Fail signal) ==> SetupFailed
    // SettingUp ==> (Complete signal) ==> SetUp
    SettingUp( 1, new SetupStackSignal[] { SetupStackSignal.COMPLETE, SetupStackSignal.FAIL},
            new Integer[] { 0, 2 },
            "Setting up the stack right now.",
            "%s signal rejected. When SettingUp only COMPLETE and FAIL signal(s) can be sent which " +
                    "cause to transition into SetUp and SetupFailed state(s) respectively" ),

    // SetUp ==> (destroy signal) ==> NotSetUp
    SetUp( 0, new SetupStackSignal[] { SetupStackSignal.DESTROY },
            new Integer[] { 4 },
            "Stack is set up and ready to start the tests.",
            "%s signal rejected. When SetUp only DESTROY signal(s) which cause to transition into " +
                    "NotSetUp state(s) respectively" );

    private static final Logger LOG = LoggerFactory.getLogger( SetupStackState.class );
    private static final String SUCCESS_MSG = "%s signal accepted, transitioning from %s state to %s";

    private final String stackStateMessage;
    private final int stateID;
    private final Map<SetupStackSignal, Integer> signalsCorrespondingStateIDs;
    private final Set<SetupStackSignal> acceptedStates;
    private final String rejectedMessage;


    private SetupStackState( int stateID, SetupStackSignal[] signals, Integer[] states, String stackStateMessage,
            String rejectedMessage ) {
        Assert.assertTrue( states.length == signals.length );
        this.stackStateMessage = stackStateMessage;
        this.stateID = stateID;
        this.rejectedMessage = rejectedMessage;
        signalsCorrespondingStateIDs = getCorrespondingStateIDs( signals, states );
        acceptedStates = getAcceptedStates( signals );
    }


    public String getStackStateMessage() {
        return stackStateMessage;
    }

    public String getMessage( SetupStackSignal signal ) {
        Formatter formatter = new Formatter();

        if ( accepts( signal ) ) {
            return formatter.format( SUCCESS_MSG, new Object[] { signal, this, next( signal ) } ).toString();
        }

        return formatter.format( rejectedMessage, signal ).toString();
    }

    public int getStateID() {
        return stateID;
    }


    /**
     * Check to see if the state accepts a signal: meaning is the signal a
     * valid signal to produce a state transition.
     *
     * @param signal the signal to check
     * @return true if the signal will be accepted, false otherwise
     */
    public boolean accepts( SetupStackSignal signal ) {
        if ( signal == null || acceptedStates == null)
            return false;
        return acceptedStates.contains( signal );
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
    public boolean accepts( SetupStackSignal signal, SetupStackState next ) {
        if ( signal == null || next == null ) {
            return false;
        }
        if ( acceptedStates == null ) {
            return false;
        }
        if ( ! acceptedStates.contains( signal ) ) {
            return false;
        }

        Integer stateID = signalsCorrespondingStateIDs.get( signal );
        if ( stateID == null ) {
            return false;
        }

        SetupStackState realNext = get( stateID );

        if ( realNext == null ) {
            return false;
        }

        return realNext.equals( next );
    }


    public SetupStackState get( Integer stateID ) {
        Preconditions.checkNotNull( stateID, "The stateID cannot be null: state = {}", toString() );

        switch ( stateID ) {
            case 0:
                return SetUp;
            case 1:
                return SettingUp;
            case 2:
                return SetupFailed;
            case 3:
                return NotSetUp;
            case 4:
                return Destroying;
            case 5:
                return JarNotFound;
        }

        throw new RuntimeException( "Should never get here!" );
    }


    public SetupStackState next( SetupStackSignal signal ) {
        if ( signal == null ) {
            return null;
        }

        Integer stateID = signalsCorrespondingStateIDs.get( signal );

        if ( stateID == null ) {
            return null;
        }

        LOG.debug( "Got signal {} in {} state: stateID = " + stateID, signal, toString() );

        return get( stateID );
    }


    private static Map<SetupStackSignal, Integer> getCorrespondingStateIDs( SetupStackSignal[] signals,
                                                                            Integer[] states ) {
        Assert.assertTrue( signals.length == states.length );
        if ( signals.length == 0 ) {
            return null;
        }
        Map<SetupStackSignal, Integer> signalsCorrespondingStateIDs = new HashMap<SetupStackSignal, Integer>( signals.length );

        for ( int ii = 0; ii < signals.length; ii++ ) {
            signalsCorrespondingStateIDs.put( signals[ii], states[ii] );
        }

        return Collections.unmodifiableMap( signalsCorrespondingStateIDs );
    }


    public Set<SetupStackSignal> getAcceptedStates( SetupStackSignal[] signals ) {
        if ( signals.length == 0 ) {
            return null;
        }
        Set<SetupStackSignal> acceptedStates = new HashSet<SetupStackSignal>( signals.length );
        Collections.addAll( acceptedStates, signals );
        return acceptedStates;
    }
}
