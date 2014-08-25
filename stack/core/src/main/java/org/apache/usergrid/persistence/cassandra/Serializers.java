/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.cassandra;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.BytesArraySerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.DynamicCompositeSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;


/**
 * A uniform place to define all serializers.
 *
 * @author stliu
 * @date 2/7/14
 */
public interface Serializers {

    public static final StringSerializer se = StringSerializer.get();
    public static final ByteBufferSerializer be = ByteBufferSerializer.get();
    public static final UUIDSerializer ue = UUIDSerializer.get();
    public static final BytesArraySerializer bae = BytesArraySerializer.get();
    public static final DynamicCompositeSerializer dce = DynamicCompositeSerializer.get();
    public static final LongSerializer le = LongSerializer.get();
    public static final DoubleSerializer de = DoubleSerializer.get();
}
