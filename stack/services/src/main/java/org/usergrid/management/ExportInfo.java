package org.usergrid.management;

import java.util.Map;

/**
 * Created by ApigeeCorporation on 1/31/14.
 */
//TODO: Documentation on this class.
public class ExportInfo { //extends Entity something {

    private String path;
    private Map<String, Object> properties;
    private String storage_provider;
    private Map<String, Object> storage_info;
    private String s3_accessId;
    private String admin_token;
    private String s3_key;
    private String bucket_location;


    /**
     *
     *the sch system doesn't make any assumes about the job or how it works.
     * and so if I need additional information to be persistant.
     *
     * The way to save data between queue and storing it.
     *
     * in my case, create a export entity. before I schedule the job and it'll have the pending state in it and
     * all the information I need to run. Then I'll pass the ID of the export info I saved in a collection and i'll put that in the jbo
     * data.
     *
     * persist the state in mechanisum that they can all access.
     *
     * I could make it a class and I can make it an entity. That way I can get it in and out.
     * doesn't get exposed to the user.
     */

    public ExportInfo ( Map<String, Object> exportData) {
        path = (String) exportData.get("path");
        properties = (Map) exportData.get("properties");
        storage_provider = (String) properties.get ("storage_provider");
        storage_info = (Map) properties.get("storage_info");
        s3_accessId = (String) storage_info.get("s3_accessId");
        admin_token = (String) storage_info.get("admin_token");
        s3_key = (String) storage_info.get("s3_key");
        bucket_location = (String) storage_info.get("bucket_location");
    }

    public String getPath () {
        return path;
    };

    //Wouldn't get exposed.
    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getStorage_provider () {
        return storage_provider;
    }
    //TODO: write setter methods

    public Map<String, Object> getStorage_info () { return storage_info; }

    public String getAdmin_token () { return admin_token; }
    //TODO: is this a security concern? How would we get rid of the key once we're done with this value?
    public String getS3_key () { return s3_key; }

    public String getBucket_location () { return bucket_location; }

    public String getS3_accessId () { return s3_accessId; }



}
