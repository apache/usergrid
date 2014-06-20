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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;


/**
 * Represents the setup state of a stack
 */
public enum SetupStackState {
    // After tests finishes, it will automatically switch to SetUp state
    // Running ==> (Stop signal) ==> Stopped
    Running( 7, new SetupStackSignal[] { SetupStackSignal.STOP }, new Integer[] { 6 },
            "Running tests on stack.",
            "%s signal rejected. When Running only a STOP signal(s) which cause to transition into " +
                    "Stopped state(s) respectively" +
                    " or it will automatically transition into SetUp state when tests are finished" ),

    // Stopped ==> (reset signal) ==> SetUp
    Stopped( 6, new SetupStackSignal[] { SetupStackSignal.RESET }, new Integer[] { 0 },
            "Tests are stopped while running, please reset first.",
            "%s signal rejected. When Stopped only a RESET signal(s) which cause to transition into " +
                    "SetUp state(s) respectively" ),

    // JarNotFound ==> (deploy signal) ==> JarAlreadyDeployed
    JarNotFound( 5, new SetupStackSignal[] { SetupStackSignal.DEPLOY }, new Integer[] { 3 },
            "No runner jars found with given parameters, deploy first.",
            "%s signal rejected. When JarNotFound only a DEPLOY signal(s) which cause to transition into " +
                    "JarAlreadyDeployed state(s) respectively" ),

    // After destroy finishes, it will automatically switch to NotSetUp state
    Destroying( 4, new SetupStackSignal[] { }, new Integer[] { },
            "Currently being destroyed. Wait until it is finished to set up again.",
            "%s signal rejected. When Destroying no Signal can be sent. It transitions into " +
                    "NotSetUp state automatically when it finishes." ),

    // NotSetUp ==> (setup signal) ==> NotSetUp
    // NotSetUp ==> (setup signal) ==> SettingUp
    NotSetUp( 3, new SetupStackSignal[] { SetupStackSignal.SETUP, SetupStackSignal.DEPLOY }, new Integer[] { 1, 3 },
            "Jar is deployed but no stack set up with it.",
            "%s signal rejected. When NotSetUp only a SETUP and DEPLOY signal(s) which cause to transition into " +
                    "SettingUp and NotSetUp state(s) respectively" ),

    // SetupFailed ==> (setup deploy) ==> NotSetUp
    // SetupFailed ==> (setup signal) ==> SettingUp
    SetupFailed( 2, new SetupStackSignal[] { SetupStackSignal.SETUP, SetupStackSignal.DEPLOY }, new Integer[] { 1 },
            "Stack was registered, however its setup failed. Call setup again to restart.",
            "%s signal rejected. When SetupFailed only a SETUP and DEPLOY signal(s) which cause to transition into " +
                    "SettingUp and NotSetUp state(s) respectively" ),

    // After setting up the stack finishes, it will automatically switch to SetUp state
    SettingUp( 1, new SetupStackSignal[] { }, new Integer[] { },
            "Setting up the stack right now.",
            "%s signal rejected. When SettingUp no Signal can be sent. It transitions into " +
                    "SetUp state automatically when it finishes." ),

    // SetUp ==> (start signal) ==> Running
    // SetUp ==> (destroy signal) ==> NotSetUp
    SetUp( 0, new SetupStackSignal[] { SetupStackSignal.DESTROY, SetupStackSignal.START }, new Integer[] { 4, 7 },
            "Stack is set up and ready to start the tests.",
            "%s signal rejected. When SetUp only a DESTROY and SetUp signal(s) which cause to transition into " +
                    "NotSetUp and SetUp state(s) respectively" );

    private static final Logger LOG = LoggerFactory.getLogger( SetupStackState.class );
    private static final String SUCCESS_MSG = "%s signal accepted, transitioning from %s state to %s";

    private final String stackStateMessage;
    private final int stateID;
    private final Map<SetupStackSignal, Integer> correspondingStateIDs;
    private final Set<SetupStackSignal> acceptedStates;
    private final String rejectedMessage;



    private SetupStackState( int stateID, SetupStackSignal[] signals, Integer[] states, String stackStateMessage, String rejectedMessage ) {
        this.stackStateMessage = stackStateMessage;
        this.stateID = stateID;
        this.rejectedMessage = rejectedMessage;
        correspondingStateIDs = getCorrespondingStateIDs( signals, states );
        acceptedStates = new HashSet<SetupStackSignal>( signals.length );
        Collections.addAll( acceptedStates, signals );
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
        Preconditions.checkNotNull( signal, "Signal parameter cannot be null: state = {}", toString() );
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

        if ( ! acceptedStates.contains( signal ) ) {
            return false;
        }

        Integer stateID = correspondingStateIDs.get( signal );
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
            case 6:
                return Stopped;
            case 7:
                return Running;
        }

        throw new RuntimeException( "Should never get here!" );
    }


    public SetupStackState next( SetupStackSignal signal ) {
        Preconditions.checkNotNull( signal, "The signal cannot be null: state = {}", toString() );
        Integer stateID = correspondingStateIDs.get( signal );

        LOG.info( "Got signal {} in {} state: stateID = " + stateID, signal, toString() );

        return get( stateID );
    }


    private static Map<SetupStackSignal, Integer> getCorrespondingStateIDs( SetupStackSignal[] signals,
                                                                            Integer[] states ) {
        Map<SetupStackSignal, Integer> correspondingStateIDs = new HashMap<SetupStackSignal, Integer>( signals.length );

        for ( int ii = 0; ii < signals.length; ii++ ) {
            correspondingStateIDs.put( signals[ii], states[ii] );
        }

        return Collections.unmodifiableMap( correspondingStateIDs );
    }
}
