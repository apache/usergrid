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
package org.apache.usergrid.persistence.token.impl;


import org.apache.usergrid.persistence.core.migration.schema.Migration;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Serialize token information to/from Cassandra. This was ported over to use a newer cassandra client from the old
 * persistence code @
 *
 *     https://github.com/apache/usergrid/tree/3f819dc0679f84edb57c52e69b58622417cfd59f
 *     org.apache.usergrid.security.tokens.cassandra.TokenServiceImpl
 *
 */
public interface TokenSerialization extends Migration {

    void deleteToken(UUID tokenUUID);

    void revokeToken(UUID tokenUUID, ByteBuffer principalKeyBuffer);

    void updateTokenAccessTime(UUID tokenUUID, int accessdTime, int inactiveTime );

    Map<String, Object> getTokenInfo(UUID tokenUUID);

    void putTokenInfo(UUID tokenUUID, Map<String, Object> tokenInfo);

    List<UUID> getTokensForPrincipal(ByteBuffer principalKeyBuffer);

}
