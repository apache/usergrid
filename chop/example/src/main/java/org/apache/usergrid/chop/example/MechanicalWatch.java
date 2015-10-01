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
package org.apache.usergrid.chop.example;


import com.google.common.base.Preconditions;
import com.google.inject.Inject;


/**
 * A mechanical watch powered by a Mainspring.
 */
public class MechanicalWatch implements Watch {
    private Mainspring spring;


    @Inject
    public void addPowerSource( PowerSource powerSource ) {
        Preconditions.checkState( powerSource.hasPower(), "Make sure the spring is wound before starting." );
        this.spring = ( Mainspring ) powerSource;
    }


    public void wind( long amount ) {
        spring.windSpring( amount );
    }


    @Override
    public long getTime() {
        Preconditions.checkState( spring.hasPower(), "Can't get the time if the spring is not wound." );
        return System.currentTimeMillis();
    }


    @Override
    public boolean isDead() {
        return ! spring.hasPower();
    }


    @Override
    public Type getType() {
        return Type.MECHANICAL;
    }
}
