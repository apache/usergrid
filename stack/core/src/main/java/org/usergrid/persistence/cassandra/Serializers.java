package org.usergrid.persistence.cassandra;

import me.prettyprint.cassandra.serializers.*;

/**
 * A uniform place to define all serializers.
 * 
 * @author stliu
 * @date 2/7/14
 */
public interface Serializers {
     StringSerializer se = StringSerializer.get();
     ByteBufferSerializer be =  ByteBufferSerializer.get();
     UUIDSerializer ue = UUIDSerializer.get();
     BytesArraySerializer bae =  BytesArraySerializer.get();
     DynamicCompositeSerializer dce =  DynamicCompositeSerializer.get();
     LongSerializer le =  LongSerializer.get();
    DoubleSerializer de = DoubleSerializer.get();
}
