/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */

create keyspace Usergrid_Applications
  with placement_strategy = 'NetworkTopologyStrategy'
  and strategy_options = {us-east : 3}
  and durable_writes = true;

use Usergrid_Applications;

create column family Application_Aggregate_Counters
  with column_type = 'Standard'
  and comparator = 'LongType'
  and default_validation_class = 'CounterColumnType'
  and key_validation_class = 'BytesType'
  and read_repair_chance = 0.1
  and dclocal_read_repair_chance = 0.0
  and populate_io_cache_on_flush = false
  and gc_grace = 864000
  and min_compaction_threshold = 4
  and max_compaction_threshold = 32
  and replicate_on_write = true
  and compaction_strategy = 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

create column family Application_Roles
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
  and compaction_strategy = 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

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
  and compaction_strategy = 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

create column family Consumer_Queue_Messages_Properties
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
  and compaction_strategy = 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

create column family Entity_Composite_Dictionaries
  with column_type = 'Standard'
  and comparator = 'DynamicCompositeType(b=>org.apache.cassandra.db.marshal.BytesType,A=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.AsciiType),B=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.BytesType),a=>org.apache.cassandra.db.marshal.AsciiType,L=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.LongType),l=>org.apache.cassandra.db.marshal.LongType,I=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.IntegerType),i=>org.apache.cassandra.db.marshal.IntegerType,U=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.UUIDType),T=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.TimeUUIDType),u=>org.apache.cassandra.db.marshal.UUIDType,t=>org.apache.cassandra.db.marshal.TimeUUIDType,s=>org.apache.cassandra.db.marshal.UTF8Type,S=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.UTF8Type),X=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.LexicalUUIDType),x=>org.apache.cassandra.db.marshal.LexicalUUIDType)'
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

create column family Entity_Counters
  with column_type = 'Standard'
  and comparator = 'BytesType'
  and default_validation_class = 'CounterColumnType'
  and key_validation_class = 'BytesType'
  and read_repair_chance = 0.1
  and dclocal_read_repair_chance = 0.0
  and populate_io_cache_on_flush = false
  and gc_grace = 864000
  and min_compaction_threshold = 4
  and max_compaction_threshold = 32
  and replicate_on_write = true
  and compaction_strategy = 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

create column family Entity_Dictionaries
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

create column family Entity_Id_Sets
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

create column family Entity_Index
  with column_type = 'Standard'
  and comparator = 'DynamicCompositeType(b=>org.apache.cassandra.db.marshal.BytesType,A=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.AsciiType),B=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.BytesType),a=>org.apache.cassandra.db.marshal.AsciiType,L=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.LongType),l=>org.apache.cassandra.db.marshal.LongType,I=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.IntegerType),i=>org.apache.cassandra.db.marshal.IntegerType,U=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.UUIDType),T=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.TimeUUIDType),u=>org.apache.cassandra.db.marshal.UUIDType,t=>org.apache.cassandra.db.marshal.TimeUUIDType,s=>org.apache.cassandra.db.marshal.UTF8Type,S=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.UTF8Type),X=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.LexicalUUIDType),x=>org.apache.cassandra.db.marshal.LexicalUUIDType)'
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

create column family Entity_Index_Entries
  with column_type = 'Standard'
  and comparator = 'DynamicCompositeType(b=>org.apache.cassandra.db.marshal.BytesType,A=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.AsciiType),B=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.BytesType),a=>org.apache.cassandra.db.marshal.AsciiType,L=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.LongType),l=>org.apache.cassandra.db.marshal.LongType,I=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.IntegerType),i=>org.apache.cassandra.db.marshal.IntegerType,U=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.UUIDType),T=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.TimeUUIDType),u=>org.apache.cassandra.db.marshal.UUIDType,t=>org.apache.cassandra.db.marshal.TimeUUIDType,s=>org.apache.cassandra.db.marshal.UTF8Type,S=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.UTF8Type),X=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.LexicalUUIDType),x=>org.apache.cassandra.db.marshal.LexicalUUIDType)'
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

create column family Entity_Metadata
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

create column family Entity_Properties
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

create column family Entity_Unique
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

create column family MQ_Consumers
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
  and compaction_strategy = 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

create column family MQ_Consumers_Timeout
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
  and compaction_strategy = 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

create column family MQ_Counters
  with column_type = 'Standard'
  and comparator = 'BytesType'
  and default_validation_class = 'CounterColumnType'
  and key_validation_class = 'BytesType'
  and read_repair_chance = 0.1
  and dclocal_read_repair_chance = 0.0
  and populate_io_cache_on_flush = false
  and gc_grace = 864000
  and min_compaction_threshold = 4
  and max_compaction_threshold = 32
  and replicate_on_write = true
  and compaction_strategy = 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

create column family MQ_Property_Index
  with column_type = 'Standard'
  and comparator = 'DynamicCompositeType(b=>org.apache.cassandra.db.marshal.BytesType,A=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.AsciiType),B=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.BytesType),a=>org.apache.cassandra.db.marshal.AsciiType,L=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.LongType),l=>org.apache.cassandra.db.marshal.LongType,I=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.IntegerType),i=>org.apache.cassandra.db.marshal.IntegerType,U=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.UUIDType),T=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.TimeUUIDType),u=>org.apache.cassandra.db.marshal.UUIDType,t=>org.apache.cassandra.db.marshal.TimeUUIDType,s=>org.apache.cassandra.db.marshal.UTF8Type,S=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.UTF8Type),X=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.LexicalUUIDType),x=>org.apache.cassandra.db.marshal.LexicalUUIDType)'
  and default_validation_class = 'BytesType'
  and key_validation_class = 'BytesType'
  and read_repair_chance = 0.1
  and dclocal_read_repair_chance = 0.0
  and populate_io_cache_on_flush = false
  and gc_grace = 864000
  and min_compaction_threshold = 4
  and max_compaction_threshold = 32
  and replicate_on_write = true
  and compaction_strategy = 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

create column family MQ_Property_Index_Entries
  with column_type = 'Standard'
  and comparator = 'DynamicCompositeType(b=>org.apache.cassandra.db.marshal.BytesType,A=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.AsciiType),B=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.BytesType),a=>org.apache.cassandra.db.marshal.AsciiType,L=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.LongType),l=>org.apache.cassandra.db.marshal.LongType,I=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.IntegerType),i=>org.apache.cassandra.db.marshal.IntegerType,U=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.UUIDType),T=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.TimeUUIDType),u=>org.apache.cassandra.db.marshal.UUIDType,t=>org.apache.cassandra.db.marshal.TimeUUIDType,s=>org.apache.cassandra.db.marshal.UTF8Type,S=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.UTF8Type),X=>org.apache.cassandra.db.marshal.ReversedType(org.apache.cassandra.db.marshal.LexicalUUIDType),x=>org.apache.cassandra.db.marshal.LexicalUUIDType)'
  and default_validation_class = 'BytesType'
  and key_validation_class = 'BytesType'
  and read_repair_chance = 0.1
  and dclocal_read_repair_chance = 0.0
  and populate_io_cache_on_flush = false
  and gc_grace = 864000
  and min_compaction_threshold = 4
  and max_compaction_threshold = 32
  and replicate_on_write = true
  and compaction_strategy = 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

create column family Queue_Dictionaries
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
  and compaction_strategy = 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

create column family Queue_Inbox
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
  and compaction_strategy = 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

create column family Queue_Properties
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
  and compaction_strategy = 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

create column family Queue_Subscribers
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
  and compaction_strategy = 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

create column family Queue_Subscriptions
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
  and compaction_strategy = 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'
  and caching = 'KEYS_ONLY'
  and compression_options = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};
