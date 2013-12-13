package org.apache.usergrid.persistence.model.field;


import java.io.Serializable;


/**
 * Interface for fields.  All fields must implement this method The T is the type of field (in the java runtime) The V
 * is the value of the field
 */
public interface Field<T> extends Serializable {

    /**
     * Get the name of the field
     * @return
     */
    public String getName();

    /**
     * Get the value of the field
     * @return
     */
    public T getValue();


}
