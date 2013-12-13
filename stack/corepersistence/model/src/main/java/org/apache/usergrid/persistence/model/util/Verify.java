package org.apache.usergrid.persistence.model.util;


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


    /**
     * Verifies that a string exists and must have characters
     * @param string
     * @param message
     */
    public static void stringExists(String string, String message){
        if(string == null){
           throw new NullPointerException( message );
        }

        if(string.length() == 0){
            throw new IllegalArgumentException( message );
        }
    }

}
