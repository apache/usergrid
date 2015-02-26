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

update keyspace Usergrid with placement_strategy = 'NetworkTopologyStrategy' and strategy_options = {KEYSPACE_REGION : REPLICATION_FACTOR} and durable_writes = true;

use Usergrid;

update column family Applications with caching=KEYS_ONLY and read_repair_chance=0.1  and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family PrincipalTokens with caching=KEYS_ONLY and read_repair_chance=0.1  and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Properties with caching=KEYS_ONLY and read_repair_chance=0.1  and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};
update column family Tokens with caching=ALL and read_repair_chance=0.1  and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};

