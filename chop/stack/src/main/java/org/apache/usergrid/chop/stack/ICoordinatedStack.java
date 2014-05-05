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


import java.util.Collection;

import org.apache.usergrid.chop.api.Commit;
import org.apache.usergrid.chop.api.Module;
import org.apache.usergrid.chop.api.Runner;


public interface ICoordinatedStack extends Stack {


    /**
     * @return Returns the commit object this coordinated stack is related to
     */
    Commit getCommit();


    /**
     * @return Returns the module object this coordinated stack is related to
     */
    Module getModule();


    /**
     * @return Returns the user owning this stack
     */
    User getUser();


    /**
     * @return Returns the state if this stack is already set up
     */
    StackState getState();


    /**
     * @return Returns the setup state of this coordinated stack
     */
    SetupStackState getSetupState();


    /**
     * Note that this may differ from <code>getRunners().size()</code> or <code>getRunnerInstances().size()</code>
     * if this stack has not yet been set up
     *
     * @return Returns the ultimate runner instance count this stack does/will have
     */
    int getRunnerCount();


    /**
     * @return Collection of runners this stack has
     */
    Collection<Runner> getRunners();


    /**
     * @return Collection of runner instances this stack has
     */
    Collection<Instance> getRunnerInstances();
}
