
drop keyspace Locks;

create keyspace Locks
  with placement_strategy = 'NetworkTopologyStrategy'
  and strategy_options = {us-east : 3}
  and durable_writes = false;

use Locks;

create column family HLocks
  with column_type = 'Standard'
  and comparator = 'UTF8Type'
  and default_validation_class = 'BytesType'
  and key_validation_class = 'UTF8Type'
  and read_repair_chance = 0.0
  and dclocal_read_repair_chance = 0.0
  and populate_io_cache_on_flush = false
  and gc_grace = 60
  and min_compaction_threshold = 4
  and max_compaction_threshold = 32
  and replicate_on_write = true
  and compaction_strategy = 'org.apache.cassandra.db.compaction.LeveledCompactionStrategy'
  and caching = 'ALL'
  and bloom_filter_fp_chance = 0.1
  and compaction_strategy_options = {'sstable_size_in_mb' : '512'}
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};
