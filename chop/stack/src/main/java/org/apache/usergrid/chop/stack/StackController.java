///*
// *  Licensed to the Apache Software Foundation (ASF) under one
// *  or more contributor license agreements.  See the NOTICE file
// *  distributed with this work for additional information
// *  regarding copyright ownership.  The ASF licenses this file
// *  to you under the Apache License, Version 2.0 (the
// *  "License"); you may not use this file except in compliance
// *  with the License.  You may obtain a copy of the License at
// *
// *    http://www.apache.org/licenses/LICENSE-2.0
// *
// *  Unless required by applicable law or agreed to in writing,
// *  software distributed under the License is distributed on an
// *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// *  KIND, either express or implied.  See the License for the
// *  specific language governing permissions and limitations
// *  under the License.
// *
// */
//package org.apache.usergrid.chop.stack;
//
//
//import org.apache.usergrid.chop.api.State;
//
//import com.google.common.base.Preconditions;
//
//
//public class StackController implements IStackController{
//    private final Object lock = new Object();
//    private SetupStackState state = initializeState();
//
//
//    private SetupStackState initializeState() {
//        // TODO find the real state!!!
//        return SetupStackState.JarNotFound;
//    }
//
//
//    public void send( final SetupStackSignal signal ) {
//        Preconditions.checkState( state.accepts( signal ), state.getMessage( signal ) );
//
//        switch ( signal ) {
//            case STOP: stop(); break;
//            case START: start(); break;
//            case RESET: reset(); break;
//            case DESTROY: destroy(); break;
//            case DEPLOY: deploy(); break;
//            case SETUP: setup(); break;
//            default:
//                throw new IllegalStateException( "Just accepting start, stop, and reset." );
//        }
//    }
//
//    public void reset() {
//        synchronized ( lock ) {
//            Preconditions.checkState( state.accepts( SetupStackSignal.RESET ),
//                    "Cannot reset the controller in state: " + state );
//            state = state.next( SetupStackSignal.RESET );
//        }
//    }
//
//
//    @Override
//    public SetupStackState getState() {
//        return state;
//    }
//
//
//    @Override
//    public void start() {
//        synchronized ( lock ) {
//            Preconditions.checkState( state.accepts( SetupStackSignal.START ), "Cannot start the controller in state: " + state );
//            state = state.next( SetupStackSignal.START );
//            //            new Thread( this ).start();
//            lock.notifyAll();
//        }
//    }
//
//
//    @Override
//    public void stop() {
//        synchronized ( lock ) {
//            Preconditions.checkState( state.accepts( SetupStackSignal.STOP ), "Cannot stop a controller in state: " + state );
//            state = state.next( SetupStackSignal.STOP );
//            lock.notifyAll();
//        }
//    }
//
//
//    @Override
//    public void setup() {
//
//    }
//
//
//    @Override
//    public void deploy() {
//        synchronized ( lock ) {
//            Preconditions.checkState( state.accepts( SetupStackSignal.DEPLOY ), "Cannot deploy jar while controller in state: " + state );
//            state = state.next( SetupStackSignal.DEPLOY );
//            lock.notifyAll();
//        }
//
//    }
//
//
//    @Override
//    public void destroy() {
//
//    }
//}
