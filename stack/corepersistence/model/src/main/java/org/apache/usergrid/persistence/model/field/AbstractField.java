package org.apache.usergrid.persistence.model.field;

import org.apache.usergrid.persistence.model.field.value.EntityObject;

/**
 * Base class for data information
 */
public abstract class AbstractField<T> implements Field<T> {

    /**
     * Set the object this field belongs to
     */
    protected EntityObject parent;
    protected String name;
    protected T value;

    /**
     * Name and value must always be present.
     *
     * @param name The name of this field
     * @param value The value to set. If value is null, this means that the value should be
     * explicitly removed from the field storage
     */
    protected AbstractField( String name, T value ) {
        this.name = name;
        this.value = value;
    }

    /**
     * Default constructor for serialization
     */
    protected AbstractField() {

    }

    public String getName() {
        return name;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }

        AbstractField that = (AbstractField) o;

        if ( !name.equals( that.name ) ) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
