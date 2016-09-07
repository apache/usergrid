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
package org.apache.usergrid.tools.export;

import java.util.UUID;

public class ExportConnection {
    private String organization;
    private String application;
    private String connectionType;
    private UUID sourceUuid;
    private UUID targetUuid;
    public ExportConnection(String organization, String application, String connectionType, UUID sourceUuid, UUID targetUuid) {
        this.organization= organization;
        this.application = application;
        this.connectionType = connectionType;
        this.sourceUuid = sourceUuid;
        this.targetUuid = targetUuid;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public UUID getSourceUuid() {
        return sourceUuid;
    }

    public void setSourceUuid(UUID sourceUuid) {
        this.sourceUuid = sourceUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public void setTargetUuid(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }
}
