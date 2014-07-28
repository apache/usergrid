
create keyspace Usergrid
  with placement_strategy = 'NetworkTopologyStrategy'
  and strategy_options = {us-east : 3}
  and durable_writes = true;

use Usergrid;

create column family Applications
  with column_type = 'Standard'
  and comparator = 'BytesType'
  and default_validation_class = 'BytesType'
  and key_validation_class = 'BytesType'
  and read_repair_chance = 0.1
  and dclocal_read_repair_chance = 0.0
  and populate_io_cache_on_flush = false
  and gc_grace = 864000
  and min_compaction_threshold = 4
  and max_compaction_threshold = 32
  and replicate_on_write = true
  and compaction_strategy = 'org.apache.cassandra.db.compaction.LeveledCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and bloom_filter_fp_chance = 0.1
  and compaction_strategy_options = {'sstable_size_in_mb' : '512'}
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

create column family PrincipalTokens
  with column_type = 'Standard'
  and comparator = 'UUIDType'
  and default_validation_class = 'BytesType'
  and key_validation_class = 'BytesType'
  and read_repair_chance = 0.1
  and dclocal_read_repair_chance = 0.0
  and populate_io_cache_on_flush = false
  and gc_grace = 864000
  and min_compaction_threshold = 4
  and max_compaction_threshold = 32
  and replicate_on_write = true
  and compaction_strategy = 'org.apache.cassandra.db.compaction.LeveledCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and bloom_filter_fp_chance = 0.1
  and compaction_strategy_options = {'sstable_size_in_mb' : '512'}
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

create column family Properties
  with column_type = 'Standard'
  and comparator = 'BytesType'
  and default_validation_class = 'BytesType'
  and key_validation_class = 'BytesType'
  and read_repair_chance = 0.1
  and dclocal_read_repair_chance = 0.0
  and populate_io_cache_on_flush = false
  and gc_grace = 864000
  and min_compaction_threshold = 4
  and max_compaction_threshold = 32
  and replicate_on_write = true
  and compaction_strategy = 'org.apache.cassandra.db.compaction.LeveledCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and bloom_filter_fp_chance = 0.1
  and compaction_strategy_options = {'sstable_size_in_mb' : '512'}
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

create column family Tokens
  with column_type = 'Standard'
  and comparator = 'BytesType'
  and default_validation_class = 'BytesType'
  and key_validation_class = 'BytesType'
  and read_repair_chance = 0.1
  and dclocal_read_repair_chance = 0.0
  and populate_io_cache_on_flush = false
  and gc_grace = 864000
  and min_compaction_threshold = 4
  and max_compaction_threshold = 32
  and replicate_on_write = true
  and compaction_strategy = 'org.apache.cassandra.db.compaction.LeveledCompactionStrategy'
  and caching = 'ALL'
  and bloom_filter_fp_chance = 0.1
  and compaction_strategy_options = {'sstable_size_in_mb' : '512'}
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};
