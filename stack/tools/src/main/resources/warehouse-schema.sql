-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

create table {tableName} (
    id VARCHAR UNIQUE NOT NULL,
    organization VARCHAR NOT NULL,
    application VARCHAR NOT NULL,
    UGtype VARCHAR  NOT NULL,
    created TIMESTAMP NOT NULL,
    modified TIMESTAMP NOT NULL,
    activity_category VARCHAR,
    activity_content VARCHAR,
    activity_title VARCHAR,
    activity_verb VARCHAR,
    asset_path VARCHAR,
    device_name VARCHAR,
    event_category VARCHAR,
    event_message VARCHAR,
    event_timestamp TIMESTAMP,
    folder_path VARCHAR,
    group_path VARCHAR,
    notification_canceled VARCHAR,
    notification_deliver VARCHAR,
    notification_errorMessage VARCHAR,
    notification_expire VARCHAR,
    notification_finished VARCHAR,
    notification_payloads VARCHAR,
    notification_queued VARCHAR,
    notification_started VARCHAR,
    notification_statistics VARCHAR,
    notifier_environment VARCHAR,
    notifier_provider VARCHAR,
    receipt_errorCode VARCHAR,
    receipt_errorMessage VARCHAR,
    receipt_notificationUUID VARCHAR,
    receipt_notifierId VARCHAR,
    receipt_payload VARCHAR,
    receipt_sent VARCHAR,
    user_email VARCHAR,
    user_name VARCHAR,
    user_picture VARCHAR,
    user_username VARCHAR
);
