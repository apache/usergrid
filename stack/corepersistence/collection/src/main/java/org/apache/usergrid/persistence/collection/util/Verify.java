package org.apache.usergrid.persistence.collection.util;


/**
 * Class to help with input verification
 */
public class Verify {

    /**
     * Class to help with verification
     * @param value
     * @param message
     */
    public static void isNull(Object value, String message){
        if(value != null){
            throw new IllegalArgumentException(message  );
        }

    }

}
