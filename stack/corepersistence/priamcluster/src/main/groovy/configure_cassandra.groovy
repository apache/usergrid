// 
// configure_cassandra.groovy 
// 
// Emits Cassandra config file based on environment and Cassandra node registry in SimpleDB
//
import com.amazonaws.auth.*
import com.amazonaws.services.simpledb.*
import com.amazonaws.services.simpledb.model.*

String domain = "UGCloudFormation"

String accessKey = (String)System.getenv().get("AWS_ACCESS_KEY")
String secretKey = (String)System.getenv().get("AWS_SECRET_KEY")
String stackName = (String)System.getenv().get("STACK_NAME")
String hostName  = (String)System.getenv().get("PUBLIC_HOSTNAME")
def clusterName  = (String)System.getenv().get("CASSANDRA_CLUSTER_NAME")

def creds = new BasicAWSCredentials(accessKey, secretKey)
def sdbClient = new AmazonSimpleDBClient(creds)

// build seed list by listing all Cassandra nodes found in SimpleDB domain with our stackName
def selectResult = sdbClient.select(new SelectRequest((String)"select * from ${domain}"))
def seeds = ""
def sep = ""
for (item in selectResult.getItems()) {
    def att = item.getAttributes().get(0)
    if (att.getValue().equals(stackName)) {
        seeds = "${seeds}${sep}${item.getName()}"
        sep = ","
    }
}

def cassandraConfig = """
cluster_name: '${clusterName}'
listen_address: ${hostName}
seed_provider:
    - class_name: org.apache.cassandra.locator.SimpleSeedProvider
      parameters:
          - seeds: "${seeds}"
auto_bootstrap: true
initial_token:
hinted_handoff_enabled: true
hinted_handoff_throttle_in_kb: 1024
max_hints_delivery_threads: 2
authenticator: org.apache.cassandra.auth.AllowAllAuthenticator
authorizer: org.apache.cassandra.auth.AllowAllAuthorizer
partitioner: org.apache.cassandra.dht.RandomPartitioner
data_file_directories:
    - /var/lib/cassandra/data
commitlog_directory: /var/lib/cassandra/commitlog
disk_failure_policy: stop
key_cache_size_in_mb:
key_cache_save_period: 14400
row_cache_size_in_mb: 0
row_cache_save_period: 0
row_cache_provider: SerializingCacheProvider
saved_caches_directory: /var/lib/cassandra/saved_caches
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
endpoint_snitch: SimpleSnitch
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
