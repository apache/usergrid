/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */


// configure_cassandra.groovy 
// 
// Emits Cassandra config file based on environment and Cassandra node 
// registry in SimpleDB
//
import com.amazonaws.auth.*
import com.amazonaws.services.simpledb.*
import com.amazonaws.services.simpledb.model.*


String hostName  = (String)System.getenv().get("PUBLIC_HOSTNAME")
// build seed list by listing all Cassandra nodes found in SimpleDB domain with our stackName



def cassandraConfig = """


cluster_name: 'opscenter'
listen_address: ${hostName}
seed_provider:
    - class_name: org.apache.cassandra.locator.SimpleSeedProvider
      parameters:
          - seeds: "${hostName}"
auto_bootstrap: false 
num_tokens: 256
hinted_handoff_enabled: true
hinted_handoff_throttle_in_kb: 1024
max_hints_delivery_threads: 2
authenticator: org.apache.cassandra.auth.AllowAllAuthenticator
authorizer: org.apache.cassandra.auth.AllowAllAuthorizer
partitioner: org.apache.cassandra.dht.Murmur3Partitioner
data_file_directories:
    - /mnt/data/cassandra/data
commitlog_directory: /mnt/data/cassandra/commitlog
disk_failure_policy: stop
key_cache_size_in_mb: 2048
key_cache_save_period: 14400
row_cache_size_in_mb: 2048
row_cache_save_period: 14400
row_cache_provider: SerializingCacheProvider
saved_caches_directory: /mnt/data/cassandra/saved_caches
commitlog_sync: periodic
commitlog_sync_period_in_ms: 10000
commitlog_segment_size_in_mb: 32
flush_largest_memtables_at: 0.75
reduce_cache_sizes_at: 0.85
reduce_cache_capacity_to: 0.6
concurrent_reads: 32
concurrent_writes: 32
memtable_flush_queue_size: 4
trickle_fsync: false
trickle_fsync_interval_in_kb: 10240
storage_port: 7000
ssl_storage_port: 7001
rpc_address: 0.0.0.0
start_native_transport: false
native_transport_port: 9042
start_rpc: true
rpc_port: 9160
rpc_keepalive: true
rpc_server_type: sync
thrift_framed_transport_size_in_mb: 15
thrift_max_message_length_in_mb: 16
incremental_backups: false
snapshot_before_compaction: false
auto_snapshot: true
column_index_size_in_kb: 64
in_memory_compaction_limit_in_mb: 64
multithreaded_compaction: false
compaction_throughput_mb_per_sec: 16
compaction_preheat_key_cache: true
read_request_timeout_in_ms: 10000
range_request_timeout_in_ms: 10000
write_request_timeout_in_ms: 10000
truncate_request_timeout_in_ms: 60000
request_timeout_in_ms: 10000
cross_node_timeout: false
endpoint_snitch: Ec2Snitch
dynamic_snitch_update_interval_in_ms: 100
dynamic_snitch_reset_interval_in_ms: 600000
dynamic_snitch_badness_threshold: 0.1
request_scheduler: org.apache.cassandra.scheduler.NoScheduler
index_interval: 128
server_encryption_options:
    internode_encryption: none
    keystore: conf/.keystore
    keystore_password: cassandra
    truststore: conf/.truststore
    truststore_password: cassandra
client_encryption_options:
    enabled: false
    keystore: conf/.keystore
    keystore_password: cassandra
internode_compression: all
"""

println cassandraConfig
