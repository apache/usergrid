/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.corepersistence.export;


import com.google.common.base.Optional;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * A builder interface to build our re-index request
 */
public interface ExportRequestBuilder {

    /**
     * Set the application id
     */
    ExportRequestBuilder withApplicationId(final UUID applicationId);


    /**
     * Get the application scope
     * @return
     */
    Optional<ApplicationScope> getApplicationScope();

}
