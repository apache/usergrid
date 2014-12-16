package org.apache.usergrid.rest.test.resource2point0.model;

/**
 * Created by rockerston on 12/16/14.
 */
public class Role extends Entity{

    public Role(){}

    public Role(String name, String path) {

        this.put("name", name);
        this.put("path", path);
    }

    public Role (ApiResponse response){
        setResponse( response,"owner" );
    }

    public String getName(){
        return (String) this.get("name");
    }

    public String getPath(){
        return (String) this.get("path");
    }

}
