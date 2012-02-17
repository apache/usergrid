package com.usergrid.count.common;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonMethod;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

/**
 * @author zznate
 */
public class CountSerDeUtils {

    public static String serialize(Count count) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(count);
        } catch (Exception ex) {
            throw new CountTransportSerDeException("Problem in serialize() call",ex);
        }
    }

    public static Count deserialize(String json) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(JsonMethod.CREATOR, JsonAutoDetect.Visibility.ANY);

        try {
            return mapper.readValue(json, Count.class);
        } catch (IOException e) {
            throw new CountTransportSerDeException("Problem in deserialize() call", e);
        }
    }
}
