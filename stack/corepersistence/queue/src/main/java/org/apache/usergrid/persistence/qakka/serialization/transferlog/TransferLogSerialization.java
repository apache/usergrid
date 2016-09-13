/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.qakka.serialization.transferlog;

import com.datastax.driver.core.PagingState;
import org.apache.usergrid.persistence.core.migration.schema.Migration;
import org.apache.usergrid.persistence.qakka.exceptions.QakkaException;
import org.apache.usergrid.persistence.qakka.serialization.Result;

import java.util.UUID;


public interface TransferLogSerialization extends Migration {

    /**
     * Record transfer log record.
     *
     * @param queueName Name of queue.
     * @param source Source region.
     * @param dest Destination region.
     * @param messageId UUID of message in message_data table.
     */
    void recordTransferLog(
        String queueName, String source, String dest, UUID messageId);

    /**
     * Remove transfer log record.
     *
     * @param queueName Name of queue.
     * @param source Source region.
     * @param dest Destination region.
     * @param messageId UUID of message in message_data table.
     * @throws QakkaException If transfer log message was not found or could not be removed.
     */
    void removeTransferLog(
        String queueName, String source, String dest, UUID messageId) throws QakkaException;

    /**
     * Get all transfer logs (for testing purposes)
     *
     * @param pagingState Paging state (or null if none)
     * @param fetchSize Number of rows to be fetched per page (or -1 for default)
     */
    Result<TransferLog> getAllTransferLogs(PagingState pagingState, int fetchSize);
}
