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
update keyspace Locks with placement_strategy = 'NetworkTopologyStrategy' and strategy_options = {KEYSPACE_REGION : REPLICATION_FACTOR} and durable_writes = false;

use Locks;

update column family HLocks with gc_grace=60 and caching=ALL and read_repair_chance=0.00 and compaction_strategy='LeveledCompactionStrategy' and bloom_filter_fp_chance=0.1 and compaction_strategy_options={sstable_size_in_mb:512};

