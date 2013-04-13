package org.usergrid.query.validator;

import org.usergrid.persistence.DynamicEntity;

import java.util.Map;

public class QueryEntity extends DynamicEntity {

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        if(!(obj instanceof QueryEntity))
            return false;

        QueryEntity other = (QueryEntity)obj;
        Map<String, Object> properties = this.getProperties();
        Map<String, Object> otherProperties = other.getProperties();
        for(String key : properties.keySet()) {
            if( "created".equals(key) || "modified".equals(key) )
                continue;

            Object value = properties.get(key);
            Object otherValue = otherProperties.get(key);
            if( otherValue == null || !value.equals(otherValue) )
                return false;
        }
        return true;
    }
}
