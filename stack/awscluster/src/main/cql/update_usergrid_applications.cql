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

update keyspace Usergrid_Applications with placement_strategy = 'NetworkTopologyStrategy' and strategy_options = {KEYSPACE_REGION : REPLICATION_FACTOR} and durable_writes = true;

use Usergrid_Applications;

update column family Application_Aggregate_Counters with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Application_Roles with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Applications with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Consumer_Queue_Messages_Properties with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Entity_Composite_Dictionaries with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Entity_Counters with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Entity_Dictionaries with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Entity_Id_Sets with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Entity_Index with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Entity_Index_Entries with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Entity_Metadata with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Entity_Properties with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Entity_Unique with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family MQ_Consumers with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family MQ_Consumers_Timeout with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family MQ_Counters with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family MQ_Property_Index with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family MQ_Property_Index_Entries with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Queue_Dictionaries with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Queue_Inbox with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Queue_Properties with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Queue_Subscribers with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Queue_Subscriptions with caching=KEYS_ONLY and read_repair_chance=0.1 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};

