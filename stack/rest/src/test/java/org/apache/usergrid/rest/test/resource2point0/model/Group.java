package org.apache.usergrid.rest.test.resource2point0.model;

import java.util.List;
import java.util.Map;

/**
 * Created by rockerston on 12/16/14.
 */
public class Group extends Entity{

    public Group(){}

    public Group(String name, String path) {

        this.put("name", name);
        this.put("path", path);
    }

    public Group (ApiResponse<Entity> response){
        if(response.getEntities() !=null &&  response.getEntities().size()>=1){
            List<Entity> entities =  response.getEntities();
            Map<String,Object> entity = entities.get(0);
            this.putAll(entity);
        }
    }

    public String getName(){
        return (String) this.get("name");
    }

    public String getPath(){
        return (String) this.get("path");
    }



}
