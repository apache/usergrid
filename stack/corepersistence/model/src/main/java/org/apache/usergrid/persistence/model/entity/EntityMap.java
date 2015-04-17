package org.apache.usergrid.persistence.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.usergrid.persistence.model.field.*;
import org.apache.usergrid.persistence.model.field.value.EntityObject;

import java.io.IOException;
import java.util.*;

/**
 * Core persistence Entity Map structure to persist to
 */
public class EntityMap extends HashMap<String,Object> {
    private static EntityToMapConverter entityToMapConverter = new EntityToMapConverter();
    public static final String VERSION_KEY = "__VERSION__";
    public static final String ID_KEY = "__ID__";
    public static final String TYPE_KEY = "__TYPE__";


    public EntityMap(){
        super();
    }

    public EntityMap(Id id,UUID version){
        super();
        setId(id);
        setVersion(version);
    }

    @JsonIgnore
    public Id getId(){
        return containsKey(ID_KEY) ? new SimpleId((UUID)get(ID_KEY), (String)get(TYPE_KEY)):null;
    }

    @JsonIgnore
    public void setId(Id id){
        if(id!=null) {
            put(ID_KEY, id.getUuid());
            put(TYPE_KEY, id.getType());
        }

    }

    @JsonIgnore
    public UUID getVersion(){
        return containsKey(VERSION_KEY) ? (UUID) get(VERSION_KEY):null;
    }

    @JsonIgnore
    public void setVersion(UUID version){
        if(version!=null) {
            put(VERSION_KEY, version);
        }
    }

    public static EntityMap fromEntity(Entity entity) {
        return entityToMapConverter.toMap(entity);
    }

    public void clearFields() {
        this.remove(ID_KEY);
        this.remove(TYPE_KEY);
        this.remove(VERSION_KEY);
    }
}
